import nv.components.NvCharacter;
import nv.components.NvCont;
import nv.core.Nv2DApp;
import nv.test.ClickTest;
import nv.test.DvdLogoBouncing;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    app.setShowFPS(true);

    var page = app.addAndSetPage("NewPage", NvCont.newPage());

    DvdLogoBouncing dvd = new DvdLogoBouncing(500,500);
    ClickTest clickAndKeyboardTest = new ClickTest(500,500, 100, 100);
    NvCharacter character = new NvCharacter(100,500, 100, 50);
    app.setKeyboardFocus(character);


    page.addChild(dvd);
    page.addChild(clickAndKeyboardTest);
    page.addChild(character);


    app.run();
}
