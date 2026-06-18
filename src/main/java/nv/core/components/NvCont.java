package nv.core.components;

import nv.core.annotations.EngineCore;
import nv.core.annotations.ReadyComponent;
import nv.core.graphic.NvGraphic;

import static nv.core.graphic.NvGraphic.camera;

/**
 * <h3>Empty container</h3>
 * <p>Simple empty component used for storing other readycomponents</p>
 * <p>Can also represent a page using .newPage()</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvCont extends NvRgbComp {
    private final boolean showBorder;

    public NvCont(int x, int y, int w, int h) {
        this(x, y, w, h, false);
    }

    public NvCont(int x, int y, int w, int h, boolean showBorder) {
        super(x, y, w, h);
        this.showBorder = showBorder;
        this.r = 1;
        this.g = 1;
        this.b = 1;
    }

    public void setBackground(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static NvCont newPage(){
        return new NvCont(0,0, 0,0, false);
    }
    public static NvCont newPage(boolean debugBorder){
        return new NvCont(0,0, 0,0, debugBorder);
    }
    @Override
    public void update(float dt) {}

    @Override
    public void drawIntern(NvGraphic g) {
        if(showBorder){
            g.drawRectBorder(0, 0, w, h, 20, 0.3f, 0.1f, 0.1f);
        }

        g.drawRect(camera.x, camera.y, w, h, r, this.g, b);
    }
}
