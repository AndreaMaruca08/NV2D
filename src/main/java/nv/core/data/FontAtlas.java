package nv.core.data;

import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FontAtlas {
    public static class Glyph {
        public final float width;
        public final float height;
        public final float uMin;
        public final float vMin;
        public final float uMax;
        public final float vMax;
        public final float advance;

        public Glyph(float width, float height, float uMin, float vMin, float uMax, float vMax, float advance) {
            this.width = width;
            this.height = height;
            this.uMin = uMin;
            this.vMin = vMin;
            this.uMax = uMax;
            this.vMax = vMax;
            this.advance = advance;
        }
    }

    private final int width;
    private final int height;
    private final ByteBuffer pixelBuffer;
    private final Map<Character, Glyph> glyphs = new HashMap<>();

    public FontAtlas(Font font) {
        int imgSize = 512;
        this.width = imgSize;
        this.height = imgSize;

        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setFont(font);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 16, 16);

        FontMetrics fm = g2d.getFontMetrics();

        // Partiamo da X = 20 per la prima riga, lasciando intatto il nostro blocco bianco
        int x = 20;
        int y = fm.getAscent();
        int rowHeight = fm.getHeight();

        // Generiamo i caratteri ASCII stampabili standard
        for (int i = 32; i < 127; i++) {
            char c = (char) i;
            int charWidth = fm.charWidth(c);

            // Se andiamo a capo, qui x torna a 0 (sicuro, perché la riga 2 è sotto i 16px del blocco)
            if (x + charWidth > imgSize) {
                x = 0;
                y += rowHeight;
            }

            g2d.drawString(String.valueOf(c), x, y);

            float uMin = (float) x / imgSize;
            float vMin = (float) (y - fm.getAscent()) / imgSize;
            float uMax = (float) (x + charWidth) / imgSize;
            float vMax = (float) (y - fm.getAscent() + rowHeight) / imgSize;

            glyphs.put(c, new Glyph(charWidth, rowHeight, uMin, vMin, uMax, vMax, charWidth));

            x += charWidth + 2;
        }

        g2d.dispose();

        // Estrazione dei pixel dall'immagine Java e conversione in un ByteBuffer (RGBA) per Vulkan
        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);

        // Allocazione nativa (Off-heap) necessaria per passarlo a LWJGL/Vulkan
        this.pixelBuffer = MemoryUtil.memAlloc(width * height * 4);

        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            // Estrazione manuale dei canali dal formato ARGB di Java
            byte a = (byte) ((argb >> 24) & 0xFF);
            byte r = (byte) ((argb >> 16) & 0xFF);
            byte g = (byte) ((argb >> 8) & 0xFF);
            byte b = (byte) (argb & 0xFF);

            // Inserimento nel buffer come RGBA
            pixelBuffer.put(r);
            pixelBuffer.put(g);
            pixelBuffer.put(b);
            pixelBuffer.put(a);
        }
        
        // Prepariamo il buffer per la lettura
        pixelBuffer.flip();
    }

    public Glyph getGlyph(char c) {
        // Ritorna il carattere richiesto, o un punto interrogativo/spazio se non esiste (fallback)
        return glyphs.getOrDefault(c, glyphs.getOrDefault('?', glyphs.get(' ')));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ByteBuffer getPixelBuffer() {
        return pixelBuffer;
    }

    public void close() {
        // Pulizia della memoria nativa quando non serve più
        if (pixelBuffer != null) {
            MemoryUtil.memFree(pixelBuffer);
        }
    }
}