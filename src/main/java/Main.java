import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import nv.core.io.AudioManager;
import nv.test.DvdLogoBouncing;
import nv.test.MovingComponent;
import nv.test.WhenOverCollision;
import nv.test.game.example.CustomCharacter;

import nv.test.game.example.Portal;
import nv.test.game.example.Wall;
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

    AudioManager.load("dialtone.mp3");
    AudioManager.setVolume("dialtone.mp3", 100);

    DvdLogoBouncing d = new DvdLogoBouncing(500,500);
    d.setWeight(100);
    page.addChild(d);

    page.addChild(new DynamicSquare(1000,1000,1000,300));

    page.addChild(new Wall(300,300,200,30));

    page.addChild(character);
    var p = new Portal(2000,500,100,100);
    page.addChild(p);

    var moving = new MovingComponent(-300,-300,400,100,
            100,200, Vector2D.RIGHT_DOWN, true);
    page.addChild(moving);

    var obj = new DynamicSquare(-500,1000,100,100);
    obj.setRgb(0.5f,0.5f,0);

    var whenOver = new WhenOverCollision(-500, 0, 300, 300);

    page.addChild(whenOver);
    page.addChild(obj);


    context.run();
}
