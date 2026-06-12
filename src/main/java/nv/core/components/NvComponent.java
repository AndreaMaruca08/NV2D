package nv.core.components;

import nv.core.drawing.NvGraphic;

import java.util.ArrayList;
import java.util.List;

public abstract class NvComponent {
    private NvComponent parent;
    private final List<NvComponent> children;
    private int x;
    private int y;

    public NvComponent(NvComponent parent, int x, int y) {
        children = new ArrayList<>();
        this.x = x;
        this.y = y;
    }
    public NvComponent(int x, int y) {
        this(null, x, y);
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void removeChild(NvComponent child){
        children.remove(child);
    }

    public NvComponent getParent(){
        return parent;
    }

    public void addChild(NvComponent child){
        children.add(child);
    }

    public void draw(NvGraphic g){
        g.setComponent(this);
        drawChildren(g);
        drawIntern(g);
    }

    public void drawChildren(NvGraphic g){
        for(NvComponent child : children){
            child.draw(g);
        }
    }
    public abstract void drawIntern(NvGraphic g);
}
