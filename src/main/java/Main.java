import nv.core.ContextBuilder;
import nv.core.NvContext;

void main() {
    // builds the game
    NvContext context = new ContextBuilder("START") // <- app/game name
            .setVsync(true)
            .setIdleWhenUnfocused(true)
//          .configurePostProcess((settings) -> { for post processing
//              settings.enableCRT(100, 1);
//          })
            .build();
    // first page
    var page = context.newPage();
    page.setBackgroundColor(0,0,0);

    //Add your components here using page.addChild([the component]);
    //it will be drawn automatically

    // run the game
    context.run(); //  (don't put anything after this line, it won't be executed until the end)
}