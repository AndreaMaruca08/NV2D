package nv.core.errors.ex;

import nv.core.errors.NvLogger;

public class EngineEx extends NvException {
    public EngineEx(String message) {
        NvLogger.logErr("Internal engine error: " + message);
        super(ExSeverity.HIGH, message);
    }
}
