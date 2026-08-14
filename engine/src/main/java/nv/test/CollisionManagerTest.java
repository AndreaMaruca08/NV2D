package nv.test;

import nv.core.collision.CollisionManager;
import nv.core.collision.CollisionSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CollisionManager Unit Tests")
public class CollisionManagerTest {

    @BeforeEach
    void setUp() {
        CollisionManager.initialize();
    }

    @Test
    @DisplayName("Test collision handling between two colliding components")
    void testHandleCollisions() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(20, 0, 50, 50);

        CollisionManager.addCanCollide(a);
        CollisionManager.addCanCollide(b);

        CollisionManager.handleCollisions();

        assertTrue(a.isCollided(), "Component 'a' should register collision");
        assertTrue(b.isCollided(), "Component 'b' should register collision");
        assertEquals(b, a.getCollidedWith());
        assertEquals(a, b.getCollidedWith());

        // Clean up static list
        CollisionManager.removeCanCollide(a);
        CollisionManager.removeCanCollide(b);
    }

    @Test
    @DisplayName("Test collision handling when components have different zIndex")
    void testCollisionDifferentZIndex() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(20, 0, 50, 50);

        a.setZIndex(0);
        b.setZIndex(1);

        CollisionManager.addCanCollide(a);
        CollisionManager.addCanCollide(b);

        CollisionManager.handleCollisions();

        assertFalse(a.isCollided(), "Components with different zIndex should not collide");
        assertFalse(b.isCollided(), "Components with different zIndex should not collide");

        CollisionManager.removeCanCollide(a);
        CollisionManager.removeCanCollide(b);
    }

    @Test
    @DisplayName("Test phaseThrough component triggers callback but skips physical resolution")
    void testPhaseThroughCollision() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(20, 0, 50, 50);

        a.setPhaseThrough(true);
        a.setWeight(10);
        b.setWeight(10);

        CollisionManager.addCanCollide(a);
        CollisionManager.addCanCollide(b);

        CollisionManager.handleCollisions();

        assertTrue(a.isCollided(), "Phase-through component should still fire whenCollide");
        assertTrue(b.isCollided(), "Colliding partner should still fire whenCollide");

        // Coordinates must NOT be modified by resolution
        assertEquals(0, a.getX());
        assertEquals(20, b.getX());

        CollisionManager.removeCanCollide(a);
        CollisionManager.removeCanCollide(b);
    }

    @Test
    @DisplayName("Test removeCanCollide successfully unregisters component")
    void testRemoveCanCollide() {
        MockComp a = new MockComp(0, 0, 50, 50);
        MockComp b = new MockComp(20, 0, 50, 50);

        CollisionManager.addCanCollide(a);
        CollisionManager.addCanCollide(b);
        CollisionManager.removeCanCollide(b);

        CollisionManager.handleCollisions();

        assertFalse(a.isCollided(), "No collision should occur after removing 'b'");

        CollisionManager.removeCanCollide(a);
    }
}
