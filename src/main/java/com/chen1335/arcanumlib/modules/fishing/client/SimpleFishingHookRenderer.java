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

/**
 * {@link SimpleFishingHook} 的客户端渲染器：把原版 1.21.1
 * {@code net.minecraft.client.renderer.entity.FishingHookRenderer} 的渲染流程整体照搬到模组的抽象鱼钩上。
 *
 * <p><b>关于"原版模型层"的说明（重要）</b>：原版 1.21.1 的 {@code ModelLayers} 里<b>没有</b>
 * {@code FISHING_BOBBER}，也没有 {@code FishingHookModel} 这个类（{@code ModelLayers} 中不存在任何
 * FISHING / BOBBER 条目，26.1.2 同样没有）。原版浮标从 1.8 一直到 26.x 的表现都是
 * "方片公告板"：把 {@code textures/entity/fishing_hook.png} 贴在一个始终面向摄像机的四边形上。
 * 因此这里<b>不烘焙任何模型</b>，而是与当前版本原版逐顶点一致地画那个四边形。
 * 凭空造一个模型层 / 模型类反而会与原版表现不一致，也违反"不要自造模型"的初衷。
 *
 * <p><b>为什么不用 {@code @EventBusSubscriber} 自己注册</b>：注册统一由客户端的
 * {@code EntityRenderersEvent.RegisterRenderers} 处理器完成
 * （{@code event.registerEntityRenderer(type, SimpleFishingHookRenderer::new)}）。
 * 本类只保证提供一个 {@link EntityRendererProvider.Context} 单参构造器。
 * 另外本类引用了大量客户端专属类型，<b>只能被客户端代码类加载</b>（服务端不要触碰本类）。
 *
 * <p>渲染的三段内容与原版一一对应：
 * <ol>
 *   <li>浮标本体：面向摄像机的四边形，缩放 1/2；</li>
 *   <li>钓线：从浮标本体 +0.25 高度连到钓手的手部位置，用 16 段折线模拟下垂弧度；</li>
 *   <li>{@code super.render}：原版的牵绳 / 名牌流程，直接沿用基类。</li>
 * </ol>
 */
public class SimpleFishingHookRenderer extends EntityRenderer<SimpleFishingHook> {

    /**
     * 与原版完全一致的贴图：16x16 的红白浮标 + 一枚鱼钩。
     * 用 {@code withDefaultNamespace} 而不是拼 {@code minecraft:} 字符串，避免写死命名空间。
     */
    private static final ResourceLocation TEXTURE_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/fishing_hook.png");

    /**
     * 浮标本体用 {@code entityCutout}（带透明剔除的实体贴图），与基类 {@link #getTextureLocation} 一致。
     * 缓存成静态常量是原版的做法：{@code RenderType} 是重对象，且渲染每帧都会取一次 buffer。
     */
    private static final RenderType RENDER_TYPE = RenderType.entityCutout(TEXTURE_LOCATION);

    /**
     * 第一人称下把手部位置推到"近平面之外"的缩放系数。
     * 值取自原版：{@code 960 / fov}，让钓线起点看起来是绑在屏幕右下/左下的竿稍上，而不是穿进摄像机。
     */
    private static final double VIEW_BOBBING_SCALE = 960.0;

    /**
     * 钓线的折线段数。原版写死 16：段数太少能看出折角，太多则纯属浪费顶点。
     */
    private static final int LINE_SEGMENTS = 16;

    public SimpleFishingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 拥有者已经消失（客户端被卸载 / 玩家下线 / 实体还没同步到 owner）时不必渲染。
     *
     * <p>原版 1.21.1 没写这个方法，只在 {@link #render} 里提前 return；这里补上是为了少走一遍
     * 视锥与实体 tick 检查 —— 效果与原版完全等价（缺了 owner 本来也画不出任何东西）。
     * 这段判断与新版原版（26.1.2）的 {@code FishingHookRenderer#shouldRender} 一致。
     */
    @Override
    public boolean shouldRender(SimpleFishingHook entity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(entity, camera, camX, camY, camZ) && entity.getPlayerOwner() != null;
    }

    @Override
    public void render(SimpleFishingHook entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // 没有钓手就没有钓线可画；原版这里直接放弃整个渲染（含名牌）
        Player player = entity.getPlayerOwner();
        if (player == null) {
            return;
        }

        // 外层 push：整个"浮标 + 钓线"都在实体自身坐标系里画。
        // EntityRenderDispatcher 在调用 render 之前已经把 poseStack 平移到实体位置，
        // 所以下面的坐标一律是"相对浮标"的局部分量（钓线之所以能用差值直接当局部坐标，就是因为这个）。
        poseStack.pushPose();

        // ---- 1. 浮标本体：面向摄像机的四边形 ----
        // 内层 push：这里的 scale / mulPose 只服务于本体，画完立刻弹出，免得污染钓线的坐标系
        poseStack.pushPose();
        // 贴图是 16x16 的像素图，缩到 0.5 恰好是原版浮标的视觉大小（一个约 8 像素宽的方块）
        poseStack.scale(0.5F, 0.5F, 0.5F);
        // 公告板：绕摄像机朝向旋转，所以无论从哪个角度看浮标都是一张正对我们的贴图
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose bobberPose = poseStack.last();
        VertexConsumer bobberBuffer = buffer.getBuffer(RENDER_TYPE);
        // 四个顶点：(x, y) 在 0..1 之间，并以 -0.5 居中；uv 按原版给的顺序贴，保证鱼钩朝向正确
        vertex(bobberBuffer, bobberPose, packedLight, 0.0F, 0, 0, 1);
        vertex(bobberBuffer, bobberPose, packedLight, 1.0F, 0, 1, 1);
        vertex(bobberBuffer, bobberPose, packedLight, 1.0F, 1, 1, 0);
        vertex(bobberBuffer, bobberPose, packedLight, 0.0F, 1, 0, 0);
        poseStack.popPose();

        // ---- 2. 钓线：浮标 -> 钓手手部 ----
        // 挥臂动画（甩竿 / 收竿）会让手部位置摆动，sin(sqrt(f)) 与原版一致地把它变成 0..1 的插值因子
        float attackAnim = player.getAttackAnim(partialTicks);
        float swing = Mth.sin(Mth.sqrt(attackAnim) * (float) Math.PI);
        Vec3 handPos = this.getPlayerHandPos(player, swing, partialTicks);
        // 线的起点不是浮标原点，而是往上抬 0.25 格 —— 正好是浮标贴图的中心（贴图实际只占四边形的上半部分）
        Vec3 lineOrigin = entity.getPosition(partialTicks).add(0.0, 0.25, 0.0);
        // 世界坐标差值 = 实体局部坐标（因为 poseStack 已经被平移到实体位置）
        float dx = (float) (handPos.x - lineOrigin.x);
        float dy = (float) (handPos.y - lineOrigin.y);
        float dz = (float) (handPos.z - lineOrigin.z);

        // 用 lineStrip 而不是 lines()：线带需要每个顶点的法线来确定线段朝向，
        // stringVertex 正是靠"下一个采样点 - 当前采样点"算出法线的；lines() 需要成对顶点，写法不同。
        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lineStrip());
        PoseStack.Pose linePose = poseStack.last();
        // LINE_STRIP 用相邻两点成段，所以 16 段需要 17 个顶点（t = 0/16 .. 16/16）；
        // 每个顶点的法线则由"下一个采样点"决定，因此循环里传的是 fraction(i) 与 fraction(i + 1)。
        // i 循环到 LINE_SEGMENTS 时 fraction(i+1) 会算到 17/16 —— 原版就是这么写的，多出的那一点只是提供法线，保持原样
        for (int i = 0; i <= LINE_SEGMENTS; ++i) {
            stringVertex(dx, dy, dz, lineBuffer, linePose, fraction(i, LINE_SEGMENTS), fraction(i + 1, LINE_SEGMENTS));
        }

        poseStack.popPose();

        // ---- 3. 基类流程：牵绳 + 名牌 ----
        // 原版同样在最后调用 super，保证鱼钩挂着拴绳 / 有自定义名字时行为与其它实体一致
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    /**
     * 算出钓手"手上握竿处"的世界坐标，即钓线的另一端。逐行对应原版的同名方法。
     *
     * @param swing       {@code sin(sqrt(getAttackAnim))}，挥臂动画的强度
     * @param partialTick 帧插值系数，用于取平滑后的眼睛位置 / 身体朝向
     */
    private Vec3 getPlayerHandPos(Player player, float swing, float partialTick) {
        // 主手是右手时向 +1 侧偏移，左手反之
        int side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        ItemStack mainHand = player.getMainHandItem();
        // 竿握在副手时左右翻转。用 NeoForge 的物品能力判定而不是硬编码 FishingRodItem，
        // 这样自定义鱼竿（本模组的 IFishingRod 物品）也能被识别
        if (!mainHand.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
            side = -side;
        }

        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson()
                && player == Minecraft.getInstance().player) {
            // 第一人称：手部模型在屏幕上被投影成"竿稍"，直接算世界坐标会穿模，
            // 于是取摄像机的近平面，在平面坐标 (横向 0.525, 纵向 -0.1) 处取点，
            // 再按 960 / fov 放大推到视野外，最后叠上挥臂产生的俯仰 / 偏航摆动。
            double scale = VIEW_BOBBING_SCALE / (double) this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 offset = this.entityRenderDispatcher.camera.getNearPlane()
                    .getPointOnPlane((float) side * 0.525F, -0.1F)
                    .scale(scale)
                    .yRot(swing * 0.5F)
                    .xRot(-swing * 0.7F);
            return player.getEyePosition(partialTick).add(offset);
        }

        // 第三人称 / 别的玩家：从眼睛位置出发，按身体朝向（yBodyRot 而不是头部 yRot，因为手挂在躯干上）
        // 向前 / 向侧偏移。所有偏移都乘玩家 scale，保证巨人 / 幼体等缩放体型也贴合。
        float bodyRot = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * ((float) Math.PI / 180.0F);
        double sin = Mth.sin(bodyRot);
        double cos = Mth.cos(bodyRot);
        float playerScale = player.getScale();
        double sideOffset = (double) side * 0.35 * (double) playerScale;
        double forwardOffset = 0.8 * (double) playerScale;
        // 潜行时身体压低，手的位置也要跟着降
        float crouchOffset = player.isCrouching() ? -0.1875F : 0.0F;
        return player.getEyePosition(partialTick).add(
                -cos * sideOffset - sin * forwardOffset,
                (double) crouchOffset - 0.45 * (double) playerScale,
                -sin * sideOffset + cos * forwardOffset);
    }

    private static float fraction(int numerator, int denominator) {
        return (float) numerator / (float) denominator;
    }

    /**
     * 浮标四边形的一个顶点。
     *
     * <p>{@code y}/{@code u}/{@code v} 沿用原版签名写成 int：它们只取 0 / 1，转 float 没有意义。
     * {@code setColor(-1)} 是纯白（0xFFFFFFFF），配合 {@code entityCutout} 的不透明贴图；
     * 法线写死 (0,1,0) 是原版的做法 —— 公告板本来就不需要正确光照方向。
     */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight,
                               float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, (float) y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * 钓线折线的一个顶点。
     *
     * <p>为什么不直接把起点和终点连成一条直线：原版要让线自然下垂，于是用抛物线
     * {@code y = y0 * (t² + t) / 2} 采样出 17 个点。{@code t = 0} 时 {@code y = 0.25}，
     * {@code t = 1} 时 {@code y = y0 + 0.25}，正好两端的抬升量与 {@link #render} 里的 {@code +0.25} 对应。
     *
     * <p>法线是相邻采样点的差分（即线段方向），归一化后交给 {@code setNormal}；
     * 颜色 {@code -16777216} 即 {@code 0xFF000000}，纯黑（原版钓线就是黑色的）。
     */
    private static void stringVertex(float dx, float dy, float dz, VertexConsumer consumer,
                                     PoseStack.Pose pose, float stringFraction, float nextStringFraction) {
        float x = dx * stringFraction;
        float y = dy * (stringFraction * stringFraction + stringFraction) * 0.5F + 0.25F;
        float z = dz * stringFraction;
        // 下一个采样点与当前点的差值，作为线段朝向
        float nx = dx * nextStringFraction - x;
        float ny = dy * (nextStringFraction * nextStringFraction + nextStringFraction) * 0.5F + 0.25F - y;
        float nz = dz * nextStringFraction - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        // 除以自身长度归一化；最后一个顶点会与自身相减得到零向量，
        // 原版同样会产生 NaN 法线，但该顶点只作为线段终点、不参与光照，因此无害，这里保持与原版一致
        nx /= length;
        ny /= length;
        nz /= length;
        consumer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx, ny, nz);
    }

    /**
     * 与原版一致：浮标永远用同一张贴图，不做变体、不看状态。
     *
     * <p>说明：原版 1.21.1 的 {@code FishingHookRenderer} 里<b>没有</b>"渲染含在水里的物品"这一步
     * （鱼钩下方的战利品要到收竿时才会被 {@code LootTable} 摇出来并生成 {@code ItemEntity}），
     * 模组基类 {@link SimpleFishingHook} 也没有同步任何"嘴里叼着的物品"字段，
     * 所以这里同样不画物品，避免与 {@code ItemEntity} 自身的渲染重复。
     * {@code getHookedIn()} 返回的 {@code ItemEntity} 由原版 {@code ItemEntityRenderer} 负责绘制。
     */
    @Override
    public ResourceLocation getTextureLocation(SimpleFishingHook entity) {
        return TEXTURE_LOCATION;
    }
}
