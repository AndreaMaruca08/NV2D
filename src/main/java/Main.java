import nv.components.NvCharacter;
import nv.components.NvCont;
import nv.core.Nv2DApp;
import nv.test.CircleTest;
import nv.test.ClickTest;
import nv.test.DvdLogoBouncing;
import nv.test.Stateless;

void main() {
    var app = Nv2DApp.createInstance("TESTING", 10000000, 4000000);

    app.setShowFPS(true);

    var page = app.addAndSetPage("NewPage", NvCont.newPage());

    DvdLogoBouncing dvd = new DvdLogoBouncing(500,500);
    ClickTest clickAndKeyboardTest = new ClickTest(500,500, 100, 100);
    NvCharacter character = new NvCharacter(100,500, 100, 50);
    CircleTest circle = new CircleTest(2000, 800, 400, 400);

    Stateless stateless = new Stateless(1000, 1000, 200, 200);

    app.setKeyboardFocus(character);

    page.addChild(dvd);
    page.addChild(clickAndKeyboardTest);
    page.addChild(character);
    page.addChild(circle);
    page.addChild(stateless);

    app.run();
}
