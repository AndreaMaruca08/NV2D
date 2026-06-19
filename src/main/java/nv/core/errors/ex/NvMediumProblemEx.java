package nv.core.errors.ex;

import nv.core.annotations.EngineCore;

/**
 * <p>Represents a medium-severity problem, throw this when it's an issue and needs attention.</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvMediumProblemEx extends NvException {
    public NvMediumProblemEx(String message) {
        super(ExSeverity.MEDIUM, message);
    }
}
