package nv.test.benchmark;

import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import java.util.Random;

public class GalaxyPhase extends NvComp {

    private static final int MAX_PARTICLES = 300_000;
    private static final int START_PARTICLES = 1_500;
    private static final int SPAWN_PER_SECOND = 10_000;

    private static final float GALAXY_RADIUS = 1100f;
    private static final float CORE_RADIUS = 40f;
    private static final float ROTATION_STRENGTH = 55f;

    private final float[] px, py;
    private final float[] orbitRadius, orbitAngle, angularSpeed;

    private final float[] life;
    private final float[] maxLife;
    private final float[] twinklePhase;
    private final float[] twinkleSpeed;
    private final float[] baseR, baseG, baseB;
    private final float[] size;

    private int activeCount;
    private float spawnAccumulator = 0f;
    private float galaxyTime = 0f;

    private float centerX, centerY;
    private final Random rnd = new Random();

    public GalaxyPhase(int x, int y, int w, int h) {
        super(x, y, w, h);

        centerX = w / 2f;
        centerY = h / 2f;

        px = new float[MAX_PARTICLES];
        py = new float[MAX_PARTICLES];
        orbitRadius = new float[MAX_PARTICLES];
        orbitAngle = new float[MAX_PARTICLES];
        angularSpeed = new float[MAX_PARTICLES];

        life = new float[MAX_PARTICLES];
        maxLife = new float[MAX_PARTICLES];
        twinklePhase = new float[MAX_PARTICLES];
        twinkleSpeed = new float[MAX_PARTICLES];
        baseR = new float[MAX_PARTICLES];
        baseG = new float[MAX_PARTICLES];
        baseB = new float[MAX_PARTICLES];
        size = new float[MAX_PARTICLES];

        for (int i = 0; i < START_PARTICLES; i++) {
            spawnStar(i, true);
        }
        activeCount = START_PARTICLES;
    }

    private void spawnStar(int i, boolean initialBurst) {
        // distanza dal centro: distribuzione pesata verso il centro (più densità vicino al nucleo,
        // come in una galassia reale) usando radice quadrata su un random uniforme
        float t = rnd.nextFloat();
        float dist = CORE_RADIUS + (float) Math.sqrt(t) * (GALAXY_RADIUS - CORE_RADIUS);

        float angle = rnd.nextFloat() * (float) (Math.PI * 2);

        orbitRadius[i] = dist;
        orbitAngle[i] = angle;

        // rotazione differenziale: più vicino al centro = velocità angolare maggiore (come una galassia reale,
        // approssimando una curva di rotazione "quasi piatta" con un termine 1/sqrt(r))
        angularSpeed[i] = ROTATION_STRENGTH / (float) Math.sqrt(dist);
        // segno casuale raro per qualche stella "controrotante" (effetto realistico di galassie interagenti)
        if (rnd.nextFloat() < 0.03f) angularSpeed[i] *= -1;

        px[i] = (float) Math.cos(angle) * dist;
        py[i] = (float) Math.sin(angle) * dist * 0.55f; // leggero appiattimento a disco visto di scorcio

        // ciclo di vita: ogni stella vive un tempo casuale, poi "muore" in un flash e rinasce
        maxLife[i] = 4f + rnd.nextFloat() * 10f;
        life[i] = initialBurst ? rnd.nextFloat() * maxLife[i] : maxLife[i]; // sfalsa le nascite iniziali

        twinklePhase[i] = rnd.nextFloat() * (float) (Math.PI * 2);
        twinkleSpeed[i] = 2f + rnd.nextFloat() * 6f;

        assignStarColor(i, dist);

        size[i] = 0.7f + rnd.nextFloat() * 1.6f;
    }

    private void assignStarColor(int i, float dist) {
        // stelle vicine al nucleo: bianche/blu (giovani, calde)
        // stelle intermedie: gialle/arancio
        // stelle esterne: rosse/rosa (fredde, vecchie) con qualche rara stella blu brillante
        float rarity = rnd.nextFloat();
        float coreFactor = 1f - Math.min(dist / GALAXY_RADIUS, 1f); // 1 = vicino al centro, 0 = bordo

        if (rarity < 0.04f) {
            // stella blu rara e brillante ovunque
            baseR[i] = 0.6f; baseG[i] = 0.75f; baseB[i] = 1.0f;
        } else if (coreFactor > 0.7f) {
            baseR[i] = 0.85f + rnd.nextFloat() * 0.15f;
            baseG[i] = 0.9f + rnd.nextFloat() * 0.1f;
            baseB[i] = 1.0f;
        } else if (coreFactor > 0.35f) {
            baseR[i] = 1.0f;
            baseG[i] = 0.8f + rnd.nextFloat() * 0.2f;
            baseB[i] = 0.5f + rnd.nextFloat() * 0.3f;
        } else {
            baseR[i] = 1.0f;
            baseG[i] = 0.4f + rnd.nextFloat() * 0.3f;
            baseB[i] = 0.45f + rnd.nextFloat() * 0.25f;
        }
    }

    @Override
    public void update(float dt) {
        galaxyTime += dt;

        if (activeCount < MAX_PARTICLES) {
            spawnAccumulator += SPAWN_PER_SECOND * dt;
            int toSpawn = (int) spawnAccumulator;
            if (toSpawn > 0) {
                spawnAccumulator -= toSpawn;
                int newCount = Math.min(activeCount + toSpawn, MAX_PARTICLES);
                for (int i = activeCount; i < newCount; i++) {
                    spawnStar(i, false);
                }
                activeCount = newCount;
            }
        }

        for (int i = 0; i < activeCount; i++) {
            // avanza l'orbita (rotazione pura, niente integrazione di forze: più stabile su scale enormi)
            orbitAngle[i] += angularSpeed[i] * dt * 0.05f;

            px[i] = (float) Math.cos(orbitAngle[i]) * orbitRadius[i];
            py[i] = (float) Math.sin(orbitAngle[i]) * orbitRadius[i] * 0.55f;

            // ciclo di vita
            life[i] -= dt;
            twinklePhase[i] += twinkleSpeed[i] * dt;

            if (life[i] <= 0f) {
                // "supernova" della singola stella: rinasce altrove con nuovo colore/orbita
                spawnStar(i, false);
            }
        }
        markDirty();
    }

    @Override
    public void drawIntern(NvGraphic g) {
        // nucleo galattico centrale: bagliore intenso multi-livello
        g.drawOval(centerX, centerY, CORE_RADIUS * 4f, 32, 0.35f, 0.3f, 0.55f);
        g.drawOval(centerX, centerY, CORE_RADIUS * 2.2f, 32, 0.6f, 0.55f, 0.85f);
        g.drawOval(centerX, centerY, CORE_RADIUS, 32, 1.0f, 0.95f, 0.9f);

        for (int i = 0; i < activeCount; i++) {
            float drawX = centerX + px[i];
            float drawY = centerY + py[i];

            // scintillio: modula luminosità con una sinusoide, e fade in/out all'inizio/fine vita
            float twinkle = 0.6f + 0.4f * (float) Math.sin(twinklePhase[i]);

            float lifeRatio = Math.min(life[i] / maxLife[i], 1f);
            float fade;
            if (lifeRatio > 0.85f) {
                // fade-in nascita
                fade = (1f - lifeRatio) / 0.15f;
            } else if (lifeRatio < 0.15f) {
                // fade-out + flash finale prima di morire (supernova)
                fade = 1f + (0.15f - lifeRatio) * 6f; // brillamento improvviso finale
            } else {
                fade = 1f;
            }

            float brightness = twinkle * fade;

            float dr = Math.min(baseR[i] * brightness, 1f);
            float dg = Math.min(baseG[i] * brightness, 1f);
            float db = Math.min(baseB[i] * brightness, 1f);

            float drawSize = size[i] * (lifeRatio < 0.15f ? fade : 1f);

            g.drawOval(drawX, drawY, drawSize, 6, dr, dg, db);
        }
    }
}