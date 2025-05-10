package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.SealUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExceptionHandler {

    private static ExceptionHandler exceptionHandler;
    private static final String TEST_DUMPABLE_PACKAGE = "dev.iseal.sealUtils.testPackage";
    private static final Set<Dumpable> dumpables = (Set<Dumpable>) GlobalUtils.findAllClassesInPackage(TEST_DUMPABLE_PACKAGE, Dumpable.class).stream().map(clazz -> {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new AssertionFailedError("Failed to create instance of Dumpable class", e);
        }
    }).collect(Collectors.toSet());

    @BeforeAll
    static void setUp() {
        // Initialize the ExceptionHandler instance
        exceptionHandler = ExceptionHandler.getInstance();
        // test if I somehow broke the singleton pattern
        assertThat(exceptionHandler)
                .isNotNull()
                .isEqualTo(ExceptionHandler.getInstance());
        SealUtils.setDebug(true);
    }

    @Test
    void testDealWithException() {
        try {
            // attempt dealing with test exception
            ArrayList<String> log = ExceptionHandler.getInstance().dealWithExceptionExtended(
                    new Exception("Test Exception"),
                    Level.INFO,
                    "TEST_ERROR_MESSAGE",
                    Logger.getLogger("TestLogger"),
                    true,
                    "Test Info 1",
                    "Test Info 2"
            ).orElseThrow();
            assertThat(log)
                    .isNotNull()
                    .isExactlyInstanceOf(ArrayList.class)
                    .isNotEmpty()
                    .contains("[SealUtils] Exception triggered by " + TestExceptionHandler.class.getName())
                    .contains("[SealUtils] The exception message is Test Exception")
                    .contains("[SealUtils] The error message is TEST_ERROR_MESSAGE")
                    .contains("[SealUtils] More info 1: Test Info 1")
                    .contains("[SealUtils] More info 2: Test Info 2")
                    .contains("[SealUtils] Dump from: ADumpable -> name: ADumpable")
                    .contains("[SealUtils] Dump from: AnotherDumpable -> name: AnotherDumpable");
        } catch (Exception e) {
            // throw an assertion error if the exception is not handled correctly
            throw new AssertionFailedError("Exception was not handled correctly", e);
        }
    }

}
