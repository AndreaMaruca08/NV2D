package nv.test;

import nv.components.NvComp;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;

public class PerformanceTest extends NvComp{
    private int rectNumber = 0;
    public PerformanceTest(int x, int y, int w, int h) {
        super(x, y, w, h);
        childrenFirst = false;
    }

    @Override
    public void drawIntern(NvGraphic g) {}

    @Override
    public void update(float dt) {
        var app = Nv2DApp.getInstance();
        var h = app.getHeight();

        int randomY = (int) (Math.random() * (double)h);
        addChild(new TestFigure(0, randomY, 200, 200, randomY));
        rectNumber++;
        if(rectNumber % 1000 == 0){
            System.out.println(rectNumber);
        }
    }
}
