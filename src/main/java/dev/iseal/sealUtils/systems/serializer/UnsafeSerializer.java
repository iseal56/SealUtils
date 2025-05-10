package dev.iseal.sealUtils.systems.serializer;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import dev.iseal.ExtraKryoCodecs.ExtraKryoCodecs;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.utils.ExceptionHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.logging.Level;

public class UnsafeSerializer {

    private static Kryo kryo;

    // housekeeping
    private static void checkKryo() {
        if (kryo != null) {
            return;
        }
        kryo = new Kryo();
        ExtraKryoCodecs.init(kryo, SealUtils.isDebug());
        kryo.setRegistrationRequired(false);
    }

    /**
     * Serialize objects to a byte array
     *
     * @param objects The objects to serialize
     * @return The byte array containing the serialized objects
     *
     */
    public static byte[] serialize(Object... objects) {
        // if no objects are passed, return an empty byte array
        if (objects.length == 0) {
            return new byte[0];
        }
        // create a new output stream && initialize kryo output on it
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        checkKryo();
        // write all objects to the output stream
        for (Object object : objects) {
            kryo.writeObject(output, object);
        }
        try {
            // flush the output stream and return the byte array
            output.flush();
            output.close();
            outputStream.flush();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "FAILED_TO_SERIALIZE_OBJECTS");
            return null;
        }
    }

    /**
     * Deserialize a byte array to objects
     * In a case where deserialization fails, the exception is handled and null is returned.
     *
     * @param data The byte array to deserialize
     * @param deserializeTo The objects to deserialize to
     * @return The deserialized objects or null if deserialization fails
     */
    public static Object[] deserialize(byte[] data, Class<?>... deserializeTo) {
        if (data.length == 0) {
            return null;
        }
        ArrayList<Object> decodedObjects = new ArrayList<>();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        Input input = new Input(inputStream);
        checkKryo();
        for (Class<?> clazz : deserializeTo) {
            try {
                decodedObjects.add(kryo.readObject(input, clazz));
            } catch (Exception e) {
                // handle the exception and return null
                e.printStackTrace();
                return null;
            }
        }
        return decodedObjects.toArray(new Object[0]);
    }

    /**
     * Register a class to be serialized/deserialized by Kryo
     * This is not necessary but improves performance / reduces size
     * **untested**
     *
     * @param clazz The class to register
     * @param serializer The serializer to use
     * @param id The id to register the class with
     */
    public static void registerClass(Class<?> clazz, Serializer<?> serializer, int id) {
        checkKryo();
        if (kryo.getRegistration(clazz) != null && kryo.getRegistration(id) != null) {
            return;
        }
        kryo.register(clazz, serializer, id);
    }

    /**
     * Register a class to be serialized/deserialized by Kryo
     * This is not necessary but improves performance / reduces size
     * **untested**
     *
     * @param clazz The class to register
     * @param id The id to register the class with
     */
    public static void registerClass(Class<?> clazz, int id) {
        checkKryo();
        if (kryo.getRegistration(clazz) != null && kryo.getRegistration(id) != null) {
            return;
        }
        kryo.register(clazz, id);
    }

    /**
     * Register a class to be serialized/deserialized by Kryo
     * This is not necessary but improves performance / reduces size
     * **untested**
     *
     * @param clazz The class to register
     */
    public static void registerClass(Class<?> clazz) {
        checkKryo();
        if (kryo.getRegistration(clazz) != null) {
            return;
        }
        kryo.register(clazz);
    }
}
