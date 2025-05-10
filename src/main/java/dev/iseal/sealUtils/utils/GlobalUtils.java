package dev.iseal.sealUtils.utils;

import org.reflections.Reflections;

import java.io.*;
import java.util.*;

public class GlobalUtils {

    /**
        * Serialize a serializable class to a byte array
        * @param obj The object to serialize
        * @return The serialized object
        * @throws IllegalArgumentException If the object is not serializable
     */
    public static byte[] serializeSerializableClass(final Object obj, final byte[] extraData) {
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Object must be serializable");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(baos)) {
            // write extra data to the beginning of the stream
            out.write(extraData);

            // write the object to the stream
            out.writeObject(obj);
            out.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Deserialize a byte array to a serializable class
     * @param bytes The byte array to deserialize
     * @return The deserialized object
     */
    public static Object deserializeSerializableClass(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Utilizes Reflections to find all classes in a package that extend a given class.
     *
     * @param packageName the package to search in
     * @param clazz the class to find subclasses of
     * @return a set of classes that extend the given class
     */
    public static Set<Class<?>> findAllClassesInPackage(String packageName, Class<?> clazz) {
        Thread.currentThread().setContextClassLoader(clazz.getClassLoader());
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> classes = (Set<Class<?>>) reflections.getSubTypesOf(clazz);
        Thread.currentThread().setContextClassLoader(GlobalUtils.class.getClassLoader());
        return classes;
    }

    /**
     * Generates a random string of a given length using lowercase letters, digits, and some special characters.
     * @param length the length of the string to generate
     * @return the generated random string
     */
    public static String generateRandomString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789_.-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (chars.length() * Math.random());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Checks if two lists are equal in size and content.
     * @param list the first list
     * @param otherList the second list
     * @return true if the lists are equal, false otherwise
     */
    public static <T> boolean areListsEqual(List<T> list, List<T> otherList) {
        if (list.size() != otherList.size()) {
            return false;
        }
        return list.equals(otherList);
    }

    /**
     * Checks if two lists are similar in size and content, ignoring the order of elements.
     * @param list the first list
     * @param otherList the second list
     * @return true if the lists are similar, false otherwise
     * @param <T> the type of elements in the lists
     */
    public static <T> boolean areListsSimilar(List<T> list, List<T> otherList) {
        if (list.size() != otherList.size()) {
            return false;
        }
        Set<T> set = new HashSet<>(list);
        return set.containsAll(otherList);
    }

}
