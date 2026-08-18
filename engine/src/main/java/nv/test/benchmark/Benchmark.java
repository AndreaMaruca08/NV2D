package nv.test.benchmark;

import com.sun.management.OperatingSystemMXBean;
import nv.core.NvContext;
import nv.core.annotations.Example;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;
import nv.utils.NvTimer;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static nv.core.errors.NvLogger.logInfo;

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

    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final Runtime runtime = Runtime.getRuntime();

    private final List<PhaseStats> statsHistory = new ArrayList<>();
    private PhaseStats currentStats;

    private long lastFrameNanos = System.nanoTime();
    private float fpsSmoothed = 0f;

    private static final class PhaseStats {
        String phaseName;
        int cycleNumber;

        double cpuSum = 0;
        double cpuMin = Double.MAX_VALUE;
        double cpuMax = 0;
        int cpuSamples = 0;

        long memSum = 0;
        long memMin = Long.MAX_VALUE;
        long memMax = 0;
        int memSamples = 0;

        double fpsSum = 0;
        double fpsMin = Double.MAX_VALUE;
        double fpsMax = 0;
        int frameCount = 0;

        void sample(double cpuPercent, long memUsedBytes, double fps) {
            if (cpuPercent >= 0) {
                cpuSum += cpuPercent;
                cpuMin = Math.min(cpuMin, cpuPercent);
                cpuMax = Math.max(cpuMax, cpuPercent);
                cpuSamples++;
            }

            memSum += memUsedBytes;
            memMin = Math.min(memMin, memUsedBytes);
            memMax = Math.max(memMax, memUsedBytes);
            memSamples++;

            if (fps > 0) {
                fpsSum += fps;
                fpsMin = Math.min(fpsMin, fps);
                fpsMax = Math.max(fpsMax, fps);
            }
            frameCount++;
        }

        double avgCpu() { return cpuSamples == 0 ? 0 : cpuSum / cpuSamples; }
        long avgMemMB() { return memSamples == 0 ? 0 : (memSum / memSamples) / (1024 * 1024); }
        double avgFps() { return frameCount == 0 ? 0 : fpsSum / frameCount; }

        String report() {
            return String.format(
                    "[%s | ciclo %d] frames=%d | CPU avg=%.1f%% min=%.1f%% max=%.1f%% | " +
                            "MEM avg=%dMB min=%dMB max=%dMB | FPS avg=%.1f min=%.1f max=%.1f",
                    phaseName, cycleNumber, frameCount,
                    avgCpu(), cpuMin == Double.MAX_VALUE ? 0 : cpuMin, cpuMax,
                    avgMemMB(), memMin == Long.MAX_VALUE ? 0 : memMin / (1024 * 1024), memMax / (1024 * 1024),
                    avgFps(), fpsMin == Double.MAX_VALUE ? 0 : fpsMin, fpsMax
            );
        }
    }

    public Benchmark(int timeForPhase, int requestedCycles) {
        var ctx = NvContext.getInstance();
        var w = (int)(ctx.getRenderWidth());
        var h = (int)(ctx.getRenderHeight());
        textX = (float) w / 2.2f;
        phases = List.of(
                new SaturnPhase(0,0,w,h),
                new BlackHole(0,0,w,h),
                new GalaxyPhase(0,0,w,h)
        );
        super(0,0, w, h);
        this.requestedCycles = requestedCycles;
        this.timer = new NvTimer(timeForPhase);
        timer.setIsLoop(true);
        timer.setOnFinished(() -> {
            finalizeCurrentStats();

            currentPhaseIndex++;
            if (currentPhaseIndex >= phases.size()) {
                this.cycles++;
                currentPhaseIndex = 0;
                if(this.cycles > requestedCycles){
                    timer.stop();
                    ended = true;
                    printFullReport();
                }
            }
            if (!ended) {
                current = phases.get(currentPhaseIndex);
                startPhaseStats();
            }
        });
        current = phases.get(currentPhaseIndex);
        timer.start();
        ctx.addUpdatable(timer);

        startPhaseStats();
    }
    public Benchmark(int timeForPhase){
        this(timeForPhase, 1);
    }

    private void startPhaseStats() {
        currentStats = new PhaseStats();
        currentStats.phaseName = current.getClass().getSimpleName();
        currentStats.cycleNumber = cycles;
    }

    private void finalizeCurrentStats() {
        if (currentStats != null) {
            statsHistory.add(currentStats);
            System.out.println(currentStats.report());
        }
    }

    private void printFullReport() {
        System.out.println("========== BENCHMARK REPORT ==========");
        for (PhaseStats s : statsHistory) {
            logInfo(s.report());
        }
        System.out.println("=======================================");
    }

    private void sampleFrame() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
        lastFrameNanos = now;

        double instantFps = deltaSeconds > 0 ? 1.0 / deltaSeconds : 0;
        fpsSmoothed = fpsSmoothed == 0 ? (float) instantFps : fpsSmoothed * 0.9f + (float) instantFps * 0.1f;

        double cpuLoad = osBean.getProcessCpuLoad();
        double cpuPercent = cpuLoad >= 0 ? cpuLoad * 100.0 : -1;

        long usedMem = runtime.totalMemory() - runtime.freeMemory();

        if (currentStats != null) {
            currentStats.sample(cpuPercent, usedMem, fpsSmoothed);
        }
    }

    @Override
    public void drawIntern(NvGraphic g) {
        if(!ended) {
            current.draw(g);
        }
        g.setRGB(0,0,0);
        g.drawText("Benchmark " + (currentPhaseIndex+1) + "/" + requestedCycles, textX, 0);

        if (!ended) {
            double cpu = osBean.getProcessCpuLoad() * 100.0;
            long memMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            g.drawText(String.format("FPS: %.0f | CPU: %.1f%% | MEM: %dMB",
                    fpsSmoothed, cpu, memMB), textX, 20);
        }
    }

    @Override
    public void update(float dt) {
        if(!ended){
            current.update(dt);
            sampleFrame();
        }
    }
}