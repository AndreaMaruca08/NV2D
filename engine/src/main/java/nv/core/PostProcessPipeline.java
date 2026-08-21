package nv.core;

import nv.core.annotations.EngineCore;
import nv.core.errors.ex.EngineEx;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Fullscreen Vulkan Post-Processing Pipeline.
 *
 * @since 1.6.2
 * @author Andrea Maruca
 */
@EngineCore
@SuppressWarnings("unused")
public final class PostProcessPipeline implements AutoCloseable {

    private final VkDevice device;
    private long descriptorSetLayoutHandle;
    private long descriptorPoolHandle;
    private long descriptorSetHandle;

    private long pipelineLayoutHandle;
    private long pipelineHandle;

    private long swapchainRenderPassHandle;
    private long[] swapchainFramebufferHandles;

    public PostProcessPipeline(VkDevice device, Swapchain swapchain, InternalRenderTarget renderTarget) {
        this.device = device;

        createDescriptorSetLayout();
        createDescriptorPool();
        allocateDescriptorSet();
        updateDescriptorSet(renderTarget);

        createSwapchainRenderPass(swapchain);
        createSwapchainFramebuffers(swapchain);
        createPipeline(swapchain);
    }

    private void createDescriptorSetLayout() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            bindings.get(0)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(bindings);

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, pLayout) != VK_SUCCESS) {
                throw new EngineEx("Failed to create PostProcess Descriptor Set Layout.");
            }
            this.descriptorSetLayoutHandle = pLayout.get(0);
        }
    }

    private void createDescriptorPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(1, stack);
            poolSizes.get(0)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(poolSizes)
                    .maxSets(1);

            LongBuffer pPool = stack.mallocLong(1);
            if (vkCreateDescriptorPool(device, poolInfo, null, pPool) != VK_SUCCESS) {
                throw new EngineEx("Failed to create PostProcess Descriptor Pool.");
            }
            this.descriptorPoolHandle = pPool.get(0);
        }
    }

    private void allocateDescriptorSet() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPoolHandle)
                    .pSetLayouts(stack.longs(descriptorSetLayoutHandle));

            LongBuffer pSet = stack.mallocLong(1);
            if (vkAllocateDescriptorSets(device, allocInfo, pSet) != VK_SUCCESS) {
                throw new EngineEx("Failed to allocate PostProcess Descriptor Set.");
            }
            this.descriptorSetHandle = pSet.get(0);
        }
    }

    public void updateDescriptorSet(InternalRenderTarget renderTarget) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.get(0)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(renderTarget.getImageViewHandle())
                    .sampler(renderTarget.getSamplerHandle());

            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack);
            write.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSetHandle)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            vkUpdateDescriptorSets(device, write, null);
        }
    }

    private void createSwapchainRenderPass(Swapchain swapchain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack)
                    .format(swapchain.getFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(colorRef);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(colorAttachment)
                    .pSubpasses(subpass)
                    .pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);
            if (vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new EngineEx("Failed to create Swapchain RenderPass for PostProcess.");
            }
            this.swapchainRenderPassHandle = pRenderPass.get(0);
        }
    }

    private void createSwapchainFramebuffers(Swapchain swapchain) {
        long[] imageViews = swapchain.getImageViews();
        int imageCount = imageViews.length;
        this.swapchainFramebufferHandles = new long[imageCount];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < imageCount; i++) {
                VkFramebufferCreateInfo fbInfo = VkFramebufferCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(swapchainRenderPassHandle)
                        .pAttachments(stack.longs(imageViews[i]))
                        .width(swapchain.getWidth())
                        .height(swapchain.getHeight())
                        .layers(1);

                LongBuffer pFb = stack.mallocLong(1);
                if (vkCreateFramebuffer(device, fbInfo, null, pFb) != VK_SUCCESS) {
                    throw new EngineEx("Failed to create swapchain framebuffer " + i + " for PostProcess.");
                }
                this.swapchainFramebufferHandles[i] = pFb.get(0);
            }
        }
    }

    private void createPipeline(Swapchain swapchain) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            byte[] vertShaderCode = readShaderFile("/shaders/postprocess.vert.spv");
            byte[] fragShaderCode = readShaderFile("/shaders/postprocess.frag.spv");

            long vertShaderModule = createShaderModule(device, vertShaderCode);
            long fragShaderModule = createShaderModule(device, fragShaderCode);

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            shaderStages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertShaderModule)
                    .pName(stack.UTF8("main"));

            shaderStages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragShaderModule)
                    .pName(stack.UTF8("main"));

            // No vertex inputs needed — vertices are generated directly in vertex shader
            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .lineWidth(1.0f)
                    .cullMode(VK_CULL_MODE_NONE)
                    .frontFace(VK_FRONT_FACE_CLOCKWISE);

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(false)
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT |
                    VK_COLOR_COMPONENT_G_BIT |
                    VK_COLOR_COMPONENT_B_BIT |
                    VK_COLOR_COMPONENT_A_BIT
            );
            colorBlendAttachment.blendEnable(false);

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .pAttachments(colorBlendAttachment);

            // Push Constants (18 floats = 72 bytes)
            VkPushConstantRange.Buffer pushConstants = VkPushConstantRange.calloc(1, stack);
            pushConstants.get(0)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .offset(0)
                    .size(18 * Float.BYTES);

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descriptorSetLayoutHandle))
                    .pPushConstantRanges(pushConstants);

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new EngineEx("Failed to create PostProcess Pipeline Layout.");
            }
            this.pipelineLayoutHandle = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(shaderStages)
                    .pVertexInputState(vertexInputInfo)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState)
                    .pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling)
                    .pColorBlendState(colorBlending)
                    .pDynamicState(dynamicState)
                    .layout(pipelineLayoutHandle)
                    .renderPass(swapchainRenderPassHandle)
                    .subpass(0);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new EngineEx("Failed to create PostProcess Graphics Pipeline.");
            }
            this.pipelineHandle = pGraphicsPipeline.get(0);

            vkDestroyShaderModule(device, vertShaderModule, null);
            vkDestroyShaderModule(device, fragShaderModule, null);
        }
    }

    public void recreateSwapchain(Swapchain swapchain, InternalRenderTarget renderTarget) {
        if (swapchainFramebufferHandles != null) {
            for (long fb : swapchainFramebufferHandles) {
                vkDestroyFramebuffer(device, fb, null);
            }
            swapchainFramebufferHandles = null;
        }
        if (swapchainRenderPassHandle != 0) {
            vkDestroyRenderPass(device, swapchainRenderPassHandle, null);
            swapchainRenderPassHandle = 0;
        }

        createSwapchainRenderPass(swapchain);
        createSwapchainFramebuffers(swapchain);
        updateDescriptorSet(renderTarget);
    }

    public long getPipelineHandle() {
        return pipelineHandle;
    }

    public long getPipelineLayoutHandle() {
        return pipelineLayoutHandle;
    }

    public long getDescriptorSetHandle() {
        return descriptorSetHandle;
    }

    public long getSwapchainRenderPassHandle() {
        return swapchainRenderPassHandle;
    }

    public long getFramebuffer(int imageIndex) {
        return swapchainFramebufferHandles[imageIndex];
    }

    private byte[] readShaderFile(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new EngineEx("Unable to find compiled shader file in: " + resourcePath +
                        "\nCheck that the .spv files are correctly placed in src/main/resources/shaders/");
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new EngineEx("Error reading shader: " + resourcePath + " specific: " + e);
        }
    }

    private long createShaderModule(VkDevice device, byte[] shaderCode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pCode = stack.malloc(shaderCode.length);
            pCode.put(shaderCode);
            pCode.flip();

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(pCode);

            LongBuffer pShaderModule = stack.mallocLong(1);
            if (vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new EngineEx("Unable to create native Shader Module.");
            }
            return pShaderModule.get(0);
        }
    }

    @Override
    public void close() {
        if (swapchainFramebufferHandles != null) {
            for (long fb : swapchainFramebufferHandles) {
                vkDestroyFramebuffer(device, fb, null);
            }
            swapchainFramebufferHandles = null;
        }

        if (pipelineHandle != 0) {
            vkDestroyPipeline(device, pipelineHandle, null);
            pipelineHandle = 0;
        }

        if (pipelineLayoutHandle != 0) {
            vkDestroyPipelineLayout(device, pipelineLayoutHandle, null);
            pipelineLayoutHandle = 0;
        }

        if (swapchainRenderPassHandle != 0) {
            vkDestroyRenderPass(device, swapchainRenderPassHandle, null);
            swapchainRenderPassHandle = 0;
        }

        if (descriptorPoolHandle != 0) {
            vkDestroyDescriptorPool(device, descriptorPoolHandle, null);
            descriptorPoolHandle = 0;
        }

        if (descriptorSetLayoutHandle != 0) {
            vkDestroyDescriptorSetLayout(device, descriptorSetLayoutHandle, null);
            descriptorSetLayoutHandle = 0;
        }
    }
}
