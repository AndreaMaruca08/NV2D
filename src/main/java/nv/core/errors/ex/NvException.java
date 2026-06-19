package nv.core.errors.ex;

import nv.core.annotations.EngineCore;

/**
 * <p>Root of all engine-related exceptions</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvException extends RuntimeException {
    private ExSeverity problem;
    public NvException(ExSeverity problem, String message) {
        super(problem.description + message);
    }
}
