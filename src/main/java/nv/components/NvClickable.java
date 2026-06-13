package nv.components;

import nv.core.Clickable;

public abstract class NvClickable extends NvComp implements Clickable {
    public NvClickable(int x, int y, int w, int h) {
        super(x, y, w, h);
    }
}
