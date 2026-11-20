import nv.core.errors.NvLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

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

        Assertions.assertDoesNotThrow(() -> NvLogger.logInfo("Test info message"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logInfo((Object) "Test info object"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logInfo(comp, "Component info"));

        Assertions.assertDoesNotThrow(() -> NvLogger.logWarn("Test warning message"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logWarn((Object) 12345));
        Assertions.assertDoesNotThrow(() -> NvLogger.logWarn(comp, "Component warning"));

        Assertions.assertDoesNotThrow(() -> NvLogger.logErr("Test error message"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logErr((Object) new RuntimeException("Test err")));
        Assertions.assertDoesNotThrow(() -> NvLogger.logErr(comp, "Component error"));

        Assertions.assertDoesNotThrow(() -> NvLogger.logEngine("Test engine message"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logEngine((Object) "Engine state"));
        Assertions.assertDoesNotThrow(() -> NvLogger.logEngine(comp, "Component engine message"));
    }

    @Test
    @DisplayName("Test default initialization when uninitialized")
    void testDefaultInitialization() {
        Assertions.assertDoesNotThrow(() -> NvLogger.initialize());
    }
}
