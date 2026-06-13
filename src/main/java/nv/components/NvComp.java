package nv.components;

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

    public NvComp(NvComp parent, int x, int y, int w, int h) {
        children = new ArrayList<>();
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    public NvComp(int x, int y, int w, int h) {
        this(null, x, y, w, h);
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
        g.setComponent(this);
        drawIntern(g);
        drawChildren(g);
    }

    public void drawChildren(NvGraphic g){
        for (NvComp child : children) {
            child.draw(g);
        }
    }

    public abstract void drawIntern(NvGraphic g);
}
