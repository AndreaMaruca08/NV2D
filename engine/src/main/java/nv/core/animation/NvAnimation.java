package nv.core.animation;

import nv.core.NvContext;
import nv.core.Updatable;
import nv.core.annotations.EngineCore;
import nv.utils.NvTimer;

import java.util.concurrent.atomic.AtomicInteger;

@EngineCore
@SuppressWarnings("unused")
public class NvAnimation implements Updatable {
    private final NvTimer timer;
    private final NvSprite sprite;

    private boolean going = false;
    private boolean resetWhenStopped = false;

    private final AtomicInteger currentFrameIndex = new AtomicInteger(0);

    public NvAnimation (NvSprite sprite, int msBetweenFrames, String... frames){
        timer = new NvTimer(msBetweenFrames);
        this.sprite = sprite;
        timer.setIsLoop(true);
        var ctx = NvContext.getInstance();

        timer.setOnFinished(() -> {
            sprite.changeImage(frames[currentFrameIndex.get()]);
            currentFrameIndex.set((currentFrameIndex.get() + 1) % frames.length);
            NvContext.markSceneDirty();
        });
        ctx.addUpdatable(timer);
        ctx.addUpdatable(this);
    }

    public void setResetWhenStopped(boolean resetWhenStopped) {
        this.resetWhenStopped = resetWhenStopped;
    }

    public void start(){
        going = true;
        currentFrameIndex.set(0);
    }
    public void stop(){
        going = false;
        if(resetWhenStopped) sprite.changeToDefault();
    }
    public void destroy(){
        NvContext.getInstance().removeUpdatable(timer);
        NvContext.getInstance().removeUpdatable(this);
    }

    @Override
    public void update(float delta) {
        if(!going) {
            timer.stop();
            return;
        }
        timer.start();
    }

    @Override
    public boolean isActive() {
        return going;
    }
}
