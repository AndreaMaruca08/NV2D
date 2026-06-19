package nv.test.game.example;

import nv.core.NvContext;
import nv.core.collision.Collidable;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

public class Portal extends NvComp implements Collidable {
    public Portal(int x, int y, int w, int h) {
        super(x, y, w, h);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(x, y, w, h);
    }

    @Override
    public void update(float dt) {

    }
    @Override
    public void whenCollide(NvComp other){
        if(other instanceof CustomCharacter character){
            var currentPage = NvContext.getInstance().getCurrentPage();
            currentPage.removeChild(character);
            currentPage.destroy();
            NvContext.getInstance().addAndSetPage("Battaglia", new BattleArena(character));
        }
    }
}
