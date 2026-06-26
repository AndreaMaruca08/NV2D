package nv.core.assets;

import nv.core.annotations.EngineCore;
import nv.core.data.NvImage;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@EngineCore
@SuppressWarnings("unused")
public final class AtlasConverter {

    public record Region(float u1, float v1, float u2, float v2) { }
    public record Atlas(NvImage image, Map<String, Region> regions, int width, int height) {}

    public static Atlas build(VkDevice device,
                              VkPhysicalDevice physicalDevice,
                              VkQueue graphicsQueue,
                              String folder) throws IOException, URISyntaxException {

        URL resourceUrl = AtlasConverter.class.getResource("/" + folder);
        if (resourceUrl == null) {
            throw new EngineEx("Resource folder not found: " + folder);
        }

        List<BufferedImage> images = new ArrayList<>();
        List<String> names = new ArrayList<>();

        if (resourceUrl.getProtocol().equals("jar")) {
            String path = resourceUrl.getPath();
            String jarPath = path.substring(5, path.indexOf("!")); // Remove "file:" prefix
            String resourcePath = path.substring(path.indexOf("!") + 2); // Remove "!/" prefix
            
            // Decode the jarPath to handle spaces or special characters
            jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);

            // Ensure resourcePath ends with a '/' for proper directory matching
            if (!resourcePath.endsWith("/")) {
                resourcePath += "/";
            }

            try (JarFile jarFile = new JarFile(jarPath)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                List<JarEntry> imageEntries = new ArrayList<>();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    // Check if the entry is directly within the specified resourcePath directory
                    if (entryName.startsWith(resourcePath) && !entry.isDirectory() &&
                            (entryName.endsWith(".png") || entryName.endsWith(".jpg"))) {
                        // Further check to ensure it's not in a subfolder of resourcePath
                        String relativePath = entryName.substring(resourcePath.length());
                        if (!relativePath.contains("/")) { // Only add if it's a direct child
                            imageEntries.add(entry);
                        }
                    }
                }
                
                // Sort entries by name to ensure consistent atlas generation
                imageEntries.sort(Comparator.comparing(JarEntry::getName));

                for (JarEntry entry : imageEntries) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        BufferedImage img = ImageIO.read(is);
                        images.add(img);
                        names.add(getFileNameWithoutExtension(entry.getName()));
                    }
                }
            }

        } else { // Running from file system
            File dir = new File(resourceUrl.toURI());
            File[] files = dir.listFiles((d, name) ->
                    name.endsWith(".png") || name.endsWith(".jpg")
            );

            if (files == null || files.length == 0) {
                throw new EngineEx("No textures found in: " + dir.getAbsolutePath());
            }

            Arrays.sort(files);

            for (File f : files) {
                BufferedImage img = ImageIO.read(f);
                images.add(img);
                names.add(getFileNameWithoutExtension(f.getName()));
            }
        }

        if (images.isEmpty()) {
            throw new EngineEx("No textures found in: " + folder);
        }

        int tileW = -1;
        int tileH = -1;

        for (BufferedImage img : images) {
            if (tileW == -1) {
                tileW = img.getWidth();
                tileH = img.getHeight();
            }

            if (img.getWidth() != tileW || img.getHeight() != tileH) {
                throw new EngineEx("All textures must have same size");
            }
        }

        int cols = (int) Math.ceil(Math.sqrt(images.size()));
        int rows = (int) Math.ceil((double) images.size() / cols);

        int atlasW = cols * tileW;
        int atlasH = rows * tileH;

        BufferedImage atlasImg = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);

        Map<String, Region> regions = new HashMap<>();

        for (int i = 0; i < images.size(); i++) {

            int x = (i % cols) * tileW;
            int y = (i / cols) * tileH;

            BufferedImage img = images.get(i);
            atlasImg.getGraphics().drawImage(img, x, y, null);

            float u1 = (float) x / atlasW;
            float v1 = (float) y / atlasH;
            float u2 = (float) (x + tileW) / atlasW;
            float v2 = (float) (y + tileH) / atlasH;

            regions.put(names.get(i), new Region(u1, v1, u2, v2));
        }

        ByteBuffer buffer = convert(atlasImg);

        NvImage vkImage = NvImage.fromAtlasBuffer(
                device,
                physicalDevice,
                graphicsQueue,
                buffer,
                atlasW,
                atlasH
        );

        return new Atlas(vkImage, regions, atlasW, atlasH);
    }

    private static ByteBuffer convert(BufferedImage img) {

        int w = img.getWidth();
        int h = img.getHeight();

        // Use MemoryUtil.memAlloc to be compatible with MemoryUtil.memFree in NvImage
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int argb = img.getRGB(x, y);

                buf.put((byte) ((argb >> 16) & 0xFF));
                buf.put((byte) ((argb >> 8) & 0xFF));
                buf.put((byte) (argb & 0xFF));
                buf.put((byte) ((argb >> 24) & 0xFF));
            }
        }

        buf.flip();
        return buf;
    }

    private static String getFileNameWithoutExtension(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        String fileName = (lastSlash == -1) ? filePath : filePath.substring(lastSlash + 1);
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}