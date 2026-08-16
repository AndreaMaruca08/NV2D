import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.test.benchmark.Benchmark;

//Example
void main() {
    // build the game
    NvContext context = new ContextBuilder("TEST")
            .setVsync(true)
            .setIdleWhenUnfocused(true)
            .build();
    // first page
    var page = context.newPage();
    page.setBackground(0,0,0);

    page.addChild(new Benchmark(1000, 2));

    // run the game
    context.run();
}