package nv.core.io;

import nv.core.annotations.EngineCore;

/**
 * Interface for components that can be hovered over.
 * {@snippet :
 * import nv.core.components.NvComp;
 * import nv.core.graphic.NvGraphic;
 * import nv.core.io.Hoverable;
 *
 * import static nv.core.errors.NvLogger.logInfo;
 *
 * public class HoverExample extends NvComp implements Hoverable {
 *     public HoverExample(int x, int y, int w, int h) {
 *         super(x, y, w, h);
 *     }
 *
 *     @Override
 *     public void drawIntern(NvGraphic g) {
 *         g.setRGB(1,0,0);
 *         g.drawRect(0,0,getW(),getH());
 *     }
 *
 *     @Override
 *     public void update(float dt) {
 *         // Now that the component, we can check if it is hovered
 *         // also get the coordinates with hoveredX and hoveredY
 *         if(isHovered) {
 *             logInfo("Hovered at x: " + hoveredX + ", y: " + hoveredY);
 *         }
 *     }
 * }
 *
 * }
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
public interface Hoverable {}
