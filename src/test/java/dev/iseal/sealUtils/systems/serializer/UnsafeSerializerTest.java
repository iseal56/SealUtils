package dev.iseal.sealUtils.systems.serializer;

import com.esotericsoftware.kryo.Kryo;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.systems.sealLogger.SLF4JLogger;
import dev.iseal.sealUtils.utils.GlobalUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UnsafeSerializerTest {

    private Kryo kryo = new Kryo();

    @BeforeAll
    public static void setup() {
        SealUtils.init(true, "1.0.0.0-TESTVERSION");
    }

    @Test
    public void trySerializingStrings() {
        for (int i = 0; i < 100; i++) {
            String testString = GlobalUtils.generateRandomString(i);
            byte[] serializedData = UnsafeSerializer.serialize(kryo, testString);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            String deserializedString = (String) UnsafeSerializer.deserialize(kryo, serializedData, String.class)[0];
            assertThat(deserializedString)
                    .isNotNull()
                    .isEqualTo(testString);
        }
    }

    @Test
    void trySerializingInts() {
        for (int i = 0; i < 100; i++) {
            byte[] serializedData = UnsafeSerializer.serialize(kryo, i);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            int deserializedInt = (int) UnsafeSerializer.deserialize(kryo, serializedData, int.class)[0];
            assertThat(deserializedInt)
                    .isNotNull()
                    .isEqualTo(i);
        }
    }

    @Test
    void trySerializingMultipleItems() {
        for (int i = 0; i < 100; i++) {
            String testString = GlobalUtils.generateRandomString(i);
            int testInt = i;
            byte[] serializedData = UnsafeSerializer.serialize(kryo, testString, testInt);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            Object[] deserializedObjects = UnsafeSerializer.deserialize(kryo, serializedData, String.class, int.class);
            assertThat(deserializedObjects)
                    .isNotNull()
                    .hasSize(2);
            assertThat(deserializedObjects[0])
                    .isNotNull()
                    .isEqualTo(testString);
            assertThat(deserializedObjects[1])
                    .isNotNull()
                    .isEqualTo(testInt);
        }
    }
}
