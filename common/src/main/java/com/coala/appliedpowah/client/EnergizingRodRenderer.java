package com.coala.appliedpowah.client;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlockEntity;
import com.coala.appliedpowah.chargingrod.RodTier;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Beam from rod toward orb — mirrors Powah's EnergizingRodRenderer logic.
 * Uses blended-no-depth RenderType and textured quad strip for the beam.
 */
public class EnergizingRodRenderer implements BlockEntityRenderer<EnergizingRodBlockEntity> {

    public static final ResourceLocation BEAM_TEXTURE =
            new ResourceLocation(AppliedPowah.MOD_ID, "textures/model/beam.png");
    private static final RenderType BEAM_TYPE = makeBeamType(BEAM_TEXTURE);

    public EnergizingRodRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(EnergizingRodBlockEntity be, float pt, PoseStack matrix,
                       MultiBufferSource rtb, int light, int overlay) {
        BlockPos orbPos = be.getOrbPos();
        if (orbPos == null || orbPos.equals(BlockPos.ZERO)) {
            return;
        }
        if (be.getLevel() == null) {
            return;
        }

        // Beam is ONLY visible when the player holds a Powah wrench in LINK mode.
        boolean show = false;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player != null) {
            for (var hand : InteractionHand.values()) {
                var stack = player.getItemInHand(hand);
                if (isPowahWrenchLink(stack)) {
                    show = true;
                    break;
                }
            }
        }
        if (!show) {
            return;
        }

        matrix.pushPose();
        matrix.translate(0.5D, 0.5D, 0.5D);

        Vec3 pos = Vec3.atCenterOf(be.getBlockPos());
        Vec3 orb = Vec3.atCenterOf(orbPos).add(0, 0.1, 0);
        Vec3 vec3d2 = pos.subtract(orb);
        double d0 = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        float f5 = (float) Math.acos(Mth.clamp(vec3d2.y, -1.0, 1.0));
        float f6 = (float) Mth.atan2(vec3d2.z, vec3d2.x);

        matrix.mulPose(Axis.YP.rotationDegrees((((float) Math.PI / 2F) - f6) * (180F / (float) Math.PI)));
        matrix.mulPose(Axis.XP.rotationDegrees(f5 * (180F / (float) Math.PI)));

        float f2 = 1.0F;
        float f3 = f2 * 0.5F % 1.0F;
        float d1 = f2 * 0.0F;

        float d12 = Mth.cos((float) (d1 + Math.PI)) * 0.12F;
        float d13 = Mth.sin((float) (d1 + Math.PI)) * 0.12F;
        float d14 = Mth.cos(d1) * 0.12F;
        float d15 = Mth.sin(d1) * 0.12F;

        float d16 = Mth.cos((float) (d1 + (Math.PI / 2D))) * 0.12F;
        float d17 = Mth.sin((float) (d1 + (Math.PI / 2D))) * 0.12F;
        float d18 = Mth.cos((float) (d1 + (Math.PI * 1.5D))) * 0.12F;
        float d19 = Mth.sin((float) (d1 + (Math.PI * 1.5D))) * 0.12F;

        float d22 = (f3 - 1.0F);
        float d23 = (float) (d0 * 5.05D + d22);
        VertexConsumer builder = rtb.getBuffer(BEAM_TYPE);
        PoseStack.Pose last = matrix.last();
        Matrix4f matrix4f = last.pose();
        Matrix3f matrix3f = last.normal();

        int color = tierColor(be);
        int r = 0xFF & (color >> 16);
        int g = 0xFF & (color >> 8);
        int b = 0xFF & color;

        pos(builder, matrix4f, matrix3f, d12, 0.0F, d13, r, g, b, 1, d23);
        pos(builder, matrix4f, matrix3f, d12, (float) -d0, d13, r, g, b, 1, d22);
        pos(builder, matrix4f, matrix3f, d14, (float) -d0, d15, r, g, b, 0.0F, d22);
        pos(builder, matrix4f, matrix3f, d14, 0.0F, d15, r, g, b, 0.0F, d23);

        pos(builder, matrix4f, matrix3f, d16, 0.0F, d17, r, g, b, 1, d23);
        pos(builder, matrix4f, matrix3f, d16, (float) -d0, d17, r, g, b, 1, d22);
        pos(builder, matrix4f, matrix3f, d18, (float) -d0, d19, r, g, b, 0.0F, d22);
        pos(builder, matrix4f, matrix3f, d18, 0.0F, d19, r, g, b, 0.0F, d23);

        matrix.popPose();
    }

    private static void pos(VertexConsumer builder, Matrix4f matrix4f, Matrix3f matrix3f,
                            float x, float y, float z, int r, int g, int b, float u, float v) {
        builder.vertex(matrix4f, x, y, z).color(r, g, b, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880 / 2)
                .normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static RenderType makeBeamType(ResourceLocation location) {
        RenderStateShard.TransparencyStateShard blendedNoDept = new RenderStateShard.TransparencyStateShard("blended_no_dept",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.depthMask(false);
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                }, () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.depthMask(true);
                    RenderSystem.defaultBlendFunc();
                });

        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(location, false, false))
                .setTransparencyState(blendedNoDept)
                .setShaderState(ShaderAccess.POSITION_COLOR_TEX)
                .setCullState(CullAccess.NO_CULL)
                .setLightmapState(LightmapAccess.NO_LIGHTMAP)
                .createCompositeState(true);

        return RenderType.create("ap_beam", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, true, true, state);
    }

    // Small accessors to reach protected RenderType / RenderStateShard constants
    private static final class ShaderAccess extends RenderStateShard {
        static final ShaderStateShard POSITION_COLOR_TEX = POSITION_COLOR_TEX_SHADER;
        ShaderAccess() { super("access", () -> {}, () -> {}); }
    }
    private static final class CullAccess extends RenderType {
        static final CullStateShard NO_CULL = RenderType.NO_CULL;
        CullAccess() { super("access", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0, false, false, () -> {}, () -> {}); }
    }
    private static final class LightmapAccess extends RenderType {
        static final LightmapStateShard NO_LIGHTMAP = RenderType.NO_LIGHTMAP;
        LightmapAccess() { super("access", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 0, false, false, () -> {}, () -> {}); }
    }

    private static boolean isPowahWrenchLink(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var tag = stack.getTagElement("PowahWrenchNBT");
        return tag != null && tag.getInt("WrenchMode") == 1;
    }

    private static int tierColor(EnergizingRodBlockEntity be) {
        try {
            RodTier t = be.tier();
            return switch (t) {
                case STARTER -> 0xA7A7A7;
                case BASIC -> 0xA3AB9F;
                case HARDENED -> 0xBBA993;
                case BLAZING -> 0xE4B040;
                case NIOTIC -> 0x13EED2;
                case SPIRITED -> 0xAFE241;
                case NITRO -> 0xD7746C;
            };
        } catch (Throwable t) {
            return 0x88FFFF;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(EnergizingRodBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
