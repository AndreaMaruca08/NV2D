package nv.core.errors.ex;

import nv.core.annotations.EngineCore;

/**
 * <p>Represents a high-severity problem, throw this when it's a big issue and needs attention.</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvHighProblemEx extends NvException {
    public NvHighProblemEx(String message) {
        super(ExSeverity.HIGH, message);
    }
}

