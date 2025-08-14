package dev.iseal.sealUtils.systems.analytics;

import com.esotericsoftware.kryo.Kryo;
import dev.iseal.ExtraKryoCodecs.Enums.Serializer;
import dev.iseal.ExtraKryoCodecs.Enums.SerializersEnums.AnalyticsAPI.PerformanceAnalyticsSerializers;
import dev.iseal.ExtraKryoCodecs.ExtraKryoCodecs;
import dev.iseal.ExtraKryoCodecs.Holders.AnalyticsAPI.Performance.PerfManagerDurations;
import dev.iseal.ExtraKryoCodecs.Utils.SerializerEnum;
import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.systems.serializer.UnsafeSerializer;
import dev.iseal.sealUtils.utils.ExceptionHandler;
import dev.iseal.sealUtils.utils.Pair;
import dev.iseal.sealUtils.systems.performance.PerfManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Level;

public class AnalyticsManager {
    public static final AnalyticsManager INSTANCE = new AnalyticsManager();
    private final Kryo kryo = new Kryo();
    private final HttpClient client;

    private final HashMap<String, Pair<String, Boolean>> analyticsEnabled = new HashMap<>();
    // Add version map
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    private final ScheduledThreadPoolExecutor scheduledExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    private AnalyticsManager() {
        // Private constructor to prevent instantiation
        ExtraKryoCodecs.init(kryo, SealUtils.isDebug(), Serializer.ANALYTICS_API);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_2)
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[AnalyticsManager] JVM shutting down, waiting for analytics tasks to complete...");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("[AnalyticsManager] Analytics tasks did not finish before timeout.");
                    executor.shutdownNow(); // Force shutdown if tasks did not finish
                }
            } catch (InterruptedException e) {
                log.warn("[AnalyticsManager] Interrupted while waiting for analytics tasks to finish.");
                Thread.currentThread().interrupt();
            }
        }));
    }

    private final SealLogger log = SealUtils.getLogger();

    /**
     * Enables or disables analytics tracking.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(String provider, boolean enabled) {
        String name = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for analytics tracking. Defaulting to 'default'.");
            name = "default";
        }
        if (analyticsEnabled.containsKey(name) && analyticsEnabled.get(name).getSecond() == enabled) {
            log.info("[AnalyticsManager] Analytics tracking for " + name + " is already set to " + enabled);
            return; // No change needed
        }
        analyticsEnabled.put(name, new Pair<>(provider, enabled));
    }

    /**
     * Checks if analytics tracking is enabled.
     *
     * @return true if enabled, false if disabled, or null if not set for the package.
     */
    public Boolean isEnabled() {
        String name = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for analytics tracking. Defaulting to 'default'.");
            name = "default";
        }
        return analyticsEnabled.containsKey(name) && analyticsEnabled.get(name).getSecond();
    }

    /**
     * Checks if analytics tracking is enabled for the caller's package.
     *
     * @param callerClass the class of the caller to determine the package name
     * @return true if analytics is enabled for the caller's package, false if disabled, or null if not set
     */
    public boolean isEnabled(Class<?> callerClass) {
        String name = callerClass.getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for analytics tracking. Defaulting to 'default'.");
            name = "default";
        }
        return analyticsEnabled.containsKey(name) && analyticsEnabled.get(name).getSecond();
    }

    /**
     * Initializes timer requests for analytics.
     * Call this method after setting up the analytics provider and enabling analytics.
     */
    public void initializeTimedRequests(boolean performanceMetrics) {
        if (performanceMetrics) {
            scheduledExecutor.scheduleAtFixedRate(
                    () -> {
                        // fake id cause health check doesn't require it
                        String fakeId = "AA-000000000";
                        if (PerfManager.getDurations() != null && !PerfManager.getDurations().isEmpty()) {
                            log.info("[AnalyticsManager] Sending health state analytics event.");
                            sendEvent(fakeId, PerformanceAnalyticsSerializers.PERF_MANAGER_DURATIONS,
                                    new PerfManagerDurations(PerfManager.getDurations()));
                        } else {
                            log.info("[AnalyticsManager] No performance metrics to send.");
                        }
                    }, 1, 1, TimeUnit.HOURS
            );
        }
    }

    /**
     * Sends an analytics event to the specified provider.
     *
     * @param event the event to send, represented by a SerializerEnum
     * @param data  the data to send with the event, must be an instance of the class defined in SerializerEnum
     */
    public CompletableFuture<AnalyticsReturnValue> sendEvent(String ID, SerializerEnum event, Object data) {
        CompletableFuture<AnalyticsReturnValue> future = new CompletableFuture<>();
        String name = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getPackageName().split("\\.")[2];
        Pair<String, Boolean> pair = analyticsEnabled.get(name);
        if (!isEnabled(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass())) {
            future.complete(AnalyticsReturnValue.NOT_ENABLED);
            return future; // Do not send events if analytics is disabled
        }
        if (event == null || data == null || ID == null) {
            log.warn("[AnalyticsManager] Provider, event, data, or ID is null. Cannot send analytics event.");
            future.complete(AnalyticsReturnValue.MALFORMED_DATA);
            return future;
        }
        Class<?> dataClass = event.getEffectClass();
        if (dataClass == null) {
            log.warn("[AnalyticsManager] No class found for event: " + event.getPacketName());
            future.complete(AnalyticsReturnValue.MALFORMED_DATA);
            return future;
        }
        if (!dataClass.isInstance(data)) {
            log.warn("[AnalyticsManager] Data is not an instance of the expected class: " + dataClass.getName());
            future.complete(AnalyticsReturnValue.MALFORMED_DATA);
            return future;
        }

        // validate id
        if (ID.isEmpty() || !ID.matches("AA-\\d{9}")) {
            log.warn("[AnalyticsManager] Invalid ID: " + ID);
            future.complete(AnalyticsReturnValue.MALFORMED_DATA);
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            HttpRequest request;
            try {
                request = HttpRequest.newBuilder()
                        .uri(new URI((SealUtils.isDebug() ?
                                "http://localhost:8080/api/v2/" :
                                "https://analytics.iseal.dev/api/v2/"
                        ) + pair.getFirst() + "/" + event.getPacketName()))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .header("AA-ID", ID)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(UnsafeSerializer.serialize(kryo, data)))
                        .build();
            } catch (URISyntaxException e) {
                ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "REQUEST_CREATION_FAILED", false, pair, data, event);
                return AnalyticsReturnValue.MALFORMED_DATA;
            }

            log.debug("[AnalyticsManager] Sending analytics event for provider: " + pair.getFirst() + ", event: " + event.getPacketName() + ", data: " + data);
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "REQUEST_CREATION_FAILED", false, pair, data, event);
                return AnalyticsReturnValue.MALFORMED_DATA;
            }
            AnalyticsReturnValue toReturn = AnalyticsReturnValue.fromStatusCode(response.statusCode());
            if (toReturn == AnalyticsReturnValue.CUSTOM_MESSAGE) {
                toReturn.setMessage(response.body());
                toReturn.setStatusCode(response.statusCode());
            }
            log.debug("[AnalyticsManager] Received response for analytics event: " + toReturn.getMessage() + " (status code: " + toReturn.getStatusCode() + ")");
            return toReturn;
        }, executor);
    }
}

