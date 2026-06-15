import nv.components.NvCharacter;
import nv.components.NvCont;

import nv.components.shapes.statics.StaticSquare;
import nv.core.Nv2DApp;

import java.awt.*;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    var page = app.addAndSetPage("NewPage", NvCont.newPage());
    page.setChildrenFirst(true);
    page.setBackground(1,0.5f,0.5f);

    NvCharacter character = new NvCharacter(100,500, 100, 300);
    character.setWeight(100);

    var hex = new StaticSquare(0,0, 10000, 50);
    page.addChild(hex);

    app.setKeyboardFocus(character);

    page.addChild(character);
    app.run();
}
