import nv.core.Nv2DApp;
import nv.test.PerformanceTest;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    app.getCurrentPage().addChild(new PerformanceTest(0,0,0,0));

    app.run();
}
