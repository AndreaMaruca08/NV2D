package nv.test.benchmark;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import java.util.Random;

/**
 * Black hole benchmark.
 */
@EngineCore
public class BlackHole extends NvComp {

    private static final int MAX_PARTICLES = 300_000;
    private static final int START_PARTICLES = 500;
    private static final int SPAWN_PER_SECOND = 10_000;

    private static final float GRAVITY = 6_000_000f;
    private static final float EVENT_HORIZON = 50f;
    private static final float SPAWN_RADIUS_MIN = 100f;
    private static final float SPAWN_RADIUS_MAX = 1000f;

    private final float[] px, py, vx, vy;
    private final float[] pr, pg, pb;
    private final float[] size;

    private int activeCount;
    private float spawnAccumulator = 0f;

    private float centerX, centerY;
    private final Random rnd = new Random();

    public BlackHole(int x, int y, int w, int h) {
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

        for (int i = 0; i < START_PARTICLES; i++) {
            spawnParticle(i);
        }
        activeCount = START_PARTICLES;
    }

    private void spawnParticle(int i) {
        float dist = SPAWN_RADIUS_MIN + rnd.nextFloat() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
        float angle = rnd.nextFloat() * (float) (Math.PI * 2);

        px[i] = (float) Math.cos(angle) * dist;
        py[i] = (float) Math.sin(angle) * dist;


        float tangentialFactor = 0.4f + rnd.nextFloat() * 0.9f;
        float speed = (float) Math.sqrt(GRAVITY / dist) * tangentialFactor;

        float dirX = -(float) Math.sin(angle);
        float dirY = (float) Math.cos(angle);

        float radialKick = (rnd.nextFloat() - 0.5f) * speed * 0.3f;

        vx[i] = dirX * speed + (float) Math.cos(angle) * radialKick;
        vy[i] = dirY * speed + (float) Math.sin(angle) * radialKick;

        randomizeColor(i);

        size[i] = 0.8f + rnd.nextFloat() * 2.2f;
    }

    private void randomizeColor(int i) {
        // toni scuri e desaturati, ma sempre diversi tra loro
        int palette = rnd.nextInt(4);
        float variance = rnd.nextFloat();
        float darkness = 0.15f + rnd.nextFloat() * 0.35f; // tiene tutto vicino al nero

        switch (palette) {
            case 0 -> { // rosso/arancio brace spenta
                pr[i] = darkness * (0.8f + variance * 0.2f);
                pg[i] = darkness * (0.2f + variance * 0.15f);
                pb[i] = darkness * 0.05f;
            }
            case 1 -> { // blu/ciano cupo
                pr[i] = darkness * 0.05f;
                pg[i] = darkness * (0.2f + variance * 0.2f);
                pb[i] = darkness * (0.7f + variance * 0.3f);
            }
            case 2 -> { // viola cupo
                pr[i] = darkness * (0.3f + variance * 0.3f);
                pg[i] = darkness * 0.05f;
                pb[i] = darkness * (0.4f + variance * 0.3f);
            }
            default -> { // grigio/bianco spento
                float shade = darkness * (0.5f + variance * 0.3f);
                pr[i] = shade;
                pg[i] = shade;
                pb[i] = shade;
            }
        }
    }

    @Override
    public void update(float dt) {
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

            // particella "consumata" dal buco nero: respawn casuale altrove
            if (dist < EVENT_HORIZON) {
                respawnFar(i);
            }
            // particella scappata troppo lontano (raro, ma per sicurezza)
            else if (dist > SPAWN_RADIUS_MAX * 2f) {
                respawnFar(i);
            }
        }
        markDirty();
    }

    private void respawnFar(int i) {
        float dist = SPAWN_RADIUS_MIN + rnd.nextFloat() * (SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN);
        float angle = rnd.nextFloat() * (float) (Math.PI * 2);

        px[i] = (float) Math.cos(angle) * dist;
        py[i] = (float) Math.sin(angle) * dist;

        float tangentialFactor = 0.4f + rnd.nextFloat() * 0.9f;
        float speed = (float) Math.sqrt(GRAVITY / dist) * tangentialFactor;

        float dirX = -(float) Math.sin(angle);
        float dirY = (float) Math.cos(angle);
        float radialKick = (rnd.nextFloat() - 0.5f) * speed * 0.3f;

        vx[i] = dirX * speed + (float) Math.cos(angle) * radialKick;
        vy[i] = dirY * speed + (float) Math.sin(angle) * radialKick;

        randomizeColor(i);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        g.drawOval(centerX, centerY, EVENT_HORIZON * 6f, 32, 0.5f, 0.15f, 0.05f);
        g.drawOval(centerX, centerY, EVENT_HORIZON * 3.5f, 32, 0.7f, 0.25f, 0.05f);
        g.drawOval(centerX, centerY, EVENT_HORIZON * 1.8f, 32, 0.9f, 0.4f, 0.1f);

        g.beginBatch();
        for (int i = 0; i < activeCount; i++) {
            g.batchDrawOval(centerX + px[i], centerY + py[i], size[i], 6, pr[i], pg[i], pb[i]);
        }
        g.endBatch();

        g.drawOval(centerX, centerY, EVENT_HORIZON, 32, 0f, 0f, 0f);
    }
}