package nv.core.assets;

import nv.core.NvContext;
import nv.core.annotations.EngineCore;
import nv.core.data.NvImage;
import nv.core.data.TextureImage;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@EngineCore
@SuppressWarnings("unused")
public final class AssetsManager {

    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final VkQueue graphicsQueue;

    private final DescriptorHook descriptorHook;

    public interface DescriptorHook {
        void bindTexture(int slot, TextureImage texture);
    }

    private final Map<String, AtlasConverter.Atlas> atlases = new HashMap<>();

    private NvImage defaultTexture;

    public AssetsManager(VkDevice device,
                         VkPhysicalDevice physicalDevice,
                         VkQueue graphicsQueue,
                         DescriptorHook descriptorHook) {

        this.device = device;
        this.physicalDevice = physicalDevice;
        this.graphicsQueue = graphicsQueue;
        this.descriptorHook = descriptorHook;
    }

    public AtlasConverter.Atlas loadAtlas(String name, String folder) {

        AtlasConverter.Atlas existing = atlases.get(name);
        if (existing != null) return existing;

        try {

            AtlasConverter.Atlas atlas = AtlasConverter.build(
                    device,
                    physicalDevice,
                    graphicsQueue,
                    "textures/" + folder
            );

            bindAtlas(atlas);
            atlases.put(name, atlas);

            return atlas;

        } catch (IOException | URISyntaxException e) {
            throw new EngineEx("Failed to load atlas: " + name + " specific: " + e);
        }
    }

    private void bindAtlas(AtlasConverter.Atlas atlas) {
        NvImage image = atlas.image();
        int slot = NvContext.getInstance().getNextTextureSlot();
        image.setTextureIndex(slot);

        // Registra in loadedTextures E aggiorna il descriptor
        NvContext.getInstance().registerTexture(slot, image);
        NvContext.markSceneDirty();
    }
    public AtlasConverter.Region getRegion(String atlasName, String textureName) {

        AtlasConverter.Atlas atlas = atlases.get(atlasName);

        if (atlas == null) {
            throw new EngineEx("Atlas not loaded: " + atlasName);
        }

        AtlasConverter.Region region = atlas.regions().get(textureName);

        if (region == null) {
            throw new EngineEx("Region not found: " + textureName + " in " + atlasName);
        }

        return region;
    }

    public AtlasConverter.Atlas getAtlas(String name) {
        return atlases.get(name);
    }

    public NvImage getAtlasTexture(String name) {
        AtlasConverter.Atlas atlas = atlases.get(name);
        return atlas != null ? atlas.image() : null;
    }

    public void setDefaultTexture(NvImage texture) {
        this.defaultTexture = texture;
    }

    public NvImage getDefaultTexture() {
        return defaultTexture;
    }

    public void clear() {
        atlases.clear();
        defaultTexture = null;
    }
}
