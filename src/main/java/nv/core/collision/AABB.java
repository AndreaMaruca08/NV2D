package nv.core.collision;

import nv.components.NvComp;

public class AABB implements CollisionSystem{

    @Override
    public boolean isColliding(NvComp a, NvComp b) {
        int x1 = a.getX(); int x2 = b.getX();
        int y1 = a.getY(); int y2 = b.getY();
        int w1 = a.getW(); int w2 = b.getW();
        int h1 = a.getH(); int h2 = b.getH();

        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2;
    }

    @Override
    public void resolveCollision(NvComp a, NvComp b) {
        int dx1 = (a.getX() + a.getW()) - b.getX();
        int dx2 = (b.getX() + b.getW()) - a.getX();

        int dy1 = (a.getY() + a.getH()) - b.getY();
        int dy2 = (b.getY() + b.getH()) - a.getY();

        int ox = Math.min(dx1, dx2);
        int oy = Math.min(dy1, dy2);

        int wA = a.getWeight();
        int wB = b.getWeight();

        // Se entrambi sono statici (peso massimo), non risolviamo nulla
        if (wA == Integer.MAX_VALUE && wB == Integer.MAX_VALUE) return;

        float ratioA, ratioB;

        if (wA == Integer.MAX_VALUE) {
            ratioA = 0;
            ratioB = 1;
        } else if (wB == Integer.MAX_VALUE) {
            ratioA = 1;
            ratioB = 0;
        } else {
            float totalWeight = wA + wB;
            if (totalWeight <= 0) {
                ratioA = 0.5f;
                ratioB = 0.5f;
            } else {
                ratioA = (float) wB / totalWeight;
                ratioB = (float) wA / totalWeight;
            }
        }

        if (ox < oy) {
            if (a.getX() < b.getX()) {
                a.setX(a.getX() - Math.round(ox * ratioA));
                b.setX(b.getX() + Math.round(ox * ratioB));
            } else {
                a.setX(a.getX() + Math.round(ox * ratioA));
                b.setX(b.getX() - Math.round(ox * ratioB));
            }
        } else {
            if (a.getY() < b.getY()) {
                a.setY(a.getY() - Math.round(oy * ratioA));
                b.setY(b.getY() + Math.round(oy * ratioB));
            } else {
                a.setY(a.getY() + Math.round(oy * ratioA));
                b.setY(b.getY() - Math.round(oy * ratioB));
            }
        }
    }
}
