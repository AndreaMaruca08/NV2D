package nv.test.benchmark;

import nv.core.NvContext;
import nv.core.annotations.Example;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.utils.NvTimer;

import java.util.List;

@Example
public class Benchmark extends NvComp {
    private final List<NvComp> phases;
    private NvComp current;
    private int currentPhaseIndex = 0;
    private int cycles = 0;
    private final int requestedCycles;
    private final NvTimer timer;
    private boolean ended = false;

    private final float textX;

    public Benchmark(int timeForPhase, int requestedCycles) {
        var ctx = NvContext.getInstance();
        ctx.setShowFPS(true);
        var w = (int)(ctx.getRenderWidth());
        var h = (int)(ctx.getRenderHeight());
        textX = (float) w / 2.2f;
        phases = List.of(
                new Phase1(0,0,w,h),
                new Phase2(0,0,w,h)
        );
        super(0,0, w, h);
        this.requestedCycles = requestedCycles;
        this.timer = new NvTimer(timeForPhase);
        timer.setIsLoop(true);
        timer.setOnFinished(() -> {
            currentPhaseIndex++;
            if (currentPhaseIndex >= phases.size()) {
                this.cycles++;
                currentPhaseIndex = 0;
                if(this.cycles > requestedCycles){
                    timer.stop();
                    ended = true;
                }
            }
            current = phases.get(currentPhaseIndex);
        });
        current = phases.get(currentPhaseIndex);
        timer.start();
        ctx.addUpdatable(timer);
    }
    public Benchmark(int timeForPhase){
        this(timeForPhase, 1);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        if(!ended) {
            current.draw(g);
        }
        g.drawText("Benchmark " + (currentPhaseIndex+1) + "/" + requestedCycles, textX, 0);

    }

    @Override
    public void update(float dt) {
        if(!ended){
            current.update(dt);
        }
    }
}
