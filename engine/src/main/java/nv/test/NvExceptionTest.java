package nv.test;

import nv.core.errors.ex.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvException Hierarchy Unit Tests")
public class NvExceptionTest {

    @Test
    @DisplayName("Test ExSeverity description formatting")
    void testExSeverityDescriptions() {
        assertEquals("[LOW]", ExSeverity.LOW.description);
        assertEquals("[MEDIUM]", ExSeverity.MEDIUM.description);
        assertEquals("[HIGH]", ExSeverity.HIGH.description);
    }

    @Test
    @DisplayName("Test base NvException message formatting")
    void testNvExceptionMessage() {
        NvException ex = new NvException(ExSeverity.HIGH, "Critical failure");
        assertTrue(ex.getMessage().contains("[HIGH] Critical failure"));
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    @DisplayName("Test subclass exceptions severity and message")
    void testSubclassExceptions() {
        NvLogicEx logicEx = new NvLogicEx("Invalid state");
        assertTrue(logicEx.getMessage().contains("[LOW] Invalid state"));
        assertTrue(logicEx instanceof NvException);

        NvLowProblemEx lowEx = new NvLowProblemEx("Minor issue");
        assertTrue(lowEx.getMessage().contains("[LOW] Minor issue"));

        NvMediumProblemEx medEx = new NvMediumProblemEx("Medium issue");
        assertTrue(medEx.getMessage().contains("[MEDIUM] Medium issue"));

        NvHighProblemEx highEx = new NvHighProblemEx("High issue");
        assertTrue(highEx.getMessage().contains("[HIGH] High issue"));
    }
}
