package com.chen1335.arcanumlib.modules.fishing.client;

import com.chen1335.arcanumlib.modules.fishing.SimpleFishingHook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

public class SimpleFishingHookRenderer extends EntityRenderer<SimpleFishingHook> {

    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");

    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);

    private static final double VIEW_BOBBING_SCALE = 960.0;

    private static final int LINE_SEGMENTS = 16;

    public SimpleFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(SimpleFishingHook entity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(entity, camera, camX, camY, camZ) && entity.getPlayerOwner() != null;
    }

    @Override
    public void render(SimpleFishingHook entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {

        Player player = entity.getPlayerOwner();
        if (player == null) {
            return;
        }

        poseStack.pushPose();

        poseStack.pushPose();

        poseStack.scale(0.5F, 0.5F, 0.5F);

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose bobberPose = poseStack.last();
        VertexConsumer bobberBuffer = buffer.getBuffer(RENDER_TYPE);

        vertex(bobberBuffer, bobberPose, packedLight, 0.0F, 0, 0, 1);
        vertex(bobberBuffer, bobberPose, packedLight, 1.0F, 0, 1, 1);
        vertex(bobberBuffer, bobberPose, packedLight, 1.0F, 1, 1, 0);
        vertex(bobberBuffer, bobberPose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        float attackAnim = player.getAttackAnim(partialTicks);
        float swing = Mth.sin(Mth.sqrt(attackAnim) * (float) Math.PI);
        Vec3 handPos = this.getPlayerHandPos(player, swing, partialTicks);

        Vec3 lineOrigin = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);

        float dx = (float) (handPos.x - lineOrigin.x);
        float dy = (float) (handPos.y - lineOrigin.y);
        float dz = (float) (handPos.z - lineOrigin.z);

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose linePose = poseStack.last();

        for (int i = 0; i <= LINE_SEGMENTS; ++i) {
            stringVertex(dx, dy, dz, lineBuffer, linePose, fraction(i, LINE_SEGMENTS), fraction(i + 1, LINE_SEGMENTS));
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private Vec3 getPlayerHandPos(Player player, float swing, float partialTick) {

        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        ItemStack mainHand = player.getMainHandItem();

        if (!mainHand.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
            side = -side;
        }

        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson()
                && player == Minecraft.getInstance().player) {

            double scale = VIEW_BOBBING_SCALE / (double) this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 offset = this.entityRenderDispatcher.camera.getNearPlane()
                    .getPointOnPlane((float) side * 0.525F, -0.1F)
                    .scale(scale)
                    .yRot(swing * 0.5F)
                    .xRot(-swing * 0.7F);
            return player.getEyePosition(partialTick).add(offset);
        }

        float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180.0F);
        double sin = Mth.sin(bodyRot);
        double cos = Mth.cos(bodyRot);
        float playerScale = player.getScale();
        double sideOffset = (double) side * 0.35 * (double) playerScale;
        double forwardOffset = 0.8 * (double) playerScale;

        float crouchOffset = player.isCrouching() ? -0.1875F : 0.0F;
        return player.getEyePosition(partialTick).add(
                -cos * sideOffset - sin * forwardOffset,
                (double) crouchOffset - 0.45 * (double) playerScale,
                -sin * sideOffset + cos * forwardOffset);
    }

    private static float fraction(int numerator, int denominator) {
        return (float) numerator / (float) denominator;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(float dx, float dy, float dz, VertexConsumer consumer,
                                     PoseStack.Pose pose, float stringFraction, float nextStringFraction) {
        float x = dx * stringFraction;
        float y = dy * (stringFraction * stringFraction + stringFraction) * 0.5F + 0.25F;
        float z = dz * stringFraction;

        float nx = dx * nextStringFraction - x;
        float ny = dy * (nextStringFraction * nextStringFraction + nextStringFraction) * 0.5F + 0.25F - y;
        float nz = dz * nextStringFraction - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);

        nx /= length;
        ny /= length;
        nz /= length;
        consumer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(SimpleFishingHook entity) {
        return TEXTURE_LOCATION;
    }
}
