package nv.components;

import nv.components.vectors.Vector2D;
import nv.core.AppendableGeometry;
import nv.core.NvContext;
import nv.core.NvGraphic;
import nv.core.UpdateCycle;
import nv.core.collision.Collidable;
import nv.core.collision.CollisionSystem;

import java.util.ArrayList;
import java.util.List;

import static nv.core.NvGraphic.camera;

/**
 * <h3>Root of the component tree</h3>
 * <p>Base class for all components in the component tree, by updating or drawing a component you draw every child with it</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public abstract class NvComp implements UpdateCycle {
    private NvComp parent;
    private final List<NvComp> children;
    protected int x, y, w, h;
    protected boolean isHovered;
    protected boolean childrenFirst;
    public float rotation = 0;
    protected int weight = CollisionSystem.NO_WEIGHT;
    public boolean border = false;
    protected boolean isHUD = false;

    public NvComp(int x, int y, int w, int h) {
        children = new ArrayList<>();
        this.parent = null;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean isHUD() {
        return isHUD;
    }

    public void setHUD(boolean HUD) {
        if(this instanceof Collidable)
            throw new UnsupportedOperationException("Collidable components cannot be set as HUD");
        isHUD = HUD;
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

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    protected void setParent(NvComp parent){
        this.parent = parent;
    }

    public void setChildrenFirst(boolean childrenFirst) {
        this.childrenFirst = childrenFirst;
    }

    public void setX(int x) {
        this.x = x;
        invalidate();
    }

    public void setY(int y) {
        this.y = y;
        invalidate();
    }

    public void setH(int h) {
        this.h = h;
        invalidate();
    }

    public void setW(int w) {
        this.w = w;
        invalidate();
    }

    public void invalidate() {}

    private NvContext app;
    public void addChild(NvComp child){
        if(app == null)
            app = NvContext.getInstance();
        children.add(child);
        child.setParent(this);
        if(child instanceof Collidable)
            app.addCanCollide(child);
    }

    public void removeChild(NvComp child){
        if(app == null)
            app = NvContext.getInstance();
        children.remove(child);
        if(child instanceof Collidable)
            app.removeCanCollide(child);
    }

    protected void mouseEnter(){}

    protected void mouseOut(){}

    public void translate(Vector2D v, float amount){
        this.x += (int) (v.x * amount);
        this.y += (int) (v.y * amount);
        invalidate();
    }

    public void handleHover(int mouseX, int mouseY){
        if(!isInside(mouseX, mouseY)) {
            isHovered = false;
            return;
        }
        for(NvComp child : children)
            child.handleHover(mouseX, mouseY);
        isHovered = true;
    }


    public void tick(float dt){
        updateChildren(dt);
        update(dt);
    }

    private void updateChildren(float dt){
        for (NvComp child : children) {
            child.tick(dt);
        }
    }

    public void draw(NvGraphic g){
        int vStart = g.getVertexFloatCount();
        int iStart = g.getImageVertexFloatCount();

        g.setComponent(this);
        if(isHovered){
            mouseEnter();
        }else{
            mouseOut();
        }
        if(childrenFirst){
            drawIntern(g);
            drawChildren(g);
        }else{
            drawChildren(g);
            g.setComponent(this);
            drawIntern(g);
        }
        if(border){
            g.setComponent(this);
            if(this instanceof AppendableGeometry comp){
                g.drawRect(0,0, w, h, 1,0,0, comp);
            }
            g.drawRect(0,0, w, h, 1,0,0);
        }
        g.setComponent(this);
        g.applyTransformsToBatch(vStart, iStart);
    }

    public void drawChildren(NvGraphic g){
        for (NvComp child : children) {
            if(camera.isComponentInRendering(child))
                child.draw(g);
        }
    }

    public boolean isInside(int x, int y){
        float shiftedX = camera.x + x;
        float shiftedY = camera.y + y;
        return  shiftedX >= this.x &&
                shiftedX <= this.x + this.w &&
                shiftedY >= this.y &&
                shiftedY <= this.y + this.h;
    }

    public abstract void drawIntern(NvGraphic g);

    protected void rotate(float angle, boolean clockwise) {
        rotation += clockwise ? angle : -angle;
    }

    public void destroy(){
        if(app == null)
            app = NvContext.getInstance();
        for (NvComp child : children) {
            child.destroy();
            if(child instanceof Collidable) {
                app.removeCanCollide(child);
            }
        }
        if(getParent() != null) {
            getParent().children.remove(this);
        }
        if(this instanceof Collidable){
            app.removeCanCollide(this);
        }
    }

    @Override
    public String toString(){
        return "NvComp: " + this.getClass().getSimpleName() + " x: " + x + " y: " + y + " w: " + w + " h: " + h + " rotation: " + rotation;
    }
}
