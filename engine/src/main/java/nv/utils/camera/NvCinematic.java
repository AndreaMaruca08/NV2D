package nv.utils.camera;

import nv.core.NvContext;
import nv.core.UpdateCycle;
import nv.core.annotations.ReadyComponent;
import nv.core.camera.NvCamera;
import nv.core.graphic.NvGraphic;

/**
 * Represents a cinematic camera that can be used to create smooth transitions between different camera positions and zoom levels.
 * @since 1.0
 * @author Andrea Maruca
 */
@ReadyComponent
@SuppressWarnings("unused")
public abstract class NvCinematic extends NvCamera implements UpdateCycle {
    protected boolean started = false;
    protected int xStart, yStart;
    protected float initialZoom;
    protected float duration, initialDuration;
    protected boolean loop;

    public NvCinematic(int x, int y, float zoom, long msDuration, boolean loop) {
        super(x, y, zoom);
        xStart = x;
        yStart = y;
        initialZoom = zoom;
        this.loop = loop;
        this.duration = (float) (msDuration/1000);
        initialDuration = (float) (msDuration/1000);
    }
    public NvCinematic(int x, int y, float zoom, long msDuration) {
        this(x, y, zoom, msDuration, false);
    }
    public NvCinematic(int x, int y, float zoom) {
        this(x, y, zoom, Integer.MAX_VALUE, false);
    }

    public void start(){
        if(!started){
            NvGraphic.setCurrentCamera(this);
            started = true;
            context = NvContext.getInstance();
            context.setCurrentCameraUpdateCycle(this);
        }
    }

    public abstract void updateCamera(float dt);

    @Override
    public void update(float dt){
        if(!started) return;

        updateCamera(dt);

        duration -= dt;
        if(duration <= 0){
            stop();
            if(loop) {
                reset();
                start();
            }
        }
    }

    public void stop(){
        started = false;
    }
    public void reset() {
        started = false;
        setXY(xStart, yStart);
        zoom = initialZoom;
        duration = initialDuration;
    }
}
