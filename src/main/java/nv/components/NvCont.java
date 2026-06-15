package nv.components;

import nv.core.NvGraphic;

/**
 * <h3>Empty container</h3>
 * <p>Simple empty component used for storing other components</p>
 * <p>Can also represent a page using .newPage()</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class NvCont extends NvComp {
    private final boolean showBorder;

    public NvCont(int x, int y, int w, int h) {
        this(x, y, w, h, false);
    }

    public NvCont(int x, int y, int w, int h, boolean showBorder) {
        super(x, y, w, h);
        this.showBorder = showBorder;
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
    }
}
