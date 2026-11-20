import nv.core.camera.NvCamera;
import nv.core.components.Vector2D;
import nv.core.graphic.NvGraphic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

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
        Assertions.assertEquals(10, parent.getX());
        Assertions.assertEquals(20, parent.getY());
        Assertions.assertEquals(100, parent.getW());
        Assertions.assertEquals(200, parent.getH());
        Assertions.assertNull(parent.getParent());
        Assertions.assertTrue(parent.getChildren().isEmpty());
        Assertions.assertEquals(0f, parent.rotation, 0.0001f);
        Assertions.assertFalse(parent.isHUD());
        Assertions.assertFalse(parent.isPhaseThrough());
        Assertions.assertEquals(0, parent.getZIndex());
    }

    @Test
    @DisplayName("Test parent-child hierarchy management")
    void testParentChildRelationship() {
        parent.addChild(child);

        Assertions.assertEquals(1, parent.getChildren().size());
        Assertions.assertTrue(parent.getChildren().contains(child));
        Assertions.assertEquals(parent, child.getParent());

        parent.removeChild(child);
        Assertions.assertEquals(0, parent.getChildren().size());
    }

    @Test
    @DisplayName("Test translation by Vector2D and amount")
    void testTranslateVector2D() {
        parent.translate(Vector2D.RIGHT, 15f);
        Assertions.assertEquals(25, parent.getX());
        Assertions.assertEquals(20, parent.getY());

        parent.translate(Vector2D.DOWN, 10f);
        Assertions.assertEquals(25, parent.getX());
        Assertions.assertEquals(30, parent.getY());
    }

    @Test
    @DisplayName("Test rotation clockwise and counter-clockwise")
    void testRotate() {
        parent.rotate(45f, true);
        Assertions.assertEquals(45f, parent.rotation, 0.0001f);

        parent.rotate(15f, false);
        Assertions.assertEquals(30f, parent.rotation, 0.0001f);
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

        Assertions.assertTrue(child.isDirty(), "Child should be marked dirty on mutation");
        Assertions.assertTrue(parent.isDirty(), "Parent should be marked dirty when child is dirty");

        parent.cleanDirty();
        Assertions.assertFalse(parent.isDirty());
    }

    @Test
    @DisplayName("Test component spatial bounding check (isInside)")
    void testIsInside() {
        MockComp comp = new MockComp(10, 10, 100, 100);
        Assertions.assertTrue(comp.isInside(50, 50), "Point inside bounds should return true");
        Assertions.assertTrue(comp.isInside(10, 10), "Point on top-left edge should return true");
        Assertions.assertTrue(comp.isInside(110, 110), "Point on bottom-right edge should return true");
        Assertions.assertFalse(comp.isInside(5, 5), "Point outside bounds should return false");
        Assertions.assertFalse(comp.isInside(120, 50), "Point outside right edge should return false");
    }
}
