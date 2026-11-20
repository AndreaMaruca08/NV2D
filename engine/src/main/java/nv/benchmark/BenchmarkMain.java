import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.benchmark.Benchmark;
import nv.utils.camera.NvControlledCamera;

//BENCHMARK MAIN
void main() {
    NvContext context = new ContextBuilder("Benchmark", true, 6000000,6000000)
            .setVsync(true)
            .setIdleWhenUnfocused(true)
            .build();

    var page = context.newPage();
    page.setBackgroundColor(0,0,0);

    page.addChild(new Benchmark(4000, 5));

    new NvControlledCamera((int)(context.getRenderWidth()/2), (int)(context.getRenderHeight()/2), 1000);

    context.run();
}