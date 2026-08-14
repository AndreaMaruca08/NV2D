package nv.test;

import nv.core.collision.AABB;
import nv.core.collision.CollisionSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AABB Collision System Unit Tests")
public class AABBTest {

    private AABB aabb;

    @BeforeEach
    void setUp() {
        aabb = new AABB();
    }

    @Test
    @DisplayName("Test collision detection when bounding boxes overlap")
    void testIsCollidingTrue() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(25, 25, 50, 50);

        assertTrue(aabb.isColliding(a, b), "Overlapping components should collide");
        assertTrue(aabb.isColliding(b, a), "Symmetric collision check should hold");
    }

    @Test
    @DisplayName("Test collision detection when bounding boxes do not overlap")
    void testIsCollidingFalse() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(100, 100, 50, 50);

        assertFalse(aabb.isColliding(a, b), "Disjoint components should not collide");
    }

    @Test
    @DisplayName("Test collision detection at exact adjacent edges (no overlap)")
    void testIsCollidingEdgeTouching() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(50, 0, 50, 50);

        assertFalse(aabb.isColliding(a, b), "Adjacent edges touching without overlap should not collide");
    }

    @Test
    @DisplayName("Test collision resolution with equal weight components")
    void testResolveCollisionEqualWeights() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(40, 0, 50, 50); // overlap ox = 10 on X axis

        a.setWeight(10);
        b.setWeight(10);

        aabb.resolveCollision(a, b);

        // Overlap is dx1 = 10. Equal weight split 50%-50%, so 'a' moves left by 5, 'b' moves right by 5
        assertEquals(-5, a.getX());
        assertEquals(45, b.getX());
    }

    @Test
    @DisplayName("Test collision resolution when one component has MAX_WEIGHT")
    void testResolveCollisionOneImmovable() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(40, 0, 50, 50);

        a.setWeight(10);
        b.setWeight(CollisionSystem.MAX_WEIGHT); // 'b' cannot move

        aabb.resolveCollision(a, b);

        // 'b' stays at 40, 'a' moves left by entire overlap ox = 10 -> x = -10
        assertEquals(-10, a.getX());
        assertEquals(40, b.getX());
    }

    @Test
    @DisplayName("Test collision resolution when both components have MAX_WEIGHT")
    void testResolveCollisionBothImmovable() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(40, 0, 50, 50);

        a.setWeight(CollisionSystem.MAX_WEIGHT);
        b.setWeight(CollisionSystem.MAX_WEIGHT);

        aabb.resolveCollision(a, b);

        // Neither component should move
        assertEquals(0, a.getX());
        assertEquals(40, b.getX());
    }
}
