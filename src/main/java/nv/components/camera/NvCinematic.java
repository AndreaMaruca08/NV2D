package nv.components.camera;

import nv.core.Nv2DApp;
import nv.core.NvGraphic;
import nv.core.UpdateCycle;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Represents a cinematic camera that can be used to create smooth transitions between different camera positions and zoom levels.
 */
public abstract class NvCinematic extends NvCamera implements UpdateCycle {
    protected boolean started = false;
    protected int xStart, yStart;
    protected float initialZoom;
    protected float duration, initialDuration;
    protected boolean loop;

    public NvCinematic(int x, int y, float zoom, long msDuration, boolean loop) {
        super(x, y, zoom/10);
        xStart = x;
        yStart = y;
        initialZoom = zoom/10;
        this.loop = loop;
        this.duration = Duration.ofMillis(msDuration).get(ChronoUnit.SECONDS);
        initialDuration = Duration.ofMillis(msDuration).get(ChronoUnit.SECONDS);
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
            app = Nv2DApp.getInstance();
            app.setCurrentCameraUpdateCycle(this);
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
