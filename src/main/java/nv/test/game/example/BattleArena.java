package nv.test.game.example;

import nv.core.NvContext;
import nv.core.components.NvCont;
import nv.core.errors.NvLogger;
import nv.core.io.AudioManager;
import nv.utils.NvTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

        AtomicInteger round = new AtomicInteger(1);
        NvTimer timer = new NvTimer(100);
        NvTimer pausa = new NvTimer(5000);

        AudioManager.playLoop("dialtone.mp3");

        timer.setOnFinished(() -> {
            int i = (int) ((Math.random() * 100) % events.size());
            var e = events.get(i);
            e.run();

            pausa.reset();
            pausa.start();
        });

        pausa.setOnFinished(() -> {
            int currentRound = round.incrementAndGet();
            if (currentRound > 5) {
                NvLogger.logInfo("rounds finished");
            } else {
                timer.reset();
                timer.start();
            }
        });

        context.addUpdatable(timer);
        context.addUpdatable(pausa);

        timer.start();

        addChild(wall1);
        addChild(wall2);
        addChild(wall3);
        addChild(wall4);

    }

}
