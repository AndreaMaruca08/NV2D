package nv.core.errors.ex;

import nv.core.annotations.EngineCore;

/**
 * <p>Severity of the exception</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public enum ExSeverity {
    LOW("[LOW]"),
    MEDIUM("[MEDIUM]"),
    HIGH("[HIGH]");

    public final String description;
    ExSeverity(String desc) {
        this.description = desc;
    }
}
