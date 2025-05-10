package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestGlobalUtils {

    @Test
    void testListSimilarity() {
        // Test with two identical lists
        List<Integer> list1 = List.of(1, 2, 3, 4, 5);
        List<Integer> list2 = List.of(1, 2, 3, 4, 5);
        assertTrue(GlobalUtils.areListsEqual(list1, list2));
        assertTrue(GlobalUtils.areListsSimilar(list1, list2));

        // Test with two completely different lists
        list1 = List.of(1, 2, 3);
        list2 = List.of(4, 5, 6);
        assertFalse(GlobalUtils.areListsEqual(list1, list2));
        assertFalse(GlobalUtils.areListsSimilar(list1, list2));

        // Test with partially overlapping lists
        list1 = List.of(1, 2, 3);
        list2 = List.of(3, 2, 1);
        assertFalse(GlobalUtils.areListsEqual(list1, list2));
        assertTrue(GlobalUtils.areListsSimilar(list1, list2));
    }

    @Test
    void testFindAllClassesInPackage() {
        String packageName = "dev.iseal.sealUtils.testPackage";
        Set<Class<?>> classes = GlobalUtils.findAllClassesInPackage(packageName, Dumpable.class);
        // assert that the classes are found
        assertNotNull(classes);
        assertFalse(classes.isEmpty());

        // assert that the classes are of type Dumpable
        for (Class<?> clazz : classes) {
            assertTrue(Dumpable.class.isAssignableFrom(clazz));
        }
    }

}
