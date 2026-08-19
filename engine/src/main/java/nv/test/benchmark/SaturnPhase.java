package nv.test.benchmark;

import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import java.util.Random;

public class SaturnPhase extends NvComp {

    private static final int MAX_PARTICLES = 100_000;
    private static final int START_PARTICLES = 2_000;
    private static final int SPAWN_PER_SECOND = 10_000;

    private static final float GRAVITY = 80000f;
    private static final float PLANET_RADIUS = 300f;
    private static final float MIN_ORBIT = 600f;
    private static final float MAX_ORBIT = 900f;

    private final float[] px, py, vx, vy;
    private final float[] pr, pg, pb;
    private final float[] size;

    private int activeCount;
    private float spawnAccumulator = 0f;

    private float centerX, centerY;
    private final Random rnd = new Random();

    public SaturnPhase(int x, int y, int w, int h) {
        super(x, y, w, h);

        centerX = w / 2f;
        centerY = h / 2f;

        px = new float[MAX_PARTICLES];
        py = new float[MAX_PARTICLES];
        vx = new float[MAX_PARTICLES];
        vy = new float[MAX_PARTICLES];
        pr = new float[MAX_PARTICLES];
        pg = new float[MAX_PARTICLES];
        pb = new float[MAX_PARTICLES];
        size = new float[MAX_PARTICLES];

        activeCount = 0;
        for (int i = 0; i < START_PARTICLES; i++) {
            spawnParticle(i);
        }
        activeCount = START_PARTICLES;
    }

    private void spawnParticle(int i) {
        float dist = MIN_ORBIT + rnd.nextFloat() * (MAX_ORBIT - MIN_ORBIT);
        float angle = rnd.nextFloat() * (float) (Math.PI * 2);
        float flatten = 0.35f;

        px[i] = (float) Math.cos(angle) * dist;
        py[i] = (float) Math.sin(angle) * dist * flatten;

        float speed = (float) Math.sqrt(GRAVITY / dist);
        vx[i] = -(float) Math.sin(angle) * speed;
        vy[i] = (float) Math.cos(angle) * flatten * speed;

        float shade = 0.6f + rnd.nextFloat() * 0.4f;
        pr[i] = shade;
        pg[i] = shade * 0.85f;
        pb[i] = shade * 0.55f;

        size[i] = 0.6f + rnd.nextFloat() * 1.3f;
    }

    @Override
    public void update(float dt) {
        // crescita progressiva del numero di particelle attive
        if (activeCount < MAX_PARTICLES) {
            spawnAccumulator += SPAWN_PER_SECOND * dt;
            int toSpawn = (int) spawnAccumulator;
            if (toSpawn > 0) {
                spawnAccumulator -= toSpawn;
                int newCount = Math.min(activeCount + toSpawn, MAX_PARTICLES);
                for (int i = activeCount; i < newCount; i++) {
                    spawnParticle(i);
                }
                activeCount = newCount;
            }
        }

        for (int i = 0; i < activeCount; i++) {
            float dx = -px[i];
            float dy = -py[i];
            float distSq = dx * dx + dy * dy;
            float dist = (float) Math.sqrt(distSq) + 0.001f;

            float forceMag = GRAVITY / distSq;
            float ax = (dx / dist) * forceMag;
            float ay = (dy / dist) * forceMag;

            vx[i] += ax * dt;
            vy[i] += ay * dt;

            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;

            if (dist < PLANET_RADIUS * 0.7f || dist > MAX_ORBIT * 1.8f) {
                respawnParticle(i);
            }
        }
        markDirty();
    }

    private void respawnParticle(int i) {
        float dist = MIN_ORBIT + rnd.nextFloat() * (MAX_ORBIT - MIN_ORBIT);
        float angle = rnd.nextFloat() * (float) (Math.PI * 2);
        float flatten = 0.35f;

        px[i] = (float) Math.cos(angle) * dist;
        py[i] = (float) Math.sin(angle) * dist * flatten;

        float speed = (float) Math.sqrt(GRAVITY / dist);
        vx[i] = -(float) Math.sin(angle) * speed;
        vy[i] = (float) Math.cos(angle) * flatten * speed;
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(centerX, centerY, PLANET_RADIUS, 32, 0.85f, 0.7f, 0.4f);
        g.drawOval(centerX, centerY, PLANET_RADIUS * 1.15f, 32, 0.9f, 0.8f, 0.55f);

        for (int i = 0; i < activeCount; i++) {
            float drawX = centerX + px[i];
            float drawY = centerY + py[i];
            g.drawOval(drawX, drawY, size[i], 6, pr[i], pg[i], pb[i]);
        }
    }
}