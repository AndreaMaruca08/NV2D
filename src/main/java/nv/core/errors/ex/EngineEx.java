package nv.core.errors.ex;

import nv.core.annotations.EngineCore;
import nv.core.errors.NvLogger;

/**
 * Represents an internal engine error.
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class EngineEx extends NvException {
    public EngineEx(String message) {
        NvLogger.logErr("Internal engine error: " + message);
        super(ExSeverity.HIGH, message);
    }
}
