package nv.components;

import nv.components.vectors.Vector2D;
import nv.core.UpdateCycle;

import java.util.ArrayList;
import java.util.List;

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
    protected float rotation = 0;

    public NvComp(int x, int y, int w, int h) {
        children = new ArrayList<>();
        this.parent = null;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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

    public int getH() {
        return h;
    }

    public int getW() {
        return w;
    }

    protected void setParent(NvComp parent){
        this.parent = parent;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void removeChild(NvComp child){
        children.remove(child);
    }

    protected void mouseEnter(){}

    protected void mouseOut(){}

    public void translate(Vector2D v, float amount){
        this.x += (int) (v.x * amount);
        this.y += (int) (v.y * amount);
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

    public void addChild(NvComp child){
        children.add(child);
        child.setParent(this);
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
            drawIntern(g);
        }

        g.setComponent(this);
        g.applyTransformsToBatch(vStart, iStart);
    }

    public void drawChildren(NvGraphic g){
        for (NvComp child : children) {
            child.draw(g);
        }
    }

    public boolean isInside(int x, int y){
        return  x >= this.x &&
                x <= this.x + this.w &&
                y >= this.y &&
                y <= this.y + this.h;
    }

    public abstract void drawIntern(NvGraphic g);

    protected void rotate(float angle, boolean clockwise) {
        rotation += clockwise ? angle : -angle;
    }

}
