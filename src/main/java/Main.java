import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.core.graphic.NvGraphic;
import nv.core.io.AudioManager;
import nv.test.game.example.AnimationTest;
import nv.test.game.example.CustomCharacter;
import nv.test.game.example.Wall;

void main() {
    // build the game
    var context = new ContextBuilder("TEST")
            .setVsync(true)
            .build();
    // first page

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(1f,0.5f,0.5f);

    AnimationTest sprite = new AnimationTest(1000,500, 100, 100);
    page.addChild(sprite);

    CustomCharacter character = new CustomCharacter(1000,500, 100, 100, 1000);
    character.setNeedCamera(true);
    context.setKeyboardFocus(character);
    NvGraphic.setCurrentCamera(character.getCamera());
    character.setWeight(100);

    // audio loading so that it does not create latency during gameplay
    AudioManager.load("dialtone.mp3");
    AudioManager.setVolume("dialtone.mp3", 100);

    // add components to the page
    page.addChild(new Wall(300,300,1000,30));
    page.addChild(character);

    // run the game
    context.run();
}