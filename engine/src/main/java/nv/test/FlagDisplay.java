package nv.test;

import nv.core.NvContext;
import nv.core.assets.AtlasConverter;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import java.util.ArrayList;
import java.util.List;

public class FlagDisplay extends NvComp {

    private final AtlasConverter.Atlas flagsAtlas;
    private final List<AtlasConverter.Region> regions = new ArrayList<>();

    public FlagDisplay(int x, int y) {
        super(x, y, 800, 200);

        // Load the atlas from the "test" subfolder
        NvContext app = NvContext.getInstance();
        flagsAtlas = app.assets().loadAtlas("flags", "");

        List<String> flagNames = List.of("AD", "AI", "AR", "VC", "ZM");
        for (String name : flagNames) {
            regions.add(app.assets().getRegion("flags", name));
        }
    }

    @Override
    public void drawIntern(NvGraphic g) {
        int flagW = 80;
        int flagH = 50;
        int spacing = 20;

        for (int i = 0; i < regions.size(); i++) {
            AtlasConverter.Region region = regions.get(i);
            int drawX = i * (flagW*3 + spacing);
            
            g.drawImageRegion(
                    flagsAtlas.image(),
                    drawX, 0, flagW*3, flagH*3,
                    region.u1(), region.v1(),
                    region.u2(), region.v2()
            );
            
            // Draw a small border around each flag
            g.setRGB(1, 1, 1);
            g.drawRectBorder(drawX, 0, flagW*3, flagH*3, 2);
        }
    }

    @Override
    public void update(float dt) {
        // Static display for now
    }
}
