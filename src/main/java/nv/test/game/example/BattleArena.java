package nv.test.game.example;

import nv.core.NvContext;
import nv.core.components.NvCont;
import nv.utils.NvTimer;

import java.util.ArrayList;
import java.util.List;

public class BattleArena extends NvCont {
    private final CustomCharacter character;
    private final NvContext context;
    private final List<Runnable> events;

    public BattleArena(CustomCharacter character) {
        super(0,0,3000,3000);
        this.character = character;
        this.context = NvContext.getInstance();
        this.events = new ArrayList<>(10);
        setBackground(0,0,0);
        addChild(character);

        character.setX(context.getWidth()/2);
        character.setY(context.getHeight()/2);

        int margin = 500;
        int screenWidth = context.getWidth();
        int screenHeight = context.getHeight();
        int thickness = 20;

        events.add(new Rain(this, margin));

        var wall1 = new Wall(margin, margin, screenWidth - 2 * margin, thickness);
        var wall2 = new Wall(screenWidth - margin - thickness, margin, thickness, screenHeight - 2 * margin);
        var wall3 = new Wall(margin, screenHeight - margin - thickness, screenWidth - 2 * margin, thickness);
        var wall4 = new Wall(margin, margin, thickness, screenHeight - 2 * margin);

        NvTimer timer = new NvTimer(500, events.get(0));
        timer.setIsLoop(true);
        context.addUpdatable(timer);
        timer.start();

        addChild(wall1);
        addChild(wall2);
        addChild(wall3);
        addChild(wall4);

    }

}
