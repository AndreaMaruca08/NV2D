package nv.core.io;

import nv.core.annotations.EngineCore;

@EngineCore
@SuppressWarnings("unused")
public interface Clickable {
    void onClick(int x, int y);
    void onClickRelease(int x, int y);
}
