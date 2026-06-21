package nv.core.collision;

import nv.core.annotations.EngineCore;
import nv.core.components.NvComp;

import java.util.ArrayList;
import java.util.List;

/**
 * Efficient class responsible for managing collisions between game components.
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class CollisionManager {
    private CollisionManager(){}

    public static void initialize(){
        collisionSystem = new AABB();
    }

    private static final int COLLISION_CELL_SIZE = 250;

    private static final List<NvComp> canCollide = new ArrayList<>(20);

    private static final CellMultiMap spatialGrid = new CellMultiMap(256);

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

        int n = canCollide.size();
        for (int idx = 0; idx < n; idx++) {
            NvComp comp = canCollide.get(idx);

            int cellX = comp.getX() / COLLISION_CELL_SIZE;
            int cellY = comp.getY() / COLLISION_CELL_SIZE;
            int endX = (comp.getX() + comp.getW()) / COLLISION_CELL_SIZE;
            int endY = (comp.getY() + comp.getH()) / COLLISION_CELL_SIZE;

            for (int x = cellX; x <= endX; x++) {
                for (int y = cellY; y <= endY; y++) {
                    long key = ((long) x << 32) | (y & 0xFFFFFFFFL);
                    spatialGrid.add(key, idx);
                }
            }
        }

        int bucketCount = spatialGrid.touchedCount;
        int[] touched = spatialGrid.touched;
        int[][] values = spatialGrid.values;
        int[] sizes = spatialGrid.valueSizes;

        for (int t = 0; t < bucketCount; t++) {
            int slot = touched[t];
            int cellSize = sizes[slot];
            if (cellSize < 2) continue;

            int[] indices = values[slot];
            for (int i = 0; i < cellSize; i++) {
                NvComp a = canCollide.get(indices[i]);
                for (int j = i + 1; j < cellSize; j++) {
                    NvComp b = canCollide.get(indices[j]);
                    if (a.getZIndex() != b.getZIndex())
                        continue;
                    if (collisionSystem.isColliding(a, b)) {
                        ((Collidable) a).whenCollide(b);
                        ((Collidable) b).whenCollide(a);
                        if (a.isPhaseThrough() || b.isPhaseThrough())
                            continue;
                        collisionSystem.resolveCollision(a, b);
                    }
                }
            }
        }
    }

    /**
     * @since 1.1
     */
    private static final class CellMultiMap {
        long[] keys;
        int[][] values;
        int[] valueSizes;
        boolean[] used;
        int[] touched;
        int touchedCount;
        int mask;
        int size;

        CellMultiMap(int initialCapacity) {
            int cap = Integer.highestOneBit(Math.max(16, initialCapacity - 1)) << 1;
            allocate(cap);
        }

        private void allocate(int cap) {
            keys = new long[cap];
            values = new int[cap][];
            valueSizes = new int[cap];
            used = new boolean[cap];
            touched = new int[cap];
            mask = cap - 1;
            touchedCount = 0;
            size = 0;
        }

        void clear() {
            for (int i = 0; i < touchedCount; i++) {
                int idx = touched[i];
                used[idx] = false;
                valueSizes[idx] = 0;
            }
            touchedCount = 0;
            size = 0;
        }

        void add(long key, int value) {
            if ((size << 1) >= keys.length) grow();

            int idx = indexFor(key);
            if (!used[idx]) {
                used[idx] = true;
                keys[idx] = key;
                if (values[idx] == null) values[idx] = new int[4];
                valueSizes[idx] = 0;
                touched[touchedCount++] = idx;
                size++;
            }

            int[] arr = values[idx];
            int sz = valueSizes[idx];
            if (sz == arr.length) {
                arr = java.util.Arrays.copyOf(arr, arr.length << 1);
                values[idx] = arr;
            }
            arr[sz] = value;
            valueSizes[idx] = sz + 1;
        }

        private int indexFor(long key) {
            int idx = (int) (mix(key) & mask);
            while (used[idx] && keys[idx] != key) {
                idx = (idx + 1) & mask;
            }
            return idx;
        }

        private void grow() {
            long[] oldKeys = keys;
            int[][] oldValues = values;
            int[] oldSizes = valueSizes;
            boolean[] oldUsed = used;
            int[] oldTouched = touched;
            int oldTouchedCount = touchedCount;

            allocate(keys.length << 1);

            for (int i = 0; i < oldTouchedCount; i++) {
                int oi = oldTouched[i];
                if (!oldUsed[oi]) continue;

                long k = oldKeys[oi];
                int idx = indexFor(k);
                used[idx] = true;
                keys[idx] = k;
                values[idx] = oldValues[oi];
                valueSizes[idx] = oldSizes[oi];
                touched[touchedCount++] = idx;
                size++;
            }
        }

        private static long mix(long z) {
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }
    }
}