import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import nv.test.DvdLogoBouncing;
import nv.test.game.example.CustomCharacter;

import nv.utils.shapes.dynamic.DynamicSquare;

void main() {
    var context = new ContextBuilder("TEST")
            .setVsync(true)
            .showFps()
            .build();

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(0.5f,0.5f,0.5f);

    CustomCharacter character = new CustomCharacter(1000,500, 100, 100, 1000);
    context.setKeyboardFocus(character);
    character.setNeedCamera(true);
    NvGraphic.setCurrentCamera(character.getCamera());
    character.setWeight(100);

    character.setZIndex(1);

    for(int i = 0; i < 100; i++){
        for(int j = 0; j < 100; j++){
            page.addChild(new DynamicSquare(10*i, 10*j, 5,5));
        }
    }

    DvdLogoBouncing d = new DvdLogoBouncing(500,500);
    page.addChild(d);

    page.addChild(character);

    context.run();
}
