package nv.core.collision;

import nv.core.annotations.DefaultChose;
import nv.core.components.NvComp;

/**
 * Default collision system for fast but simple collision detection.
 * @since 1.0
 * @author Andrea Maruca
 */
@DefaultChose
public final class AABB implements CollisionSystem{

    @Override
    public boolean isColliding(NvComp a, NvComp b) {
        float x1 = a.getX(); float x2 = b.getX();
        float y1 = a.getY(); float y2 = b.getY();
        float w1 = a.getW(); float w2 = b.getW();
        float h1 = a.getH(); float h2 = b.getH();

        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2;
    }

    @Override
    public void resolveCollision(NvComp a, NvComp b) {
        float dx1 = (a.getX() + a.getW()) - b.getX();
        float dx2 = (b.getX() + b.getW()) - a.getX();
        float dy1 = (a.getY() + a.getH()) - b.getY();
        float dy2 = (b.getY() + b.getH()) - a.getY();

        float ox = Math.min(dx1, dx2);
        float oy = Math.min(dy1, dy2);

        float wA = a.getWeight();
        float wB = b.getWeight();

        if (wA == Integer.MAX_VALUE && wB == Integer.MAX_VALUE) return;

        float ratioA, ratioB;
        if (wA == Integer.MAX_VALUE) {
            ratioA = 0; ratioB = 1;
        } else if (wB == Integer.MAX_VALUE) {
            ratioA = 1; ratioB = 0;
        } else {
            float totalWeight = wA + wB;
            ratioA = totalWeight <= 0 ? 0.5f : wB / totalWeight;
            ratioB = totalWeight <= 0 ? 0.5f : wA / totalWeight;
        }

        if (ox < oy) {
            float correctionA = ox * ratioA;
            float correctionB = ox * ratioB;
            if (dx1 < dx2) {
                a.setX(a.getX() - correctionA);
                b.setX(b.getX() + correctionB);
            } else {
                a.setX(a.getX() + correctionA);
                b.setX(b.getX() - correctionB);
            }
        } else {
            float correctionA = oy * ratioA;
            float correctionB = oy * ratioB;
            if (dy1 < dy2) {
                a.setY(a.getY() - correctionA);
                b.setY(b.getY() + correctionB);
            } else {
                a.setY(a.getY() + correctionA);
                b.setY(b.getY() - correctionB);
            }
        }
    }
}
