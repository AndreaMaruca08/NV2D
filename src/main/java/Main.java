import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.core.PostProcessSettings;
import nv.utils.camera.NvControlledCamera;

//Example
void main() {
    // build the game
    NvContext context = new ContextBuilder("START")
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
    context.run();
}