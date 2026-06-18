package nv.core.data;

import nv.core.annotations.EngineCore;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * GPU image (Vulkan).
 * Supports both atlas textures and single images
 * @since 1.0
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class NvImage implements AutoCloseable {

    private final TextureImage textureImage;
    private final int width;
    private final int height;

    private int textureIndex = -1;

    // --- atlas support ---
    private boolean atlas = false;

    private NvImage(VkDevice device,
                    VkPhysicalDevice physicalDevice,
                    VkQueue graphicsQueue,
                    BufferedImage bufferedImage) {

        this.width = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();

        ByteBuffer pixels = toRGBA(bufferedImage);

        try {
            this.textureImage = new TextureImage(
                    device,
                    physicalDevice,
                    graphicsQueue,
                    pixels,
                    width,
                    height
            );
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    /**
     * Costruttore interno per atlas (buffer già pronto)
     */
    private NvImage(VkDevice device,
                    VkPhysicalDevice physicalDevice,
                    VkQueue graphicsQueue,
                    ByteBuffer rgbaBuffer,
                    int width,
                    int height,
                    boolean atlas) {

        this.width = width;
        this.height = height;
        this.atlas = atlas;

        this.textureImage = new TextureImage(
                device,
                physicalDevice,
                graphicsQueue,
                rgbaBuffer,
                width,
                height
        );

        MemoryUtil.memFree(rgbaBuffer);
    }

    // -------------------------------------------------------------
    // FACTORY: FILE
    // -------------------------------------------------------------

    public static NvImage fromFile(VkDevice device,
                                   VkPhysicalDevice physicalDevice,
                                   VkQueue graphicsQueue,
                                   String filePath) {

        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            if (img == null) {
                throw new RuntimeException("Formato immagine non supportato: " + filePath);
            }
            return new NvImage(device, physicalDevice, graphicsQueue, img);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare immagine: " + filePath, e);
        }
    }

    // -------------------------------------------------------------
    // FACTORY: CLASSPATH
    // -------------------------------------------------------------

    public static NvImage fromResource(VkDevice device,
                                       VkPhysicalDevice physicalDevice,
                                       VkQueue graphicsQueue,
                                       String resourcePath) {

        try (InputStream is = NvImage.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Risorsa non trovata: " + resourcePath);
            }

            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new RuntimeException("Formato immagine non supportato: " + resourcePath);
            }

            return new NvImage(device, physicalDevice, graphicsQueue, img);

        } catch (IOException e) {
            throw new RuntimeException("Errore caricamento risorsa: " + resourcePath, e);
        }
    }

    // -------------------------------------------------------------
    // FACTORY: ATLAS BUFFER (VULKAN CORRECT)
    // -------------------------------------------------------------

    public static NvImage fromAtlasBuffer(VkDevice device,
                                          VkPhysicalDevice physicalDevice,
                                          VkQueue graphicsQueue,
                                          ByteBuffer rgbaBuffer,
                                          int width,
                                          int height) {

        return new NvImage(device, physicalDevice, graphicsQueue,
                rgbaBuffer, width, height, true);
    }

    // -------------------------------------------------------------
    // CONVERSIONE CPU → RGBA
    // -------------------------------------------------------------

    private static ByteBuffer toRGBA(BufferedImage img) {

        int w = img.getWidth();
        int h = img.getHeight();

        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int argb = img.getRGB(x, y);

                buf.put((byte) ((argb >> 16) & 0xFF)); // R
                buf.put((byte) ((argb >> 8) & 0xFF));  // G
                buf.put((byte) (argb & 0xFF));         // B
                buf.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }

        buf.flip();
        return buf;
    }

    // -------------------------------------------------------------
    // GETTERS
    // -------------------------------------------------------------

    public int getTextureIndex() {
        return textureIndex;
    }

    public void setTextureIndex(int index) {
        this.textureIndex = index;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureImage getTextureImage() {
        return textureImage;
    }

    public boolean isAtlas() {
        return atlas;
    }

    // -------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------

    @Override
    public void close() {
        textureImage.close();
    }
}