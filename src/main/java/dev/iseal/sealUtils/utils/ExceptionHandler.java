package dev.iseal.sealUtils.utils;

import dev.iseal.ExtraKryoCodecs.Enums.SerializersEnums.AnalyticsAPI.SealUtilsAnalyticsSerializers;
import dev.iseal.ExtraKryoCodecs.Holders.AnalyticsAPI.SealUtils.ErrorReport;
import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.systems.analytics.AnalyticsManager;
import dev.iseal.sealUtils.systems.analytics.AnalyticsReturnValue;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ExceptionHandler {

    private static ExceptionHandler instance;
    private final SealLogger log = SealUtils.getLogger();
    private final ConcurrentHashMap<String, String> analyticsVersion = new ConcurrentHashMap<>();
    private ArrayList<String> currentLog = new ArrayList<>();

    /**
     * Set the version string for the caller's package.
     *
     * @param version the version string to set
     */
    public void setVersion(String version) {
        String name = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for version tracking. Defaulting to 'default'.");
            name = "default";
        }
        analyticsVersion.put(name, version);
    }

    /**
     * Get the version string for the caller's package.
     *
     * @return the version string, or null if not set
     */
    public String getVersion() {
        String name = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for version tracking. Defaulting to 'default'.");
            name = "default";
        }
        return analyticsVersion.get(name);
    }

    /**
     * Set the version string for a specific class' package.
     *
     * @param clazz   the class whose package version to set
     * @param version the version string
     */
    public void setVersion(Class<?> clazz, String version) {
        String name = clazz.getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for version tracking. Defaulting to 'default'.");
            name = "default";
        }
        analyticsVersion.put(name, version);
    }

    /**
     * Get the version string for a specific class' package.
     *
     * @param clazz the class whose package version to get
     * @return the version string, or null if not set
     */
    public String getVersion(Class<?> clazz) {
        String name = clazz.getPackageName().split("\\.")[2];
        if (name == null || name.isEmpty()) {
            log.warn("[AnalyticsManager] Unable to determine package name for version tracking. Defaulting to 'default'.");
            name = "default";
        }
        return analyticsVersion.get(name);
    }

    // class, instance
    private final HashMap<Class<? extends Dumpable>, Dumpable> registeredClasses = new HashMap<>();

    public static ExceptionHandler getInstance() {
        if (instance == null)
            instance = new ExceptionHandler();
        return instance;
    }

    public void dealWithException(Exception ex, Level logLevel, String errorMessage, Boolean sendAnalytics, Object... moreInfo) {
        dealWithExceptionExtended(
                ex, logLevel, errorMessage,
                false,
                // execute here instead of passing it since it would return ExceptionHandler.class
                Optional.of(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()),
                sendAnalytics,
                Arrays.stream(moreInfo).map(Object::toString).toArray(String[]::new)
        );
    }

    public void dealWithException(Exception ex, Level logLevel, String errorMessage, Object... moreInfo) {
        dealWithExceptionExtended(
                ex, logLevel, errorMessage,
                false,
                // execute here instead of passing it since it would return ExceptionHandler.class
                Optional.of(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()),
                true,
                Arrays.stream(moreInfo).map(Object::toString).toArray(String[]::new)
        );
    }

    public Optional<ArrayList<String>> dealWithExceptionExtended(Exception ex, Level logLevel, String errorMessage, boolean returnLog, Optional<Class<?>> callerClassOpt, Boolean sendAnalytics, String... moreInfo){
        currentLog = new ArrayList<>();
        Class<?> callerClass = callerClassOpt.orElse(
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
        );

        currentLog.add("Exception triggered by "+callerClass.getName());
        currentLog.add("The exception message is "+ex.getMessage());
        currentLog.add("The error message is "+errorMessage);
        currentLog.add("The stacktrace and all of its details known are as follows: ");
        for (StackTraceElement stackTraceElement : ex.getStackTrace())
            currentLog.add(stackTraceElement.toString());

        currentLog.add("More details (make sure to tell these to the developer): ");
        int i = 1;
        for (Object obj : moreInfo) {
            currentLog.add("More info "+i+": "+obj.toString());
            i++;
        }

        attemptToDealWithCustomException(ex);

        if (sendAnalytics) {
            CompletableFuture<AnalyticsReturnValue> returnValueCompletableFuture = AnalyticsManager.INSTANCE.sendEvent("AA-000000000", SealUtilsAnalyticsSerializers.ERROR_REPORT,
                    new ErrorReport(
                            getVersion(callerClass), errorMessage,
                            callerClass.getPackageName(), Instant.now(Clock.systemUTC()), logLevel
                    )
            );

            returnValueCompletableFuture.thenAcceptAsync((returnValue) -> {
                if (returnValue == AnalyticsReturnValue.EVENT_SENT) {
                    log.info("Analytics event for error report sent successfully.");
                } else if (returnValue == AnalyticsReturnValue.CUSTOM_MESSAGE) {
                    log.info("Analytics event for error report sent with custom message: " + returnValue.getMessage());
                } else {
                    log.warn("Failed to send analytics event for error report: " + returnValue.getMessage());
                }
            }).exceptionally((ex1) -> {
                log.warn("Exception while sending analytics event: " + ex1.getMessage());
                return null;
            });
        }


        if (SealUtils.isDebug())
            currentLog.addAll(dumpAllClasses(false).orElse(new ArrayList<>()));
        currentLog.forEach((str) -> log.log(logLevel, str));
        return returnLog ? Optional.of(currentLog) : Optional.empty();
    }
    
    public Optional<ArrayList<String>> dumpAllClasses(boolean printToConsole) {
        ArrayList<String> dumpLog = new ArrayList<>();
        HashMap<String, HashMap<String, Object>> dumpMap = new HashMap<>();
        registeredClasses.forEach((clazz, dumpable) -> {
            dumpMap.put(clazz.getSimpleName(), dumpable.dump());
        });

        dumpMap.forEach((className, dumpMapTemp) -> {
            dumpMapTemp.forEach((toDump, dumpValue) -> {
                if (dumpValue == null)
                    dumpLog.add("Dump from: "+className+" -> "+toDump+": null - something is wrong.");
                else
                    dumpLog.add("Dump from: "+className+" -> "+toDump+": "+dumpValue.toString());
            });
        });
        if (printToConsole) {
            dumpLog.forEach(log::info);
        }
        
        return printToConsole ? Optional.empty() : Optional.of(dumpLog);
    }

    private void attemptToDealWithCustomException(Exception ex) {
        if (ex instanceof SecurityException se) {
            currentLog.add("[SealUtils] SecurityException caught, what?");
        }
    }

    public void registerClass(Class<? extends Dumpable> clazz, Dumpable instance) {
        registeredClasses.put(clazz, instance);
    }
}
