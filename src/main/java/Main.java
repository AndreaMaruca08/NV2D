import nv.core.ContextBuilder;
import nv.core.components.NvCont;
import nv.test.game.example.CustomCharacter;

import nv.test.game.example.Portal;

void main() {
    var context = new ContextBuilder("TEST")
            .setVsync(true)
            .showFps()
            .build();

    var page = context.addAndSetPage("NewPage", NvCont.newPage());
    page.setBackground(0,0,0);

    CustomCharacter character = new CustomCharacter(1000,500, 100, 100, 800);
    context.setKeyboardFocus(character);

    page.addChild(new Portal(page.getX(), page.getY(), 1000, 100));

    page.addChild(character);

    context.run();
}
