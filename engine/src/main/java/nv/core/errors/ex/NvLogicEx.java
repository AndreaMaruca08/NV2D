package nv.core.errors.ex;

import nv.core.errors.NvLogger;

/**
 * Represents a logic error.
 * @since 1.0
 * @author Andrea Maruca
 */
public class NvLogicEx extends NvException {
    public NvLogicEx(String message) {
        NvLogger.logErr("Logic error: "+message);
        super(ExSeverity.LOW, message);
    }
}
