import nv.core.ContextBuilder;
import nv.core.EmptyKeyboardListener;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import nv.test.game.example.CustomCharacter;

import nv.utils.NvTimer;
import nv.utils.camera.NvCinematic;
import nv.utils.shapes.dynamic.DynamicSquare;

void main() {
    var context = new ContextBuilder("TEST")
            .setVsync(true)
            .showFps()
            .build();

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(1,1,1);

    CustomCharacter character = new CustomCharacter(1000,500, 100, 100, 1000);
    context.setKeyboardFocus(character);
    character.setWeight(100);

    NvCinematic cinematica = new NvCinematic(100,100,10,10000) {
        @Override
        public void updateCamera(float dt) {
            x += dt*1000;
        }
    };
    NvGraphic.setCurrentCamera(cinematica);
    cinematica.start();

    for(int i = 0; i < 100; i++){
        for(int j = 0; j < 100; j++){
            page.addChild(new DynamicSquare(100*i, 100*j, 50,50));
        }
    }

    page.addChild(character);

    context.run();
}
