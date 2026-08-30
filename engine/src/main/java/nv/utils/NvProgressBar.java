package nv.utils;

import nv.core.annotations.ReadyComponent;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

/**
 * Progress bar component that supports both vertical and horizontal orientation.
 * <p>Default orientation is vertical</p>
 *
 * @author Andrea Maruca
 * @since 1.7.0
 */
@ReadyComponent
@SuppressWarnings("unused")
public class NvProgressBar extends NvComp {

    private float value;
    private float maxValue;
    private float minValue;
    private boolean horizontal = false;

    private float backgroundR = 0.10f;
    private float backgroundG = 0.10f;
    private float backgroundB = 0.12f;

    private float fillR = 0.15f;
    private float fillG = 0.85f;
    private float fillB = 0.35f;

    private float borderR = 0.35f;
    private float borderG = 0.35f;
    private float borderB = 0.40f;

    private float borderThickness = 2.0f;
    private float cornerRadius = 6.0f;

    public NvProgressBar(
            int x,
            int y,
            int w,
            int h,
            float maxValue,
            float minValue,
            float value,
            boolean horizontal
    ) {
        super(x, y, w, h);

        this.maxValue = maxValue;
        this.minValue = minValue;
        this.value = value;
        this.horizontal = horizontal;
    }

    public NvProgressBar(
            int x,
            int y,
            int w,
            int h,
            float maxValue,
            float minValue,
            float value
    ) {
        this(x, y, w, h, maxValue, minValue, value, false);
    }

    public NvProgressBar(
            int x,
            int y,
            int w,
            int h,
            float maxValue,
            float minValue,
            boolean horizontal
    ) {
        this(x, y, w, h, maxValue, minValue, minValue, horizontal);
    }

    public NvProgressBar(
            int x,
            int y,
            int w,
            int h,
            float maxValue,
            float minValue
    ) {
        this(x, y, w, h, maxValue, minValue, minValue, false);
    }

    public void setValue(float value) {
        this.value = value;
        markDirty();
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        markDirty();
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        markDirty();
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getValue() {
        return value;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
        markDirty();
    }

    public boolean isVertical() {
        return !horizontal;
    }

    public void setVertical(boolean vertical) {
        this.horizontal = !vertical;
        markDirty();
    }

    public void setFillColor(float r, float g, float b) {
        this.fillR = r;
        this.fillG = g;
        this.fillB = b;
        markDirty();
    }

    public void setBackgroundColor(float r, float g, float b) {
        this.backgroundR = r;
        this.backgroundG = g;
        this.backgroundB = b;
        markDirty();
    }

    public void setBorderColor(float r, float g, float b) {
        this.borderR = r;
        this.borderG = g;
        this.borderB = b;
        markDirty();
    }

    public void setBorderThickness(float thickness) {
        this.borderThickness = Math.max(0.0f, thickness);
        markDirty();
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = Math.max(0.0f, radius);
        markDirty();
    }

    public float getProgress() {
        float range = maxValue - minValue;

        if (range <= 0.0f) {
            return 0.0f;
        }

        return Math.clamp(
                (value - minValue) / range,
                0.0f,
                1.0f
        );
    }

    @Override
    public void drawIntern(NvGraphic g) {

        float x = 0;
        float y = 0;

        float w = getW();
        float h = getH();

        g.drawRoundRect(
                x,
                y,
                w,
                h,
                cornerRadius,
                backgroundR,
                backgroundG,
                backgroundB
        );

        float innerX = borderThickness;
        float innerY = borderThickness;
        float innerW = w - borderThickness * 2.0f;
        float innerH = h - borderThickness * 2.0f;

        if (innerW > 0.0f && innerH > 0.0f) {

            float progress = getProgress();

            if (progress > 0.0f) {

                float fillX;
                float fillY;
                float fillWidth;
                float fillHeight;

                if (horizontal) {
                    fillWidth = innerW * progress;
                    fillHeight = innerH;
                    fillX = innerX;
                    fillY = innerY;
                } else {
                    fillWidth = innerW;
                    fillHeight = innerH * progress;
                    fillX = innerX;
                    fillY = innerY + innerH - fillHeight;
                }

                float radius = Math.min(
                        cornerRadius,
                        Math.min(fillWidth, fillHeight) * 0.5f
                );

                g.drawRoundRect(
                        fillX,
                        fillY,
                        fillWidth,
                        fillHeight,
                        radius,
                        fillR,
                        fillG,
                        fillB
                );
            }
        }

        if (borderThickness > 0.0f) {
            g.drawRectBorder(
                    0,
                    0,
                    w,
                    h,
                    borderThickness,
                    borderR,
                    borderG,
                    borderB
            );
        }
    }

    @Override
    public void update(float dt) {}
}