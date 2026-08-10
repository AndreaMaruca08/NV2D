package nv.test.game.example;

import nv.utils.shapes.dynamic.DynamicSquare;

public class Wall extends DynamicSquare {
    public Wall(int x, int y, int w, int h) {
        super(x, y, w, h);
        setRgb(1,1,1);
        setWeight(1);
    }
}
