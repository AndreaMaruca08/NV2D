package nv.core.io;

import nv.core.annotations.EngineCore;

@EngineCore
@SuppressWarnings("unused")
public interface Clickable {
    void onClick();
    void onClickRelease();
}
