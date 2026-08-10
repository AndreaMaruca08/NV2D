package nv.core.io;

import nv.core.annotations.EngineCore;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>Utility class to resolve application base paths automatically across all deployment modes:</p>
 * <ul>
 *     <li><b>macOS .app Bundle (jpackage)</b>: Returns the directory containing the .app bundle (so external folders stay alongside the app).</li>
 *     <li><b>Standalone JAR</b>: Returns the directory containing the executed .jar file.</li>
 *     <li><b>jpackage Windows/Linux</b>: Returns the application installation root directory (parent of 'app').</li>
 *     <li><b>IDE / Gradle execution</b>: Returns the project root directory (user.dir).</li>
 * </ul>
 *
 * @author Andrea Maruca
 * @since 1.4
 */
@EngineCore
public final class AppPathUtils {

    private AppPathUtils() {}

    /**
     * Determines the base directory of the application automatically.
     *
     * @return Path representing the root folder of the app executable or project
     */
    public static Path getAppDirectory() {
        try {
            URI location = AppPathUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Paths.get(location);
            Path dir = Files.isDirectory(codePath) ? codePath : codePath.getParent();

            Path current = dir;
            while (current != null) {
                if (current.getFileName() != null && current.getFileName().toString().endsWith(".app")) {
                    Path appParent = current.getParent();
                    if (appParent != null) {
                        return appParent;
                    }
                    break;
                }
                current = current.getParent();
            }

            String pathStr = codePath.toString();
            if (pathStr.contains("build" + File.separator + "classes") ||
                pathStr.contains("build" + File.separator + "libs") ||
                pathStr.contains("target" + File.separator + "classes")) {
                return Paths.get(System.getProperty("user.dir"));
            }

            if (dir != null && dir.getFileName() != null && dir.getFileName().toString().equalsIgnoreCase("app")) {
                Path parent = dir.getParent();
                if (parent != null) {
                    return parent;
                }
            }

            return dir != null ? dir : Paths.get(System.getProperty("user.dir"));
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    /**
     * Resolves a relative path string against the automatic app base directory.
     * If the given path is already absolute, it is returned unchanged.
     *
     * @param relativeOrAbsolutePath path string
     * @return Path resolved against app directory
     */
    public static Path resolvePath(String relativeOrAbsolutePath) {
        Path path = Paths.get(relativeOrAbsolutePath);
        if (path.isAbsolute()) {
            return path;
        }
        return getAppDirectory().resolve(path);
    }
}
