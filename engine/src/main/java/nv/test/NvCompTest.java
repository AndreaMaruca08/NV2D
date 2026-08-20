package nv.test;

import nv.core.camera.NvCamera;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvComp Core Component Unit Tests")
public class NvCompTest {

    private MockComp parent;
    private MockComp child;

    @BeforeEach
    void setUp() {
        NvGraphic.camera = new NvCamera(0f, 0f, 1f);
        parent = new MockComp(10, 20, 100, 200);
        child = new MockComp(30, 40, 50, 60);
    }

    @Test
    @DisplayName("Test initial component properties")
    void testInitialState() {
        assertEquals(10, parent.getX());
        assertEquals(20, parent.getY());
        assertEquals(100, parent.getW());
        assertEquals(200, parent.getH());
        assertNull(parent.getParent());
        assertTrue(parent.getChildren().isEmpty());
        assertEquals(0f, parent.rotation, 0.0001f);
        assertFalse(parent.isHUD());
        assertFalse(parent.isPhaseThrough());
        assertEquals(0, parent.getZIndex());
    }

    @Test
    @DisplayName("Test parent-child hierarchy management")
    void testParentChildRelationship() {
        parent.addChild(child);

        assertEquals(1, parent.getChildren().size());
        assertTrue(parent.getChildren().contains(child));
        assertEquals(parent, child.getParent());

        parent.removeChild(child);
        assertEquals(0, parent.getChildren().size());
    }

    @Test
    @DisplayName("Test translation by Vector2D and amount")
    void testTranslateVector2D() {
        parent.translate(Vector2D.RIGHT, 15f);
        assertEquals(25, parent.getX());
        assertEquals(20, parent.getY());

        parent.translate(Vector2D.DOWN, 10f);
        assertEquals(25, parent.getX());
        assertEquals(30, parent.getY());
    }

    @Test
    @DisplayName("Test rotation clockwise and counter-clockwise")
    void testRotate() {
        parent.rotate(45f, true);
        assertEquals(45f, parent.rotation, 0.0001f);

        parent.rotate(15f, false);
        assertEquals(30f, parent.rotation, 0.0001f);
    }

    @Test
    @DisplayName("Test dirty state tracking and propagation")
    void testDirtyStatePropagation() {
        parent.cleanDirty();
        child.cleanDirty();

        parent.addChild(child);

        child.cleanDirty();
        parent.cleanDirty();

        child.setX(100);

        assertTrue(child.isDirty(), "Child should be marked dirty on mutation");
        assertTrue(parent.isDirty(), "Parent should be marked dirty when child is dirty");

        parent.cleanDirty();
        assertFalse(parent.isDirty());
    }

    @Test
    @DisplayName("Test component spatial bounding check (isInside)")
    void testIsInside() {
        MockComp comp = new MockComp(10, 10, 100, 100);
        assertTrue(comp.isInside(50, 50), "Point inside bounds should return true");
        assertTrue(comp.isInside(10, 10), "Point on top-left edge should return true");
        assertTrue(comp.isInside(110, 110), "Point on bottom-right edge should return true");
        assertFalse(comp.isInside(5, 5), "Point outside bounds should return false");
        assertFalse(comp.isInside(120, 50), "Point outside right edge should return false");
    }
}
