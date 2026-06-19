package nv.core.errors.ex;

import nv.core.annotations.EngineCore;

/**
 * <p>Represents a low-severity problem, throw this when it's a minor issue but still needs attention.</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvLowProblemEx extends NvException {
    public NvLowProblemEx(String message) {
        super(ExSeverity.LOW, message);
    }
}
