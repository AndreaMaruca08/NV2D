import nv.components.NvClickable;
import nv.components.NvCont;
import nv.components.NvGraphic;
import nv.core.Nv2DApp;

void main() {
    var app = Nv2DApp.createInstance("TESTING");

    var home = app.addAndSetPage("homePage", NvCont.newPage());

    app.setShowFPS(true);
    home.addChild(new ProvaComponent( 200,200,200,200));

    app.run();
}

static class ProvaComponent extends NvClickable {

    private float r=0,g=0,b=0;
    String text;
    Nv2DApp app = Nv2DApp.getInstance();

    public ProvaComponent(int x, int y, int w, int h) {
        super(x, y, w, h);
        this.r = 0;
        this.g = 0.5f;
        this.b = 0;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawRect(0, 0, 200, 200, r, this.g, b);
        g.drawText(text, 0, 0);
    }
    @Override
    public void mouseEnter(){
        r = 1.0f;
        this.g = 0.0f;
        b = 0.0f;
        text = "cliccato";
    }
    @Override
    public void mouseOut(){
        r = 0.0f;
        this.g = 0.0f;
        b = 1.0f;
        text = "negro";
    }

    boolean front = true;
    @Override
    public void update(float dt) {
        if(front){
            x -= 2;
        }else {
            x += 2;
        }
        if(x+w >= app.getWidth() || x <= 0){
            front = !front;
        }
    }

    @Override
    public void onClick() {
        System.out.println("cliccato");
    }
}
