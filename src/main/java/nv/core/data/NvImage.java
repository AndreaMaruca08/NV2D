package nv.core.data;

import nv.core.NvGraphic;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * <h3>Immagine caricata su GPU</h3>
 * <p>Carica un'immagine da file o risorsa classpath e la rende disponibile per il rendering.
 * Supporta immagini singole e texture atlas (specificando le coordinate UV nella chiamata draw).</p>
 *
 * <p>Per disegnare una regione specifica di un texture atlas usare
 * {@link NvGraphic#drawImageRegion}.</p>
 *
 * @since 1.0
 * @author Andrea Maruca
 */
public class NvImage implements AutoCloseable {

    private final TextureImage textureImage;
    private final int width;
    private final int height;
    private int textureIndex = -1;

    private NvImage(VkDevice device, VkPhysicalDevice physicalDevice,
                    VkQueue graphicsQueue, BufferedImage bufferedImage) {
        this.width  = bufferedImage.getWidth();
        this.height = bufferedImage.getHeight();

        ByteBuffer pixels = toRGBA(bufferedImage);
        try {
            this.textureImage = new TextureImage(device, physicalDevice, graphicsQueue,
                    pixels, width, height);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    /**
     * Carica un'immagine da un percorso file assoluto o relativo.
     */
    public static NvImage fromFile(VkDevice device, VkPhysicalDevice physicalDevice,
                                   VkQueue graphicsQueue, String filePath) {
        try {
            BufferedImage img = ImageIO.read(new File(filePath));
            if (img == null) throw new RuntimeException("Formato immagine non supportato: " + filePath);
            return new NvImage(device, physicalDevice, graphicsQueue, img);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare l'immagine: " + filePath, e);
        }
    }

    /**
     * Carica un'immagine dal classpath (es. {@code "/images/sprite.png"}).
     */
    public static NvImage fromResource(VkDevice device, VkPhysicalDevice physicalDevice,
                                       VkQueue graphicsQueue, String resourcePath) {
        try (InputStream is = NvImage.class.getResourceAsStream(resourcePath)) {
            if (is == null) throw new RuntimeException("Risorsa non trovata: " + resourcePath);
            BufferedImage img = ImageIO.read(is);
            if (img == null) throw new RuntimeException("Formato immagine non supportato: " + resourcePath);
            return new NvImage(device, physicalDevice, graphicsQueue, img);
        } catch (IOException e) {
            throw new RuntimeException("Impossibile caricare la risorsa immagine: " + resourcePath, e);
        }
    }

    private static ByteBuffer toRGBA(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                buf.put((byte) ((argb >> 16) & 0xFF)); // R
                buf.put((byte) ((argb >>  8) & 0xFF)); // G
                buf.put((byte) (argb         & 0xFF)); // B
                buf.put((byte) ((argb >> 24) & 0xFF)); // A
            }
        }
        buf.flip();
        return buf;
    }

    /** Indice del texture slot assegnato da {@code NvContext.loadImage()}. -1 se non ancora registrata. */
    public int getTextureIndex() { return textureIndex; }

    /** Usato internamente da {@code NvContext} per assegnare il texture slot. */
    public void setTextureIndex(int index) { this.textureIndex = index; }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public TextureImage getTextureImage() { return textureImage; }

    @Override
    public void close() {
        textureImage.close();
    }
}
