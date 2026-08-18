package nv.core.components;

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

        /*
         * Store the size before ticking.
         *
         * Components added during the current tick will therefore
         * be processed starting from the next tick.
         */
        int n = allComponents.size();

        for (int i = 0; i < n; i++) {
            allComponents.get(i).tick(dt);
        }

        /*
         * Collect components that requested destruction.
         */
        toDestroy.clear();

        for (int i = 0; i < allComponents.size(); i++) {
            NvComp comp = allComponents.get(i);

            if (comp.shouldGetDestroyed) {
                toDestroy.add(comp);
            }
        }

        /*
         * Destroy components after the iteration.
         */
        for (NvComp comp : toDestroy) {

            /*
             * If an ancestor was already destroyed, this component
             * has already been removed from the flat list.
             */
            if (!allComponents.contains(comp)) {
                continue;
            }

            /*
             * NvComp handles the actual destruction recursively.
             */
            comp.actualDestroy();

            /*
             * Remove the component and its complete subtree from
             * the flat list.
             */
            removeFromFlatListRecursive(comp);

            /*
             * Remove the component from the hierarchy.
             */
            NvComp parent = comp.getParent();

            if (parent != null) {
                parent.getChildren().remove(comp);
            }
        }
    }

    /**
     * Removes a component and all of its descendants from the flat list.
     *
     * @param comp component to remove
     */
    private void removeFromFlatListRecursive(NvComp comp) {
        allComponents.remove(comp);

        for (NvComp child : comp.getChildren()) {
            removeFromFlatListRecursive(child);
        }
    }

    /**
     * Draws all components contained in this page.
     *
     * <p>
     * Camera culling is performed before drawing each component.
     * </p>
     *
     * @param g graphics context
     */
    public void drawAllComponents(NvGraphic g) {
        int n = allComponents.size();
        draw(g);

        for (int i = 0; i < n; i++) {
            NvComp comp = allComponents.get(i);

            if (camera.isComponentInRendering(comp)) {
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

        g.drawRect(
                camera.x,
                camera.y,
                getW(),
                getH(),
                r,
                this.g,
                b
        );
    }
}