package nv.core.errors.ex;

import nv.core.errors.NvLogger;

public class NvLogicEx extends NvException {
    public NvLogicEx(String message) {
        NvLogger.logErr("Logic error: "+message);
        super(ExSeverity.LOW, message);
    }
}
