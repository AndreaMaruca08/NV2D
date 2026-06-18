package nv.core.assets;

import nv.core.annotations.EngineCore;
import nv.core.data.NvImage;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

@EngineCore
@SuppressWarnings("unused")
public final class AtlasConverter {

    public record Region(float u1, float v1, float u2, float v2) { }
    public record Atlas(NvImage image, Map<String, Region> regions, int width, int height) {}

    public static Atlas build(VkDevice device,
                              VkPhysicalDevice physicalDevice,
                              VkQueue graphicsQueue,
                              String folder) throws IOException {

        // Point to the source resources directory
        File dir = new File("src/main/resources/textures/" + folder);

        File[] files = dir.listFiles((d, name) ->
                name.endsWith(".png") || name.endsWith(".jpg")
        );

        if (files == null || files.length == 0) {
            throw new RuntimeException("No textures found in: " + dir.getAbsolutePath());
        }

        Arrays.sort(files);

        List<BufferedImage> images = new ArrayList<>();
        List<String> names = new ArrayList<>();

        int tileW = -1;
        int tileH = -1;

        for (File f : files) {
            BufferedImage img = ImageIO.read(f);

            if (tileW == -1) {
                tileW = img.getWidth();
                tileH = img.getHeight();
            }

            if (img.getWidth() != tileW || img.getHeight() != tileH) {
                throw new RuntimeException("All textures must have same size");
            }

            images.add(img);
            names.add(f.getName().replace(".png", "").replace(".jpg", ""));
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
}
