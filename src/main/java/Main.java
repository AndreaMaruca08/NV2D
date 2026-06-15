import nv.components.NvCharacter;
import nv.components.NvCont;

import nv.components.shapes.dynamic.DynamicCircle;

import nv.core.Nv2DApp;
void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    var page = app.addAndSetPage("NewPage", NvCont.newPage());

    NvCharacter character = new NvCharacter(100,500, 100, 300);
    character.setWeight(100);

    var hex = new DynamicCircle(1000,1000, 150);
    page.addChild(hex);
    hex.setWeight(50);

    app.setKeyboardFocus(character);

    page.addChild(character);
    app.run();
}
