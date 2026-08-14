package nv.core;

import nv.core.annotations.EngineCore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@EngineCore
@SuppressWarnings("all")
public class MoltenVKBootstrap {

    public static void setup() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            return;
        }

        String arch = System.getProperty("os.arch");
        String nativeDir = arch.startsWith("aarch64") ? "macos-arm64" : "macos-x64";
        String resourcePath = "/natives/" + nativeDir + "/libMoltenVK.dylib";

        try {
            Path tempDir = Files.createTempDirectory("nv2d_moltenvk");
            Path targetFile = tempDir.resolve("libMoltenVK.dylib");

            try (InputStream in = MoltenVKBootstrap.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IllegalStateException(
                        "libMoltenVK.dylib non trovata nelle risorse: " + resourcePath +
                        " — controlla che sia inclusa nel jar."
                    );
                }
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            targetFile.toFile().setReadable(true, false);
            targetFile.toFile().setExecutable(true, false);

            System.setProperty("org.lwjgl.vulkan.libname", targetFile.toAbsolutePath().toString());

            targetFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();

        } catch (IOException e) {
            throw new RuntimeException("Impossibile estrarre libMoltenVK.dylib bundled", e);
        }
    }
}