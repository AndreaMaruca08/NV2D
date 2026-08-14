package nv.utils;

import nv.core.NvContext;
import nv.core.annotations.ReadyComponent;
import nv.core.components.NvComp;
import nv.core.graphic.NvGraphic;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a deck of and you can .
 * @author Andrea Maruca
 * @since 1.6
 */
@ReadyComponent
@SuppressWarnings("unused")
public class NvDeck extends NvComp{
    private List<NvComp> cards = new ArrayList<>(5);

    private int index = 0;

    public NvDeck(List<NvComp> cards, int x, int y, int w, int h) {
        super(x,y,w,h);
        this.cards = cards;
        for(NvComp c : cards){
            adjust(c);
        }
    }
    public NvDeck(int x, int y, int w, int h){
        super(x,y,w,h);
    }

    private void adjust(NvComp c){
        c.setX(c.getX() + getX());
        c.setY(c.getY() + getY());
    }

    private int checkIndex(int index){
        if(index > cards.size() - 1){
            return 0;
        }
        if(index < 0){
            return cards.size() - 1;
        }
        return index;
    }

    public void next(){
        index = checkIndex(index + 1);
        NvContext.markSceneDirty();
    }
    public void previous(){
        index = checkIndex(index - 1);
        NvContext.markSceneDirty();
    }

    public NvComp getActive(){
        return cards.get(index);
    }

    public int getIndex() {
        return index;
    }

    public void addCard(NvComp card){
        this.cards.add(card);
        adjust(card);
    }

    public void removeCard(NvComp card){
        cards.remove(card);
    }

    @Override
    public void drawIntern(NvGraphic g) {
        cards.get(index).draw(g);
    }

    @Override
    public void update(float dt) {
        cards.get(index).update(dt);
    }
}
