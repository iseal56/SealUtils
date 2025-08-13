package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.systems.analytics.AnalyticsManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.Random;
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

    // List of made-up but real-sounding error messages
    private static final String[] FAKE_ERRORS = {
            "NETWORK_TIMEOUT_ERROR",
            "INVALID_PACKET_FORMAT",
            "AUTHENTICATION_FAILURE",
            "RESOURCE_LOCKED_EXCEPTION",
            "UNSUPPORTED_PROTOCOL_VERSION",
            "PAYLOAD_TOO_LARGE",
            "CONNECTION_RESET_BY_PEER",
            "MALFORMED_HEADER_EXCEPTION",
            "CHECKSUM_MISMATCH_ERROR",
            "RATE_LIMIT_EXCEEDED"
    };

    private static final Level[] ACCEPTABLE_LEVELS = {
            Level.SEVERE, Level.WARNING, Level.INFO
    };

    private static final Random RANDOM = new Random();

    @BeforeAll
    static void setUp() {
        SealUtils.init(true, "1.0.0.0-TESTVER");
        // Initialize the ExceptionHandler instance
        exceptionHandler = ExceptionHandler.getInstance();
        AnalyticsManager.INSTANCE.setEnabled("SealUtils", true);
        // test if I somehow broke the singleton pattern
        assertThat(exceptionHandler)
                .isNotNull()
                .isEqualTo(ExceptionHandler.getInstance());
        SealUtils.setDebug(true);
    }

    @Test
    void testPacketSending() {
        String randomError = FAKE_ERRORS[RANDOM.nextInt(FAKE_ERRORS.length)];
        Level randomLevel = ACCEPTABLE_LEVELS[RANDOM.nextInt(ACCEPTABLE_LEVELS.length)];
        ExceptionHandler.getInstance().dealWithException(
                new Exception("Test Exception"),
                randomLevel,
                randomError,
                "Test Info 1"
        );
    }

    @Test
    void testDealWithException() {
        try {
            // attempt dealing with test exception
            ArrayList<String> log = ExceptionHandler.getInstance().dealWithExceptionExtended(
                    new Exception("Test Exception"),
                    Level.INFO,
                    "TEST_ERROR_MESSAGE",
                    true,
                    Optional.empty(),
                    false,
                    "Test Info 1",
                    "Test Info 2"
            ).orElseThrow();
            assertThat(log)
                    .isNotNull()
                    .isExactlyInstanceOf(ArrayList.class)
                    .isNotEmpty()
                    .contains("Exception triggered by " + TestExceptionHandler.class.getName())
                    .contains("The exception message is Test Exception")
                    .contains("The error message is TEST_ERROR_MESSAGE")
                    .contains("More info 1: Test Info 1")
                    .contains("More info 2: Test Info 2")
                    .contains("Dump from: ADumpable -> name: ADumpable")
                    .contains("Dump from: AnotherDumpable -> name: AnotherDumpable");
        } catch (Exception e) {
            // throw an assertion error if the exception is not handled correctly
            throw new AssertionFailedError("Exception was not handled correctly", e);
        }
    }

}
