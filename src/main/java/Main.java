import nv.components.NvCharacter;
import nv.components.NvCont;

import nv.components.shapes.dynamic.DynamicSquare;
import nv.core.NvContext;
import nv.core.NvGraphic;
import nv.test.ClickTest;

void main() {
    var context = NvContext.createInstance("TEST");

    context.setShowFPS(true);
    context.setVsync(false);

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setChildrenFirst(true);
    page.setBackground(1,0.5f,0.5f);

    NvCharacter character = new NvCharacter(1000,500, 100, 300);
    NvGraphic.setCurrentCamera(character.getCamera());
    character.setNeedCamera(true);

    var click = new ClickTest(0, 1000, 500, 100);
    click.setWeight(2);

    for(int i = 1; i < 50; i++){
        for(int j =1 ; j < 50; j++){
            var c = new DynamicSquare(i*20,100 * j,15,15);
            c.setRgb(0,0,0);
            page.addChild(c);
        }
    }

    var a = new DynamicSquare(100,100,10000,50);
    a.setWeight(40);
    a.setRgb(0,1,0);
    page.addChild(a);

    page.addChild(click);
    character.setWeight(100);
    context.setKeyboardFocus(character);

    page.addChild(character);

    context.run();
}
