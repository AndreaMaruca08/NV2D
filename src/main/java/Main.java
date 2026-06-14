import nv.core.Nv2DApp;
import nv.test.DvdLogoBouncing;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    app.getCurrentPage().addChild(new DvdLogoBouncing(0,0));

    app.run();
}
