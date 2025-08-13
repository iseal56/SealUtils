package dev.iseal.sealUtils.systems.analytics;

import dev.iseal.ExtraKryoCodecs.Enums.SerializersEnums.AnalyticsAPI.AnalyticsSerializers;
import dev.iseal.ExtraKryoCodecs.Holders.AnalyticsAPI.PluginVersionInfo;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.systems.sealLogger.SLF4JLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAnalytics {

    @Test
    public void testAnalytics() {
        SealUtils.init(true, "1.0.0.0-TESTVERSION");
        AnalyticsManager.INSTANCE.setEnabled("SealUtils", true);
        CompletableFuture<AnalyticsReturnValue> value = AnalyticsManager.INSTANCE.sendEvent("AA-000000000", AnalyticsSerializers.PLUGIN_VERSION_INFO,
                new PluginVersionInfo(
                        "1.0.0.0", "1.20.1",
                        "Paper", "21",
                        "Linux", "6.15.7-arch1-1", "x86_64")
                );
        AnalyticsReturnValue returnValue = value.join();
        assertNotNull(returnValue);
        assertEquals(AnalyticsReturnValue.EVENT_SENT, returnValue);
    }

}
