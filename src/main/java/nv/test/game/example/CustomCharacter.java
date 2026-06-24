package nv.test.game.example;

import nv.core.NvContext;
import nv.core.assets.AtlasConverter;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.core.io.AudioManager;
import nv.core.io.GameSaveManager;
import nv.utils.NvCharacter;
import nv.utils.shapes.dynamic.NvLabel;
import org.lwjgl.glfw.GLFW;

import static nv.core.errors.NvLogger.logInfo;

public class CustomCharacter extends NvCharacter {
    //All images
    private final AtlasConverter.Atlas atlas;

    private final AtlasConverter.Region character;

    private int hp;
    private final NvLabel hplabel;

    public CustomCharacter(int x, int y, int w, int h, float velocity) {
        super(x, y, w, h, velocity);
        hp = 1000;
        NvContext context = NvContext.getInstance();
        atlas = context.assets().loadAtlas("undertale", "test/undertale");
        character = context.assets().getRegion("undertale", "undertaleHeart");

        hplabel = new NvLabel(context.getWidth()/2,100);
        hplabel.changeText("HP: " + hp);
        hplabel.setHUD(true);

        addChild(hplabel);
    }
    public void takeDamage(int amount){
        hp -= amount;
        hplabel.changeText("HP: " + hp);
    }
    @Override
    public void drawIntern(NvGraphic g){
        g.drawImageRegion(
                atlas.image(),
                0, 0,
                w,h,
                character.u1(), character.v1(),
                character.u2(), character.v2()
        );
    }
    private boolean playing = true;
    @Override
    public void update(float dt) {
        super.update(dt);
        // Example of saving and loading
        if(keys[GLFW.GLFW_KEY_ESCAPE]){
            GameSaveManager.save(new GameSave(x,y));
        }else if(keys[GLFW.GLFW_KEY_R]){
            GameSave cha = GameSaveManager.get(GameSave.class);
            this.x = cha.playerX();
            this.y = cha.playerY();
        }
        else if(keys[GLFW.GLFW_KEY_SPACE]){
            if(playing){
                AudioManager.stop("dialtone.mp3");
                playing = false;
            }else{
                AudioManager.playLoop("dialtone.mp3");
                playing = true;
            }
        }
        if(hp <= 0 && hplabel != null){
            destroy();
        }
    }
    private boolean shake = false;
    @Override
    public void whenCollide(NvComp other){
        if(!shake){
            camera.shake(20,100);
            shake = true;
        }

    }
}
