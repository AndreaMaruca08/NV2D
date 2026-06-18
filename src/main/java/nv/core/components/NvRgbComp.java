package nv.core.components;

import nv.core.annotations.EngineCore;
import nv.core.annotations.ReadyComponent;

/**
 * <p>Ready component with rgb variables ready with getters and setters</p>
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public abstract class NvRgbComp extends NvComp {
    protected float r=0, g=0, b=0;
    public NvRgbComp(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    public void setRgb(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float getR() {
        return r;
    }
    public float getG() {
        return g;
    }
    public float getB() {
        return b;
    }

    public void setR(float r) {
        this.r = r;
    }
    public void setG(float g) {
        this.g = g;
    }
    public void setB(float b) {
        this.b = b;
    }

}
