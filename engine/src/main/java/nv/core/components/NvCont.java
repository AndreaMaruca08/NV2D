
package nv.core.components;

import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.graphic.NvGraphic;

import java.util.ArrayList;
import java.util.List;

import static nv.core.graphic.NvGraphic.camera;

/**
 * <h3>Empty container</h3>
 * <p>
 * Simple empty component used for storing other components.
 * </p>
 * <p>
 * Can also represent a page using {@link #newPage()}.
 * </p>
 *
 * <p>
 * The container maintains a flat list of all components belonging
 * to the page, allowing update and rendering without recursively
 * traversing the component tree.
 * </p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public class NvCont extends NvRgbComp {

    private final boolean showBorder;

    /**
     * Flat list containing every component in this page.
     *
     * <p>
     * The list is managed by {@link NvComp#addChild(NvComp)} and
     * {@link NvComp#removeChild(NvComp)} through the root component
     * reference.
     * </p>
     */
    private final List<NvComp> allComponents;

    /**
     * Components that requested destruction during the current tick.
     */
    private final List<NvComp> toDestroy;

    public NvCont(int x, int y, int w, int h) {
        this(x, y, w, h, false);
    }

    public NvCont(int x, int y, int w, int h, boolean showBorder) {
        super(x, y, w, h);
        setHUD(true);

        this.showBorder = showBorder;

        this.allComponents = new ArrayList<>();
        this.toDestroy = new ArrayList<>();

        this.r = 1;
        this.g = 1;
        this.b = 1;

        this.setRootComponentList(allComponents);
    }

    /**
     * Returns the flat list containing all components in this page.
     *
     * @return flat component list
     */
    public List<NvComp> getAllComponents() {
        return allComponents;
    }

    /**
     * Sets the background color of the container.
     *
     * @param r red component
     * @param g green component
     * @param b blue component
     */
    public void setBackgroundColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Creates an empty page.
     *
     * @return new empty page
     */
    public static NvCont newPage() {
        return new NvCont(0, 0, 0, 0, false);
    }

    /**
     * Creates an empty page with an optional debug border.
     *
     * @param debugBorder whether the page should display its border
     * @return new empty page
     */
    public static NvCont newPage(boolean debugBorder) {
        return new NvCont(0, 0, 0, 0, debugBorder);
    }

    /**
     * Updates every component in the flat list.
     *
     * <p>
     * Components destroyed during the update are removed only after
     * the iteration has completed.
     * </p>
     *
     * @param dt delta time
     */
    public void tickAllComponents(float dt) {
        int n = allComponents.size();

        for (int i = 0; i < n; i++) {
            allComponents.get(i).tick(dt);
        }

        toDestroy.clear();

        for (int i = 0; i < allComponents.size(); i++) {
            NvComp comp = allComponents.get(i);

            if (comp.shouldGetDestroyed) {
                toDestroy.add(comp);
            }
        }

        for (NvComp comp : toDestroy) {
            if (!allComponents.contains(comp)) {
                continue;
            }

            comp.actualDestroy();
            removeFromFlatListRecursive(comp);

            NvComp parent = comp.getParent();
            if (parent != null) {
                parent.getChildren().remove(comp);
            }
        }
    }

    private void removeFromFlatListRecursive(NvComp comp) {
        allComponents.remove(comp);

        for (NvComp child : comp.getChildren()) {
            removeFromFlatListRecursive(child);
        }
    }

    public void drawAllComponents(NvGraphic g) {
        draw(g);

        float renderW = g.getRenderWidth();
        float renderH = g.getRenderHeight();
        float safeZoom = Math.max(camera.zoom, 0.0001f);
        float viewW = renderW / safeZoom;
        float viewH = renderH / safeZoom;

        int size = allComponents.size();
        for (int i = 0; i < size; i++) {
            NvComp comp = allComponents.get(i);

            if (camera.isComponentInRendering(comp, renderW, renderH, viewW, viewH)) {
                comp.draw(g);
            }
        }
    }

    @Override
    public void update(float dt) {
    }

    @Override
    public void drawIntern(NvGraphic g) {
        if (showBorder) {
            g.drawRectBorder(
                    0,
                    0,
                    getW(),
                    getH(),
                    20,
                    0.3f,
                    0.1f,
                    0.1f
            );
        }

        // Disegna lo sfondo partendo da (0, 0) fino a tutta la larghezza e altezza dello schermo
        g.drawRect(
                0,
                0,
                getW(),
                getH(),
                r,
                this.g,
                b
        );
    }
}