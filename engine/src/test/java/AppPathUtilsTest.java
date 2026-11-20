import nv.core.io.AppPathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;


@DisplayName("AppPathUtils Unit Tests")
public class AppPathUtilsTest {

    @Test
    @DisplayName("Test getAppDirectory returns non-null valid path")
    void testGetAppDirectory() {
        Path appDir = AppPathUtils.getAppDirectory();
        Assertions.assertNotNull(appDir, "App directory should not be null");
        Assertions.assertTrue(appDir.isAbsolute(), "App directory path should be absolute");
    }

    @Test
    @DisplayName("Test resolvePath with relative path")
    void testResolveRelativePath() {
        Path resolved = AppPathUtils.resolvePath("save/save.bin");
        Assertions.assertNotNull(resolved);
        Assertions.assertTrue(resolved.isAbsolute());
        Assertions.assertTrue(resolved.endsWith(Path.of("save", "save.bin")));
    }

    @Test
    @DisplayName("Test resolvePath with absolute path")
    void testResolveAbsolutePath() {
        Path absolute = Path.of(System.getProperty("user.home"), "testfile.dat").toAbsolutePath();
        Path resolved = AppPathUtils.resolvePath(absolute.toString());
        Assertions.assertEquals(absolute, resolved, "Absolute path should be returned unchanged");
    }
}
