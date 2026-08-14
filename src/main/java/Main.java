import nv.core.ContextBuilder;
import nv.core.NvContext;

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

    // run the game
    context.run();
}