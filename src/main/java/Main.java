import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.core.graphic.NvGraphic;
import nv.test.benchmark.Benchmark;
import nv.test.benchmark.MovingCamera;

//Example
void main() {
    // build the game
    NvContext context = new ContextBuilder("TEST", 3000000,3000000)
            .setVsync(true)
            .setIdleWhenUnfocused(true)
            .build();
    // first page
    var page = context.newPage();
    page.setBackground(0,0,0);

    page.addChild(new Benchmark(10000, 5));
    var cam = new MovingCamera((int) (context.getRenderWidth()/2f), (int) (context.getRenderHeight()/2f));
    page.addChild(cam);
    cam.setNeedCamera(true);
    context.setKeyboardFocus(cam);
    NvGraphic.setCurrentCamera(cam.getCamera());

    // run the game
    context.run();
}