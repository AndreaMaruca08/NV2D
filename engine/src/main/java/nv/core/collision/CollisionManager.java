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
    private static final PairSet checkedPairs = new PairSet(1024);

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

    /** Updated in 1.6. */
    public static void handleCollisions() {
        spatialGrid.clear();
        checkedPairs.clear();

        int n = canCollide.size();
        for (int idx = 0; idx < n; idx++) {
            NvComp comp = canCollide.get(idx);

            int cellX = Math.floorDiv(comp.getX(), COLLISION_CELL_SIZE);
            int cellY = Math.floorDiv(comp.getY(), COLLISION_CELL_SIZE);
            int endX = Math.floorDiv(comp.getX() + comp.getW(), COLLISION_CELL_SIZE);
            int endY = Math.floorDiv(comp.getY() + comp.getH(), COLLISION_CELL_SIZE);

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
                int aIndex = indices[i];
                NvComp a = canCollide.get(aIndex);
                for (int j = i + 1; j < cellSize; j++) {
                    int bIndex = indices[j];
                    if (!checkedPairs.add(aIndex, bIndex)) {
                        continue;
                    }
                    NvComp b = canCollide.get(bIndex);
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

    /** Reusable primitive set that prevents a pair spanning multiple cells from being processed repeatedly. */
    private static final class PairSet {
        private long[] keys;
        private boolean[] used;
        private int[] touched;
        private int mask;
        private int size;
        private int touchedCount;

        /** @since 1.6 */
        PairSet(int initialCapacity) {
            allocate(initialCapacity);
        }

        /** @since 1.6 */
        void clear() {
            for (int i = 0; i < touchedCount; i++) {
                used[touched[i]] = false;
            }
            size = 0;
            touchedCount = 0;
        }

        /** @since 1.6 */
        boolean add(int first, int second) {
            if ((size << 1) >= keys.length) {
                grow();
            }

            int min = (first < second) ? first : second;
            int max = (first < second) ? second : first;
            long key = ((long) min << 32) | (max & 0xFFFF_FFFFL);
            int slot = indexFor(key);
            if (used[slot]) {
                return false;
            }

            used[slot] = true;
            keys[slot] = key;
            touched[touchedCount++] = slot;
            size++;
            return true;
        }

        /** @since 1.6 */
        private void allocate(int requestedCapacity) {
            int capacity = Integer.highestOneBit(Math.max(16, requestedCapacity - 1)) << 1;
            keys = new long[capacity];
            used = new boolean[capacity];
            touched = new int[capacity];
            mask = capacity - 1;
            size = 0;
            touchedCount = 0;
        }

        /** @since 1.6 */
        private int indexFor(long key) {
            int slot = (int) (mix(key) & mask);
            while (used[slot] && keys[slot] != key) {
                slot = (slot + 1) & mask;
            }
            return slot;
        }

        /** @since 1.6 */
        private void grow() {
            long[] oldKeys = keys;
            boolean[] oldUsed = used;
            int[] oldTouched = touched;
            int oldTouchedCount = touchedCount;
            allocate(keys.length << 1);

            for (int i = 0; i < oldTouchedCount; i++) {
                int oldSlot = oldTouched[i];
                if (!oldUsed[oldSlot]) {
                    continue;
                }
                long key = oldKeys[oldSlot];
                int newSlot = indexFor(key);
                used[newSlot] = true;
                keys[newSlot] = key;
                touched[touchedCount++] = newSlot;
                size++;
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
            // splitmix64 finalizer: ottima distribuzione, evita cluster sulle chiavi
            // (x,y) tipiche di una griglia, che con hash banali tendono ad allinearsi.
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }
    }

    /** @since 1.6 */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
