import nv.core.ContextBuilder;
import nv.core.NvContext;
import nv.core.ScreenSize;

import java.awt.*;

void main() {
    // builds the game
    NvContext context = new ContextBuilder("START", true /*, new Dimension(800, 800)*/) // <- app/game name
            .setVsync(true)
//          .setInternalResolution(ScreenSize._1920x1080) for specific uses
//          .setFpsLimit(30)
            .setIdleWhenUnfocused(true)
//          .configurePostProcess((settings) -> { for post processing
//             settings.presetVHS();
//          })
            .build();
    // first page
    var page = context.newPage();
    page.setBackgroundColor(0,0,0);

    //Add your components here using page.addChild([the component]);
    //it will be drawn and updated automatically

    // run the game
    context.run(); //  (don't put anything after this line, it won't be executed until the end)
}