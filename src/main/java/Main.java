import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.core.components.NvCont;
//Example
void main() {
    // build the game
    NvContext context = new ContextBuilder("TEST")
            .setVsync(true)
            .setIdleWhenUnfocused(true)
            .build();
    // first page
    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(0,0,0);

    // run the game
    context.run();
}