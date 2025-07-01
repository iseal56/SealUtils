package dev.iseal.sealUtils.systems.analytics;

import dev.iseal.ExtraKryoCodecs.Enums.SerializersEnums.AnalyticsAPI.AnalyticsSerializers;
import dev.iseal.ExtraKryoCodecs.Holders.AnalyticsAPI.PluginVersionInfo;
import dev.iseal.sealUtils.SealUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestAnalytics {

    @Test
    public void testAnalytics() {
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
