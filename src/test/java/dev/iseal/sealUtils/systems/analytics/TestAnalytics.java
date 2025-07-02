package dev.iseal.sealUtils.systems.analytics;

import dev.iseal.ExtraKryoCodecs.Enums.SerializersEnums.AnalyticsAPI.AnalyticsSerializers;
import dev.iseal.ExtraKryoCodecs.Holders.AnalyticsAPI.PluginVersionInfo;
import dev.iseal.sealUtils.SealUtils;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAnalytics {

    @Test
    public void testAnalytics() throws Exception{
        try {
            // check if localhost:8080 is reachable
            URL url = new URL("http://localhost:8080");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1000); // 1 second timeout
            connection.setReadTimeout(1000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                System.out.println("Analytics server is not reachable. Skipping test.");
                return; // Skip the test if the server is not reachable
            }
        } catch (ConnectException e) {
            System.out.println("Analytics server is not reachable. Skipping test.");
            return; // Skip the test if the server is not reachable
        }

        SealUtils.init(true);
        AnalyticsManager.INSTANCE.setEnabled("SealUtils", true);
        CompletableFuture<AnalyticsReturnValue> value = AnalyticsManager.INSTANCE.sendEvent("SealUtils", AnalyticsSerializers.PLUGIN_VERSION_INFO,
                new PluginVersionInfo("1.0.0.0", "1.20.1", "Paper", "21")
                );
        AnalyticsReturnValue returnValue = value.join();
        assertNotNull(returnValue);
        assertEquals(AnalyticsReturnValue.EVENT_SENT, returnValue);
    }

}
