import nv.components.NvCharacter;
import nv.components.NvCont;

import nv.components.NvTimer;
import nv.components.shapes.dynamic.DynamicSquare;
import nv.components.shapes.dynamic.DynamicTriangle;
import nv.core.NvContext;
import nv.core.NvGraphic;
import nv.core.collision.CollisionSystem;
import nv.test.ClickTest;

void main() {
    var context = NvContext.createInstance("TESTING");

    context.setShowFPS(true);

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setChildrenFirst(true);
    page.setBackground(1,0.5f,0.5f);

    NvCharacter character = new NvCharacter(1000,500, 100, 300);
    character.setNeedCamera(true);
    character.setWeight(100);
    context.setKeyboardFocus(character);
    NvGraphic.setCurrentCamera(character.getCamera());

    var sq = new DynamicSquare(0,0, 10000, 50);
    sq.setWeight(CollisionSystem.MAX_WEIGHT);

    var tri = new DynamicTriangle(1000,1000, 100, 50);

    context.setFpsLimit(500);

    page.addChild(tri);
    page.addChild(sq);
    page.addChild(character);

    context.run();
}
