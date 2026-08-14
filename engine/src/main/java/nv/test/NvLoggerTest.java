package nv.test;

import nv.core.errors.NvLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvLogger Unit Tests")
public class NvLoggerTest {

    @BeforeEach
    void setUp() {
        NvLogger.initialize("TestApp", 1, 0, 0);
    }

    @Test
    @DisplayName("Test logging methods complete without throwing exceptions")
    void testLogMethods() {
        MockComp comp = new MockComp(0, 0, 10, 10);

        assertDoesNotThrow(() -> NvLogger.logInfo("Test info message"));
        assertDoesNotThrow(() -> NvLogger.logInfo((Object) "Test info object"));
        assertDoesNotThrow(() -> NvLogger.logInfo(comp, "Component info"));

        assertDoesNotThrow(() -> NvLogger.logWarn("Test warning message"));
        assertDoesNotThrow(() -> NvLogger.logWarn((Object) 12345));
        assertDoesNotThrow(() -> NvLogger.logWarn(comp, "Component warning"));

        assertDoesNotThrow(() -> NvLogger.logErr("Test error message"));
        assertDoesNotThrow(() -> NvLogger.logErr((Object) new RuntimeException("Test err")));
        assertDoesNotThrow(() -> NvLogger.logErr(comp, "Component error"));

        assertDoesNotThrow(() -> NvLogger.logEngine("Test engine message"));
        assertDoesNotThrow(() -> NvLogger.logEngine((Object) "Engine state"));
        assertDoesNotThrow(() -> NvLogger.logEngine(comp, "Component engine message"));
    }

    @Test
    @DisplayName("Test default initialization when uninitialized")
    void testDefaultInitialization() {
        assertDoesNotThrow(() -> NvLogger.initialize());
    }
}
