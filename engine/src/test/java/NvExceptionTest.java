import nv.core.errors.ex.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

@DisplayName("NvException Hierarchy Unit Tests")
public class NvExceptionTest {

    @Test
    @DisplayName("Test ExSeverity description formatting")
    void testExSeverityDescriptions() {
        Assertions.assertEquals("[LOW]", ExSeverity.LOW.description);
        Assertions.assertEquals("[MEDIUM]", ExSeverity.MEDIUM.description);
        Assertions.assertEquals("[HIGH]", ExSeverity.HIGH.description);
    }

    @Test
    @DisplayName("Test base NvException message formatting")
    void testNvExceptionMessage() {
        NvException ex = new NvException(ExSeverity.HIGH, "Critical failure");
        Assertions.assertTrue(ex.getMessage().contains("[HIGH] Critical failure"));
        Assertions.assertTrue(ex instanceof RuntimeException);
    }

    @Test
    @DisplayName("Test subclass exceptions severity and message")
    void testSubclassExceptions() {
        NvLogicEx logicEx = new NvLogicEx("Invalid state");
        Assertions.assertTrue(logicEx.getMessage().contains("[LOW] Invalid state"));
        Assertions.assertTrue(logicEx instanceof NvException);

        NvLowProblemEx lowEx = new NvLowProblemEx("Minor issue");
        Assertions.assertTrue(lowEx.getMessage().contains("[LOW] Minor issue"));

        NvMediumProblemEx medEx = new NvMediumProblemEx("Medium issue");
        Assertions.assertTrue(medEx.getMessage().contains("[MEDIUM] Medium issue"));

        NvHighProblemEx highEx = new NvHighProblemEx("High issue");
        Assertions.assertTrue(highEx.getMessage().contains("[HIGH] High issue"));
    }
}
