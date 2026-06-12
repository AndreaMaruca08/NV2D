package nv.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline implements AutoCloseable {

    private final VkDevice device;
    private final long pipelineLayoutHandle;
    private final long pipelineHandle;

    public GraphicsPipeline(VkDevice device, Swapchain swapchain, long renderPass, long descriptorSetLayout) {
        this.device = device;

        long tempLayout;
        long tempPipeline;

        try (MemoryStack stack = MemoryStack.stackPush()) {

            // 0. Shader: lettura SPIR‑V e creazione shader modules
            byte[] vertShaderCode = readShaderFile("/shaders/shader.vert.spv");
            byte[] fragShaderCode = readShaderFile("/shaders/shader.frag.spv");

            long vertShaderModule = createShaderModule(device, vertShaderCode);
            long fragShaderModule = createShaderModule(device, fragShaderCode);

            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.calloc(2, stack);

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


            // 1. Vertex Input (7 FLOAT STRIDE: vec2 pos + vec3 color + vec2 UV)
            VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.calloc(1, stack);
            bindingDescription.binding(0);
            bindingDescription.stride(7 * Float.BYTES); // 2 pos + 3 color + 2 uv = 7 float
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                    VkVertexInputAttributeDescription.calloc(3, stack);

            // location 0: vec2 posizione
            attributeDescriptions.get(0)
                    .binding(0)
                    .location(0)
                    .format(VK_FORMAT_R32G32_SFLOAT)
                    .offset(0);

            // location 1: vec3 colore (Ritorna a vec3 per combaciare con lo shader SPIR-V)
            attributeDescriptions.get(1)
                    .binding(0)
                    .location(1)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(2 * Float.BYTES);

            // location 2: vec2 inUV
            attributeDescriptions.get(2)
                    .binding(0)
                    .location(2)
                    .format(VK_FORMAT_R32G32_SFLOAT)
                    .offset(5 * Float.BYTES); // Inizia dopo i 2 float di pos e i 3 di colore

            VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                    VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(bindingDescription);
            vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions);

            // 2. Input Assembly
            VkPipelineInputAssemblyStateCreateInfo inputAssembly =
                    VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // 3. Viewport & Scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) swapchain.getWidth());
            viewport.height((float) swapchain.getHeight());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(swapchain.getWidth(), swapchain.getHeight());

            VkPipelineViewportStateCreateInfo viewportState =
                    VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            // 4. Rasterizer
            VkPipelineRasterizationStateCreateInfo rasterizer =
                    VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK_CULL_MODE_NONE);
            rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);

            // 5. Multisampling
            VkPipelineMultisampleStateCreateInfo multisampling =
                    VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // 6. Color Blending (ATTIVATO PER TRASPARENZA DEL TESTO)
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT |
                            VK_COLOR_COMPONENT_G_BIT |
                            VK_COLOR_COMPONENT_B_BIT |
                            VK_COLOR_COMPONENT_A_BIT
            );
            // Setup del blending Alpha standard (fondamentale per i Font)
            colorBlendAttachment.blendEnable(true);
            colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
            colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
            colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
            colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
            colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
            colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);

            VkPipelineColorBlendStateCreateInfo colorBlending =
                    VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.pAttachments(colorBlendAttachment);

            // 7. Pipeline Layout
            VkPipelineLayoutCreateInfo pipelineLayoutInfo =
                    VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            // colleghiamo il descriptorSetLayout (set = 0, binding = 0 UBO)
            LongBuffer pSetLayouts = stack.longs(descriptorSetLayout);
            pipelineLayoutInfo.pSetLayouts(pSetLayouts);

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare il Pipeline Layout");
            }
            tempLayout = pPipelineLayout.get(0);

            // 8. Graphics Pipeline
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo =
                    VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(tempLayout);      // vero VkPipelineLayout
            pipelineInfo.renderPass(renderPass);
            pipelineInfo.subpass(0);

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);
            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare la Graphics Pipeline");
            }
            tempPipeline = pGraphicsPipeline.get(0);

            // shader module non più necessari dopo la creazione della pipeline
            vkDestroyShaderModule(device, vertShaderModule, null);
            vkDestroyShaderModule(device, fragShaderModule, null);
        }

        this.pipelineLayoutHandle = tempLayout;
        this.pipelineHandle = tempPipeline;
    }

    public long getPipelineLayoutHandle() {
        return pipelineLayoutHandle;
    }

    private byte[] readShaderFile(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Impossibile trovare il file shader compilato in: " + resourcePath +
                        "\nVerifica che i file .spv siano posizionati correttamente in src/main/resources/shaders/");
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Errore durante la lettura dello shader: " + resourcePath, e);
        }
    }

    private long createShaderModule(VkDevice device, byte[] shaderCode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pCode = stack.malloc(shaderCode.length);
            pCode.put(shaderCode);
            pCode.flip();

            VkShaderModuleCreateInfo createInfo =
                    VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(pCode);

            LongBuffer pShaderModule = stack.mallocLong(1);
            if (vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Impossibile creare lo Shader Module nativo.");
            }

            return pShaderModule.get(0);
        }
    }

    public long getHandle() {
        return pipelineHandle;
    }

    @Override
    public void close() {
        vkDestroyPipeline(device, pipelineHandle, null);
        vkDestroyPipelineLayout(device, pipelineLayoutHandle, null);
    }
}