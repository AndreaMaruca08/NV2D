package nv.test;

import nv.components.NvComp;
import nv.core.NvContext;
import nv.core.NvGraphic;

public class PerformanceTest extends NvComp{
    private int rectNumber = 0;
    public PerformanceTest() {
        super(500,500,1000,1000);
        childrenFirst = false;
    }

    @Override
    public void drawIntern(NvGraphic g) {}

    @Override
    public void update(float dt) {
        var app = NvContext.getInstance();
        var h = app.getHeight();

        int randomY = (int) (Math.random() * (double)h);
        addChild(new DvdLogoBouncing(0, randomY));
        rectNumber++;
        if(rectNumber % 1000 == 0){
            System.out.println(rectNumber);
        }
    }
}
