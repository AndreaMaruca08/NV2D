import nv.components.NvCharacter;
import nv.components.NvCont;

import nv.components.shapes.dynamic.DynamicSquare;
import nv.core.NvContext;
import nv.core.NvGraphic;
import nv.core.collision.CollisionSystem;
import nv.test.BigOvalTest;

void main() {
    var context = NvContext.createInstance("aa");

    context.setShowFPS(true);
    context.setVsync(true);

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setChildrenFirst(true);
    page.setBackground(1,0.5f,0.5f);

    NvCharacter character = new NvCharacter(1000,500, 100, 300);
    character.setNeedCamera(true);
    character.setWeight(100);
    context.setKeyboardFocus(character);
    NvGraphic.setCurrentCamera(character.getCamera());

    var sq = new DynamicSquare(0,0, 10000, 50);
    var circ = new BigOvalTest(0,0, 100);
    sq.setWeight(CollisionSystem.MAX_WEIGHT);

    page.addChild(sq);
    page.addChild(circ);
    page.addChild(character);

    context.run();
}
