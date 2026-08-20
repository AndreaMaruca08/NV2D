package nv.core.io;

import nv.core.annotations.EngineCore;

/**
 * Implement this interface to make your component clickable<br>
 * for example, it can be used to make a button<br>
 * <p>To add a {@link Clickable} do: {@link ClickSystem}.addClickable(NvComp comp)</p>
 * {@snippet :
 *     import nv.core.components.NvComp;
 *     import nv.core.graphic.NvGraphic;
 *     import nv.core.io.Clickable;
 *
 *     import static nv.core.errors.NvLogger.logInfo;
 *
 *     public class ClickExample extends NvComp implements Clickable {
 *        public ClickExample(int x, int y, int w, int h) {
 *            super(x, y, w, h);
 *        }
 *
 *        @Override
 *        public void drawIntern(NvGraphic g) {
 *            g.setRGB(1,0,0);
 *            g.drawRect(0,0,getW(),getH());
 *        }
 *
 *        @Override
 *        public void update(float dt) {
 *
 *        }
 *        //gets called when the mouse is clicked on this component
 *        @Override
 *        public void onClick(int x, int y) {
 *            logInfo("Clicked at: " + x + ", " + y);
 *        }
 *        //gets called when the mouse is released on this component
 *        @Override
 *        public void onClickRelease(int x, int y) {
 *            logInfo("Released at: " + x + ", " + y);
 *        }
 *     }
 * }
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public interface Clickable {
    void onClick(int x, int y);
    void onClickRelease(int x, int y);
}
