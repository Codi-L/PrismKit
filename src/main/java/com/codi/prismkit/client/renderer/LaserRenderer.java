package com.codi.prismkit.client.renderer;

import com.codi.prismkit.PrismKit;
import com.codi.prismkit.entity.vfx.LaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static java.lang.Math.pow;

@OnlyIn(Dist.CLIENT)
public class LaserRenderer extends EntityRenderer<LaserEntity> {
    
    public LaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LaserEntity entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        
        double height = entity.getLaserHeight();
        int duration = entity.getDuration();
        int maxDuration = entity.getMaxDuration();
        
        // 计算归一化的生命进度（0.0 到 1.0）
        float normalizedAge = 1 - ((float) duration / (float) maxDuration);
        normalizedAge = Math.max(0.0f, Math.min(1.0f, normalizedAge)); // 确保在 [0, 1] 范围
        
        // 使用 PrismCurve 计算整体透明度（淡入效果）
        float alpha = PrismKit.getCurveValue("pulse", normalizedAge);

        poseStack.pushPose();
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lightning());
        
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        
        float width = 0.5f;
        float red = 1.0f;
        float green = 0.2f;
        float blue = 0.2f;
        
        float bottom = 0.0f;
        float top = (float) height;
        
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, bottom, width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, bottom, -width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, top, -width, red, green, blue, alpha, 15728880, 0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, top, width, red, green, blue, alpha, 15728880, 0f);
        
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, bottom, -width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, bottom, width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, top, width, red, green, blue, alpha, 15728880, 0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, top, -width, red, green, blue, alpha, 15728880, 0f);
        
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, bottom, width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, bottom, width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, top, width, red, green, blue, alpha, 15728880, 0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, top, width, red, green, blue, alpha, 15728880, 0f);
        
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, bottom, -width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, bottom, -width, red, green, blue, alpha, 15728880, 1.0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, -width, top, -width, red, green, blue, alpha, 15728880, 0f);
        addVertexWithGradient(vertexConsumer, matrix4f, matrix3f, width, top, -width, red, green, blue, alpha, 15728880, 0f);
        
        poseStack.popPose();
    }

    private void addVertexWithGradient(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                          float x, float y, float z, 
                          float red, float green, float blue, float baseAlpha,
                          int packedLight, float gradientFactor) {
        float finalAlpha = (float) (baseAlpha * pow(gradientFactor,10));
        
        consumer.vertex(matrix4f, x, y, z)
                .color(red, green, blue, finalAlpha)
                .uv(0, 0)
                .overlayCoords(0)
                .uv2(packedLight)
                .normal(matrix3f, 0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(LaserEntity entity) {
        return null;
    }
}
