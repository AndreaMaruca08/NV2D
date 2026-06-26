import nv.core.ContextBuilder;
import nv.core.ScreenSize;
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
import nv.utils.NvTextField;
import nv.utils.shapes.dynamic.DynamicCircle;
import nv.utils.shapes.dynamic.DynamicSquare;

import java.awt.*;

void main() {
    // build the game
    var context = new ContextBuilder("TEST")
            .setVsync(true)
            .showFps()
            .setInternalResolution(ScreenSize._1920x1080)
            .build();

    // first page

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(1f,0.5f,0.5f);

    CustomCharacter character = new CustomCharacter(1000,500, 100, 100, 1000);
    character.setNeedCamera(true);
    context.setKeyboardFocus(character);
    NvGraphic.setCurrentCamera(character.getCamera());
    character.setWeight(100);

    // audio loading so that it does not create latency during gameplay
    AudioManager.load("dialtone.mp3");
    AudioManager.setVolume("dialtone.mp3", 100);

    var p = new Portal(2000,500,100,100);

    var moving = new MovingComponent(-300,-300,400,100,
            100,200, Vector2D.RIGHT_DOWN, true);
    var obj = new DynamicSquare(-500,1000,100,100);
    obj.setRgb(0.5f,0.5f,0);

    var oval = new DynamicCircle(-1000,1,1000);

    var whenOver = new WhenOverCollision(-500, 0, 300, 300);

    var field = new NvTextField(-500, 500, 100, 100, Color.BLACK, Color.WHITE);

    // add components to the page
    page.addChild(oval);
    page.addChild(p);
    page.addChild(moving);
    page.addChild(whenOver);
    page.addChild(obj);
    page.addChild(new DynamicSquare(1000,1000,1000,300));
    page.addChild(new Wall(300,300,1000,30));
    page.addChild(character);
    page.addChild(field);

    // run the game
    context.run();
}