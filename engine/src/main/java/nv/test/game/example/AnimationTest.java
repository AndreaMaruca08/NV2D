package nv.test.game.example;

import nv.core.NvContext;
import nv.core.animation.NvAnimation;
import nv.core.animation.NvSprite;

public class AnimationTest extends NvSprite {
    private final NvAnimation anim;
    public AnimationTest(int x, int y, int w, int h) {
        var atlas = NvContext.getInstance().assets().loadAtlas("flags", "test");
        super(x, y, w, h, atlas.image(), "flags", "AD");

        anim = new NvAnimation(this, 100, "AG", "AI", "AL", "AM", "AT");
        anim.start();
    }
    @Override
    public void whenDestroyed(){
        anim.destroy();
    }
}
