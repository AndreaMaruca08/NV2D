package nv.utils.shapes.dynamic;

import nv.core.NvContext;
import nv.core.components.NvComp;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;

public class NvLabel extends NvRgbComp {
    private String text;
    public NvLabel(int x, int y) {
        super(x, y, 0,0);
    }
    public void changeText(String newText){
        this.text = newText;
        NvContext.markSceneDirty();
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.setRGB(r,this.g,b);
        g.drawText(text, 0,0);
    }

    @Override
    public void update(float dt) {

    }
}
