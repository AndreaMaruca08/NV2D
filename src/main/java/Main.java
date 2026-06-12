import nv.core.NvApp;
import nv.core.components.NvComponent;
import nv.core.drawing.NvGraphic;

void main() {
    var app = NvApp.getInstance();

    app.addTreeComponent(new NvComponent(200, 200) {
        @Override
        public void drawIntern(NvGraphic g) {
            g.drawTri(380, 80, 0.5f, 0.6f, 1.0f);
            g.drawRect(
                    400,
                    500,
                    100,
                    200,
                    0.5f, 0.6f, 0.0f);
            g.drawText("ciao", 400, 500);
        }
    });


    app.run();
}
