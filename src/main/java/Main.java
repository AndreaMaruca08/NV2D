import nv.core.NvApp;
import nv.components.NvComponent;
import nv.components.NvGraphic;

void main() {
    NvApp.createInstance((dt) -> {

    });

    NvApp.getInstance().addTreeComponent(new NvComponent(500, 200) {
        @Override
        public void drawIntern(NvGraphic g) {
            g.drawTri(600, 200, 1000, 0, 0, 1);
            g.drawRect(200, 500, 200, 300, 0, 0, 1);
        }
    });


    NvApp.getInstance().run();
}
