package nv.test.game.example;

import nv.core.NvContext;
import nv.core.assets.AtlasConverter;
import nv.core.graphic.NvGraphic;
import nv.utils.NvCharacter;
import nv.utils.shapes.dynamic.NvLabel;

public class CustomCharacter extends NvCharacter {
    //All images
    private final AtlasConverter.Atlas atlas;

    private final AtlasConverter.Region character;

    private int hp;
    private NvLabel hplabel;

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
    @Override
    public void update(float dt) {
        super.update(dt);
        if(hp <= 0 && hplabel != null){
            destroy();
        }
    }
}
