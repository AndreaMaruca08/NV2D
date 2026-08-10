package nv.test.game.example;

import nv.core.components.Vector2D;

public class Rain implements Runnable{
    private final BattleArena arena;
    private final int margin;
    public Rain(BattleArena arena, int margin) {
        this.arena = arena;
        this.margin = margin;
    }
    @Override
    public void run() {
        int x = (int) (Math.random() * (arena.getW() - margin));
        int y = margin;
        var p = new Projectile(x, y, 10, 10, 10,Vector2D.DOWN);
        p.setRgb(1,1,1);
        arena.addChild(p);
    }
}
