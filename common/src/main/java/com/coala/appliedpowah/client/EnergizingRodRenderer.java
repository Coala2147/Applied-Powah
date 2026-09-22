package com.coala.appliedpowah.client;

import com.coala.appliedpowah.AppliedPowah;
import com.coala.appliedpowah.chargingrod.EnergizingRodBlockEntity;
import com.coala.appliedpowah.chargingrod.RodTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Beam from rod toward Powah orb while transferring energy (visual only; charge still works without it).
 */
public class EnergizingRodRenderer implements BlockEntityRenderer<EnergizingRodBlockEntity> {

    private static final ResourceLocation BEAM =
            new ResourceLocation(AppliedPowah.MOD_ID, "textures/model/beam.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(BEAM);

    public EnergizingRodRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(EnergizingRodBlockEntity be, float pt, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockPos orbPos = be.getOrbPos();
        if (orbPos == null || orbPos.equals(BlockPos.ZERO)) {
            return;
        }
        if (be.getLevel() == null) {
            return;
        }

        // Beam when we have buffer (charging pipeline active) — mirrors Powah showing link while working
        boolean show = be.getBufferFe() > 0 || be.isBeaming();
        // Also show beam when player holds Powah wrench in link mode (visualise linkability)
        if (!show) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var player = mc.player;
            if (player != null) {
                for (var hand : net.minecraft.world.InteractionHand.values()) {
                    var stack = player.getItemInHand(hand);
                    if (isPowahWrenchLink(stack)) {
                        show = true;
                        break;
                    }
                }
            }
        }
        if (!show) {
            return;
        }

        Vec3 rod = Vec3.atCenterOf(be.getBlockPos());
        Vec3 orb = Vec3.atCenterOf(orbPos);
        // Orb center is slightly above block center in Powah
        orb = orb.add(0, 0.1, 0);

        Vec3 delta = rod.subtract(orb);
        double len = delta.length();
        if (len < 0.01 || len > 32) {
            return;
        }
        Vec3 dir = delta.normalize();
        float yaw = (float) ((Math.PI / 2F - Mth.atan2((float) dir.z, (float) dir.x)) * (180F / Math.PI));
        float pitch = (float) (Math.acos(Mth.clamp(dir.y, -1.0, 1.0)) * (180F / Math.PI));

        int color = tierColor(be);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));

        float half = 0.10F;
        float v0 = 0f;
        float v1 = (float) (len * 5.0);
        VertexConsumer buf = buffers.getBuffer(TYPE);
        Matrix4f poseM = pose.last().pose();
        Matrix3f normM = pose.last().normal();

        // Two crossed quads (rod → orb along -Y in this space)
        quad(buf, poseM, normM, -half, half, r, g, b, v0, v1);
        quad(buf, poseM, normM, 0, half, r, g, b, v0, v1);
        pose.popPose();
    }

    private static void quad(VertexConsumer buf, Matrix4f m, Matrix3f n,
                             float x0, float x1, int r, int g, int b, float v0, float v1) {
        float len = 0; // filled by caller via v scale; vertices use y=0 and y=-(v1 related)
        // Use explicit y extents from UV: caller sets v1 ≈ length*5; draw y from 0 to -length
        // We encode length as v1/5
        float yLen = v1 / 5.0F;
        vert(buf, m, n, x0, 0, 0, r, g, b, 0, v0);
        vert(buf, m, n, x0, -yLen, 0, r, g, b, 0, v1);
        vert(buf, m, n, x1, -yLen, 0, r, g, b, 1, v1);
        vert(buf, m, n, x1, 0, 0, r, g, b, 1, v0);
        // second orientation (crossed)
        vert(buf, m, n, 0, 0, x0, r, g, b, 0, v0);
        vert(buf, m, n, 0, -yLen, x0, r, g, b, 0, v1);
        vert(buf, m, n, 0, -yLen, x1, r, g, b, 1, v1);
        vert(buf, m, n, 0, 0, x1, r, g, b, 1, v0);
    }

    private static void vert(VertexConsumer buf, Matrix4f m, Matrix3f n,
                             float x, float y, float z, int r, int g, int b, float u, float v) {
        buf.vertex(m, x, y, z).color(r, g, b, 255).uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(n, 0, 1, 0)
                .endVertex();
    }

    /** Safe check for Powah wrench in LINK mode (no hard dep on Powah classes). */
    private static boolean isPowahWrenchLink(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        var tag = stack.getTagElement("PowahWrenchNBT");
        return tag != null && tag.getInt("WrenchMode") == 1; // 1 = LINK
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
