import nv.components.NvComp;
import nv.components.NvCont;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    var home = app.addAndSetPage("homePage", NvCont.newPage(true));
    var gamePage = app.addPage("gamePage", NvCont.newPage());

    gamePage.addChild(new NvComp(200,200,200,200) {
        @Override
        public void drawIntern(NvGraphic g) {
            g.drawRect(0,0,100,100, 1,0.5f,0);

            g.drawText("HELLO", 0,0);
        }

        @Override
        public void update(float dt) {

        }
    });


    app.setShowFPS(true);
    home.addChild(new NvComp(200,200,200,200) {
        @Override
        public void drawIntern(NvGraphic g) {
            g.drawRect(0,0,100,100, 1,0.5f,0);
            g.drawText("Ciao", 0,0);

        }

        @Override
        public void update(float dt) {
            app.setCurrentPage("gamePage");
        }
    });


    app.run();
}
