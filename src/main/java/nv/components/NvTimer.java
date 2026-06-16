package nv.components;

import nv.core.NvContext;
import nv.core.UpdateCycle;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * A timer that can be used to track time or execute actions after a certain amount of time.
 * @since 1.0
 * @author Andrea Maruca
 */
public class NvTimer implements UpdateCycle {
    private float remaining;
    private float fullRemaining;
    private boolean finished;
    private boolean started;
    private boolean loop;
    private Runnable onFinished;

    public NvTimer(long milliseconds) {
        remaining =  (float)(milliseconds/1000);
        fullRemaining =  (float)(milliseconds/1000);
        NvContext.getInstance();
    }

    public NvTimer(long milliseconds, Runnable onFinished) {
        this(milliseconds);
        this.onFinished = onFinished;
    }

    public boolean isInLoop() {
        return loop;
    }

    public void setIsLoop(boolean loop){
        this.loop = loop;
    }


    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void setDuration(long milliseconds){
        remaining =  (float)(milliseconds/1000);
        fullRemaining =  (float)(milliseconds/1000);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isStarted() {
        return started;
    }

    public float getRemaining() {
        return remaining;
    }

    public void start(){
        started = true;
    }
    public void stop(){
        started = false;
    }
    public void reset(){
        remaining = fullRemaining;
        finished = false;
        started = false;
    }
    @Override
    public void update(float dt) {
        if(finished || !started)
            return;
        remaining -= dt;
        if(remaining <= 0){
            finished = true;
            started = false;
            if(onFinished != null)
                onFinished.run();
            if(loop){
                reset();
                start();
            }
        }
    }

    @Override
    public String toString(){
        return "NvTimer{" +
                "remaining=" + remaining +
                ", fullRemaining=" + fullRemaining +
                ", finished=" + finished +
                ", started=" + started +
                '}';
    }

}
