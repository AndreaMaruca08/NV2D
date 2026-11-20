import nv.core.components.NvCont;
import nv.core.components.NvRgbComp;
import nv.core.graphic.NvGraphic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

@DisplayName("NvRgbComp and NvCont Unit Tests")
public class NvRgbCompTest {

    static class MockRgbComp extends NvRgbComp {
        public MockRgbComp(int x, int y, int w, int h) {
            super(x, y, w, h);
        }
        @Override
        public void drawIntern(NvGraphic g) {}
        @Override
        public void update(float dt) {}
    }

    @Test
    @DisplayName("Test NvRgbComp RGB getters and setters")
    void testRgbGettersSetters() {
        MockRgbComp comp = new MockRgbComp(0, 0, 100, 100);

        Assertions.assertEquals(0f, comp.getR());
        Assertions.assertEquals(0f, comp.getG());
        Assertions.assertEquals(0f, comp.getB());

        comp.setRgb(0.5f, 0.7f, 0.9f);
        Assertions.assertEquals(0.5f, comp.getR(), 0.0001f);
        Assertions.assertEquals(0.7f, comp.getG(), 0.0001f);
        Assertions.assertEquals(0.9f, comp.getB(), 0.0001f);

        comp.setR(1.0f);
        comp.setG(0.0f);
        comp.setB(0.2f);
        Assertions.assertEquals(1.0f, comp.getR(), 0.0001f);
        Assertions.assertEquals(0.0f, comp.getG(), 0.0001f);
        Assertions.assertEquals(0.2f, comp.getB(), 0.0001f);
    }

    @Test
    @DisplayName("Test NvCont initialization and page creation")
    void testNvCont() {
        NvCont cont = new NvCont(10, 20, 300, 400);
        Assertions.assertEquals(10, cont.getX());
        Assertions.assertEquals(20, cont.getY());
        Assertions.assertEquals(300, cont.getW());
        Assertions.assertEquals(400, cont.getH());
        Assertions.assertEquals(1.0f, cont.getR(), 0.0001f);

        cont.setBackgroundColor(0.1f, 0.2f, 0.3f);
        Assertions.assertEquals(0.1f, cont.getR(), 0.0001f);
        Assertions.assertEquals(0.2f, cont.getG(), 0.0001f);
        Assertions.assertEquals(0.3f, cont.getB(), 0.0001f);

        NvCont page = NvCont.newPage();
        Assertions.assertEquals(0, page.getX());
        Assertions.assertEquals(0, page.getY());
        Assertions.assertEquals(0, page.getW());
        Assertions.assertEquals(0, page.getH());
    }
}
