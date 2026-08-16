package nv.test.benchmark;

import nv.core.graphic.NvGraphic;
import nv.utils.NvCharacter;
import org.lwjgl.glfw.GLFW;

public class MovingCamera extends NvCharacter {
    public MovingCamera(int x, int y) {
        super(x, y, 10,10, 1500);
        camera.setXYOnCenter(x, y);
        velocity = 400;
    }

    @Override
    public void drawIntern(NvGraphic g){}

    @Override
    public void update(float dt){
        super.update(dt);
        if(keys[GLFW.GLFW_KEY_Z]){
            camera.zoomOnCenter(0.05f, 1f, Float.POSITIVE_INFINITY);
            markDirty();
        }else if(keys[GLFW.GLFW_KEY_X]){
            camera.zoomOnCenter(-0.05f, 1f, Float.POSITIVE_INFINITY);
            markDirty();
        }
    }
}
