import nv.components.NvCharacter;
import nv.components.camera.NvCinematic;
import nv.components.NvCont;

import nv.components.shapes.dynamic.DynamicCircle;
import nv.components.shapes.dynamic.DynamicSquare;
import nv.core.Nv2DApp;
import nv.core.NvGraphic;
import nv.core.collision.CollisionSystem;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    var page = app.addAndSetPage("NewPage", NvCont.newPage());
    page.setChildrenFirst(true);
    page.setBackground(1,0.5f,0.5f);

    NvCharacter character = new NvCharacter(1000,500, 100, 300);
    character.setNeedCamera(true);
    character.setWeight(100);

    var hex = new DynamicSquare(0,0, 10000, 50);
    hex.setWeight(CollisionSystem.MAX_WEIGHT);
    var circle = new DynamicCircle(1000,1000, 300);
    NvGraphic.setCurrentCamera(character.getCamera());

    page.addChild(circle);
    page.addChild(hex);

    app.setKeyboardFocus(character);

    page.addChild(character);


    app.run();
}
