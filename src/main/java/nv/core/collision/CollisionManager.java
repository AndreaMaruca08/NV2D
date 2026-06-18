package nv.core.collision;

import nv.components.NvComp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class CollisionManager {
    private CollisionManager(){}

    public static void initialize(){
        collisionSystem = new AABB();
    }

    private static final Map<Long, List<NvComp>> spatialGrid = new HashMap<>();
    private static final int COLLISION_CELL_SIZE = 250;

    private static final List<NvComp> canCollide = new ArrayList<>(20);

    public static void addCanCollide(NvComp component){
        canCollide.add(component);
    }
    public static void removeCanCollide(NvComp component){
        canCollide.remove(component);
    }

    private static CollisionSystem collisionSystem;

    /**
     * Standard is AABB
     * @param collisionSystem new CollisionSystem
     */
    public static void setCollisionSystem(CollisionSystem collisionSystem) {
        CollisionManager.collisionSystem = collisionSystem;
    }

    public static void handleCollisions() {

        spatialGrid.clear();
        int size = canCollide.size();

        for (NvComp comp : canCollide) {
            int cellX = comp.getX() / COLLISION_CELL_SIZE;
            int cellY = comp.getY() / COLLISION_CELL_SIZE;

            int endX = (comp.getX() + comp.getW()) / COLLISION_CELL_SIZE;
            int endY = (comp.getY() + comp.getH()) / COLLISION_CELL_SIZE;

            for (int x = cellX; x <= endX; x++) {
                for (int y = cellY; y <= endY; y++) {
                    long key = ((long) x << 32) | (y & 0xFFFFFFFFL);
                    spatialGrid.computeIfAbsent(key, k -> new ArrayList<>()).add(comp);
                }
            }
        }

        for (List<NvComp> cellContent : spatialGrid.values()) {
            int cellSize = cellContent.size();
            if (cellSize < 2) continue;

            for (int i = 0; i < cellSize; i++) {
                NvComp a = cellContent.get(i);
                for (int j = i + 1; j < cellSize; j++) {
                    NvComp b = cellContent.get(j);
                    if (collisionSystem.isColliding(a, b)) {
                        ((Collidable) a).whenCollide(b);
                        ((Collidable) b).whenCollide(a);
                        collisionSystem.resolveCollision(a, b);
                    }
                }
            }
        }
    }
}


