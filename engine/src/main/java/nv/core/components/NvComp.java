package nv.core.components;

import nv.core.AppendableGeometry;
import nv.core.Drawable;
import nv.core.NvContext;
import nv.core.Updatable;
import nv.core.annotations.EngineCore;
import nv.core.collision.Collidable;
import nv.core.collision.CollisionManager;
import nv.core.collision.CollisionSystem;
import nv.core.errors.ex.NvLogicEx;
import nv.core.graphic.NvGraphic;
import nv.core.io.ClickSystem;
import nv.core.io.Clickable;
import nv.core.io.HoverSystem;
import nv.core.io.Hoverable;

import java.util.ArrayList;
import java.util.List;

import static nv.core.errors.NvLogger.logInfo;
import static nv.core.graphic.NvGraphic.camera;

/**
 * <h3>Root of the component tree</h3>
 * <p>Base class for all components in the component tree. Uses a flat array per page for efficient iteration.</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public abstract class NvComp implements Updatable, Drawable {
    private NvComp parent;
    private final List<NvComp> children;
    private List<NvComp> rootComponentList;
    private float x, y, w, h;
    protected boolean isHovered;
    protected boolean childrenFirst;
    public float rotation = 0;
    public float pivotX = 0.5f;
    public float pivotY = 0.5f;
    protected float hoveredX = -1;
    protected float hoveredY = -1;
    protected int weight = CollisionSystem.NO_WEIGHT;
    public boolean border = false;
    protected boolean isHUD = false;
    protected boolean phaseThrough = false;
    protected int zIndex = 0;
    boolean shouldGetDestroyed = false;
    private boolean dirty = false;

    public NvComp(float x, float y, float w, float h) {
        children = new ArrayList<>();
        this.parent = null;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean isPhaseThrough() {
        return phaseThrough;
    }

    public void setPhaseThrough(boolean phaseThrough) {
        this.phaseThrough = phaseThrough;
        markDirty();
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
        markDirty();
    }

    public boolean isHUD() {
        return isHUD;
    }

    public void setHUD(boolean HUD) {
        if(this instanceof Collidable)
            throw new NvLogicEx("Collidable components cannot be set as HUD");
        if (isHUD != HUD) {
            isHUD = HUD;
            markDirty();
        }
    }

    public boolean isChildrenFirst() {
        return childrenFirst;
    }

    public int getWeight() {
        return weight;
    }

    public List<NvComp> getChildren() {
        return children;
    }

    public NvComp getParent(){
        return parent;
    }

    public float getY() {
        return y;
    }

    public float getX() {
        return x;
    }

    public void setWeight(int weight) {
        this.weight = weight;
        markDirty();
    }

    public float getH() {
        return h;
    }

    public float getW() {
        return w;
    }

    protected void setParent(NvComp parent){
        this.parent = parent;
    }

    public void setChildrenFirst(boolean childrenFirst) {
        this.childrenFirst = childrenFirst;
        markDirty();
    }

    public void setX(float x) {
        if (this.x != x) {
            this.x = x;
            markDirty();
        }
    }

    public void setY(float y) {
        if (this.y != y) {
            this.y = y;
            markDirty();
        }
    }

    public void setH(float h) {
        if (this.h != h) {
            this.h = h;
            markDirty();
        }
    }

    public void setW(float w) {
        if (this.w != w) {
            this.w = w;
            markDirty();
        }
    }

    public void markDirty() {
        if (!this.dirty) {
            this.dirty = true;
            if (parent != null) {
                parent.childIsDirty(this);
            } else {
                NvContext.markSceneDirty();
            }
        }
    }

    protected void childIsDirty(NvComp child) {
        markDirty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void cleanDirty() {
        this.dirty = false;
    }

    private NvContext context;

    /**
     * Set the root component list reference (called by NvCont)
     */
    public void setRootComponentList(List<NvComp> rootList) {
        this.rootComponentList = rootList;
    }

    public void addChild(NvComp child) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }

        if (context == null) {
            context = NvContext.getInstance();
        }

        if (children.contains(child)) {
            return;
        }

        children.add(child);
        child.setParent(this);

        if (rootComponentList != null) {
            addSubtreeToFlatList(child, rootComponentList);
        }

        if (child instanceof Collidable)
            CollisionManager.addCanCollide(child);

        if (child instanceof Clickable)
            ClickSystem.addClickable(child);

        if (child instanceof Hoverable)
            HoverSystem.addHoverable(child);

        markDirty();
    }
    private void addSubtreeToFlatList(
            NvComp component,
            List<NvComp> rootList
    ) {
        component.setRootComponentList(rootList);

        if (!rootList.contains(component)) {
            rootList.add(component);
        }

        for (NvComp child : component.getChildren()) {
            addSubtreeToFlatList(child, rootList);
        }
    }

    public void removeChild(NvComp child){
        if(context == null)
            context = NvContext.getInstance();

        children.remove(child);

        // Remove child and all descendants from flat list
        if(rootComponentList != null) {
            removeFromFlatList(child);
        }

        if(child instanceof Collidable)
            CollisionManager.removeCanCollide(child);
        if(child instanceof Clickable)
            ClickSystem.removeClickable(child);
        if(child instanceof Hoverable)
            HoverSystem.removeHoverable(child);
        markDirty();
    }

    /**
     * Recursively remove component and all descendants from flat list
     */
    private void removeFromFlatList(NvComp comp) {
        rootComponentList.remove(comp);
        for(NvComp child : comp.getChildren()) {
            removeFromFlatList(child);
        }
    }

    protected void mouseEnter(){}

    protected void mouseOut(){}

    public void translate(Vector2D v, float amount){
        this.x += v.x * amount;
        this.y += v.y * amount;
        markDirty();
    }

    public void handleHover(int mouseX, int mouseY){
        boolean hoveredNow = isInside(mouseX, mouseY);
        if(!hoveredNow) {
            if (isHovered) {
                isHovered = false;
                markDirty();
            }
            return;
        }
        this.hoveredX = mouseX;
        this.hoveredY = mouseY;
        for(NvComp child : children)
            child.handleHover(mouseX, mouseY);
        if (!isHovered) {
            isHovered = true;
            markDirty();
        }
    }

    public void tick(float dt){
        update(dt);
    }

    @Override
    public void draw(NvGraphic g){
        int vStart = g.getVertexFloatCount();
        int iStart = g.getImageVertexFloatCount();

        g.setComponent(this);
        if(isHovered){
            mouseEnter();
        }else{
            mouseOut();
        }
        drawIntern(g);
        if(border){
            g.setComponent(this);
            if(this instanceof AppendableGeometry comp){
                g.drawRect(0,0, w, h, 1,0,0, comp);
            }
            g.drawRect(0,0, w, h, 1,0,0);
        }
        g.setComponent(this);
        g.applyTransformsToBatch(vStart, iStart);

        cleanDirty();
    }

    public boolean isInside(int x, int y) {
        float worldX;
        float worldY;

        if (isHUD()) {
            worldX = x;
            worldY = y;
        } else {
            float safeZoom = Math.max(camera.zoom, 0.0001f);
            worldX = camera.x + (x / safeZoom);
            worldY = camera.y + (y / safeZoom);
        }

        //handles rotation
        if (rotation != 0) {
            float pivotWorldX = this.x + this.w * this.pivotX;
            float pivotWorldY = this.y + this.h * this.pivotY;

            float rad = (float) Math.toRadians(-rotation);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float dx = worldX - pivotWorldX;
            float dy = worldY - pivotWorldY;

            worldX = pivotWorldX + (dx * cos - dy * sin);
            worldY = pivotWorldY + (dx * sin + dy * cos);
        }

        return worldX >= this.x &&
                worldX <= this.x + this.w &&
                worldY >= this.y &&
                worldY <= this.y + this.h;
    }

    public abstract void drawIntern(NvGraphic g);

    public void rotate(float angle, boolean clockwise) {
        rotation += clockwise ? angle : -angle;
        markDirty();
    }

    public void destroy(){
        this.shouldGetDestroyed = true;
        markDirty();
    }

    protected void actualDestroy(){
        if(context == null)
            context = NvContext.getInstance();

        List<NvComp> childrenCopy = new ArrayList<>(children);
        for (NvComp child : childrenCopy) {
            child.actualDestroy();
        }
        children.clear();

        if(this instanceof Collidable){
            CollisionManager.removeCanCollide(this);
        }
        if(this instanceof Clickable) {
            ClickSystem.removeClickable(this);
        }
        if(this instanceof Hoverable)
            HoverSystem.removeHoverable(this);

        whenDestroyed();
        markDirty();
    }

    /**
     * Override for custom behavior when destroyed
     */
    protected void whenDestroyed(){}

    @Override
    public String toString(){
        return "NvComp: " + this.getClass().getSimpleName() + " x: " + x + " y: " + y + " w: " + w + " h: " + h + " rotation: " + rotation;
    }

}