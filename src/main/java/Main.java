import nv.core.Nv2DApp;
import nv.test.PerformanceTest;

void main() {
    var app = Nv2DApp.createInstance("TESTING", 2_000_000, 3_000_000);
    app.setShowFPS(true);

    app.addTreeComponent(new PerformanceTest(0, 0, 200, 200));

    app.run();
}
