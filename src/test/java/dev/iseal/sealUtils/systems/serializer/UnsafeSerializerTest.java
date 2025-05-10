package dev.iseal.sealUtils.systems.serializer;

import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.utils.GlobalUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UnsafeSerializerTest {

    @Test
    public void trySerializingStrings() {
        SealUtils.init(true);
        for (int i = 0; i < 100; i++) {
            String testString = GlobalUtils.generateRandomString(i);
            byte[] serializedData = UnsafeSerializer.serialize(testString);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            String deserializedString = (String) UnsafeSerializer.deserialize(serializedData, String.class)[0];
            assertThat(deserializedString)
                    .isNotNull()
                    .isEqualTo(testString);
        }
    }

    @Test
    void trySerializingInts() {
        SealUtils.init(true);
        for (int i = 0; i < 100; i++) {
            byte[] serializedData = UnsafeSerializer.serialize(i);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            int deserializedInt = (int) UnsafeSerializer.deserialize(serializedData, int.class)[0];
            assertThat(deserializedInt)
                    .isNotNull()
                    .isEqualTo(i);
        }
    }

    @Test
    void trySerializingMultipleItems() {
        SealUtils.init(true);
        for (int i = 0; i < 100; i++) {
            String testString = GlobalUtils.generateRandomString(i);
            int testInt = i;
            byte[] serializedData = UnsafeSerializer.serialize(testString, testInt);
            assertThat(serializedData)
                    .isNotNull()
                    .hasSizeGreaterThan(0)
                    .isNotEmpty();
            Object[] deserializedObjects = UnsafeSerializer.deserialize(serializedData, String.class, int.class);
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
