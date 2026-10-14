package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.IFishMediaType;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用鱼钩基类，行为对齐原版 {@link net.minecraft.world.entity.projectile.FishingHook}。
 *
 * <p>本类负责与"鱼钩"这个概念无关实现细节的全部流程：浮标物理（飞行 / 入水漂浮 / 挂在实体上）、
 * 咬钩状态机（等待 → 引鱼 → 咬钩 → 脱钩）、开阔水面判定、收竿结算（战利品、经验、耐久损耗）与同步数据。
 * "在哪钓、钓什么、怎么钓"交给子类：
 * <ul>
 *   <li>{@link #getFishMediaType()}：垂钓水体（水 / 岩浆 / 虚空……），直接决定 {@link #canFishAt}；</li>
 *   <li>{@link #isFloatingInFluid}：浮标是否算"浮"在该水体上（水有液面高度，虚空没有）；</li>
 *   <li>{@link #isOpenWaterFluid}：什么样的流体算"开阔水面"；</li>
 *   <li>{@link #getLootTable} / {@link #buildLootParams} / {@link #modifyLootParams} / {@link #rollLoot}：
 *       战利品表来源、参数组装与摇取方式；</li>
 *   <li>{@link #getMinLureTicks()} 等：等待 / 咬钩 / 脱钩时长；</li>
 *   <li>{@link #getBubbleParticle()} 等：气泡 / 钓鱼 / 水花粒子。</li>
 * </ul>
 *
 * <p>与模块其它部分的衔接（这里刻意不重复实现）：
 * <ul>
 *   <li>鱼竿与鱼饵保存在实体附件 {@code ArcanumAttachments.FISHING_HOOK_DATA} 上。抛竿时本类只负责把玩家
 *       手里的鱼竿写进附件；取鱼饵、消耗鱼饵、派发 {@code LivingGetFishBaitEvent} / {@code ModifyFishingHookEvent}
 *       由 {@code FishingEvents#EntityJoinLevelEvent} 在实体加入世界时统一处理；</li>
 *   <li>战利品参数用 {@code FishingHooks#buildLootParams} 组装，鱼饵与鱼竿的额外参数由
 *       {@code LootParams$BuilderMixin} 在 {@code create} 时注入（含 {@code FishingExtraLootContextParams.FISH_BAIT}）。</li>
 * </ul>
 */
public abstract class SimpleFishingHook extends Projectile implements IFishingHook {

    /**
     * 被钩住的实体 id + 1（0 表示没有），单机 / 联机都要靠它同步钩住状态。
     */
    private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY =
            SynchedEntityData.defineId(SimpleFishingHook.class, EntityDataSerializers.INT);

    /**
     * 是否已经咬钩（浮标下沉 + 玩家可以收竿上鱼）。
     */
    private static final EntityDataAccessor<Boolean> DATA_BITING =
            SynchedEntityData.defineId(SimpleFishingHook.class, EntityDataSerializers.BOOLEAN);

    /**
     * 只用于"咬钩下坠"的随机源。原版用它保证客户端与服务端用同一个种子算出同样的下坠速度，
     * 所以不能换成 {@code Entity#random}。
     */
    private final RandomSource syncronizedRandom = RandomSource.create();

    private boolean biting;
    private int outOfWaterTime;
    /**
     * 鱼钩落地后经过的刻数，超过 {@link #getMaxIdleTicks()} 就自然消失。
     */
    private int life;
    private int nibble;
    private float timeUntilLured;
    private float timeUntilHooked;
    private float fishAngle;
    private boolean openWater = true;

    @Nullable
    private Entity hookedIn;

    private FishHookState currentState = FishHookState.FLYING;

    /**
     * 对应接口的 fishSpeed：缩略"引鱼倒计时"，原版叫 lureSpeed（附魔的"饵钓"就是加它）。
     */
    private float fishSpeed;

    /**
     * 对应接口的 fishLuck：原版叫 luck，会加进战利品参数的幸运值（附魔"海之眷顾"就是加它）。
     */
    private float fishLuck;

    protected SimpleFishingHook(EntityType<? extends Projectile> entityType, Level level) {
        this(entityType, level, 0, 0);
    }

    /**
     * @param fishLuck  幸运值，见 {@link #setFishLuck}
     * @param fishSpeed 引鱼速度，见 {@link #setFishSpeed}
     */
    protected SimpleFishingHook(EntityType<? extends Projectile> entityType, Level level, float fishLuck, float fishSpeed) {
        super(entityType, level);
        this.noCulling = true;
        this.setFishLuck(fishLuck);
        this.setFishSpeed(fishSpeed);
    }

    /**
     * 抛竿构造：把玩家设为拥有者，并把鱼竿（主手优先，其次副手）记进附件。
     *
     * <p>注意 {@code level().isClientSide} 时不会记录鱼竿：客户端的实体是从网络包重建的，
     * 若在客户端也记录，{@code FishingEvents#EntityJoinLevelEvent} 会在客户端重跑一遍消耗鱼饵的流程。
     */
    protected SimpleFishingHook(EntityType<? extends Projectile> entityType, Player player, Level level, float fishLuck, float fishSpeed) {
        this(entityType, player, null, level, fishLuck, fishSpeed);
    }

    /**
     * 抛竿构造，并显式指定鱼竿（适用于鱼竿物品不实现 {@link IFishingRod}、无法从手上识别的场合）。
     *
     * @param rod 可为 null / 空，表示交给 {@link #findRodStack} 从玩家手上找
     */
    protected SimpleFishingHook(EntityType<? extends Projectile> entityType, Player player, @Nullable ItemStack rod,
                                Level level, float fishLuck, float fishSpeed) {
        this(entityType, level, fishLuck, fishSpeed);
        if (rod != null && !rod.isEmpty()) {
            this.storeRod(rod);
        }
        this.setOwner(player);
        this.castFrom(player);
    }

    // ------------------------------------------------------------------
    // 同步数据
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_HOOKED_ENTITY, 0);
        builder.define(DATA_BITING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_HOOKED_ENTITY.equals(key)) {
            int id = this.getEntityData().get(DATA_HOOKED_ENTITY);
            this.hookedIn = id > 0 ? this.level().getEntity(id - 1) : null;
        }
        if (DATA_BITING.equals(key)) {
            this.biting = this.getEntityData().get(DATA_BITING);
            if (this.biting) {
                // 咬钩瞬间浮标被拽下去一点；种子固定，两端表现一致
                this.setDeltaMovement(this.getDeltaMovement().x, -0.4F * Mth.nextFloat(this.syncronizedRandom, 0.6F, 1.0F), this.getDeltaMovement().z);
            }
        }
        super.onSyncedDataUpdated(key);
    }

    // ------------------------------------------------------------------
    // 接口实现
    // ------------------------------------------------------------------


    @Override
    public void setFishSpeed(float fishSpeed) {
        this.fishSpeed = Math.max(0.0F, fishSpeed);
    }

    @Override
    public float getFishSpeed() {
        return this.fishSpeed;
    }

    @Override
    public void setFishLuck(float fishLuck) {
        this.fishLuck = Math.max(0.0F, fishLuck);
    }

    @Override
    public float getFishLuck() {
        return this.fishLuck;
    }

    // ------------------------------------------------------------------
    // 供子类定制的钩子
    // ------------------------------------------------------------------

    /**
     * 本鱼钩能钓到什么水体里。默认水；岩浆 / 虚空垂钓请返回 {@link FishMediaType#LAVA} / {@link FishMediaType#VOID}，
     */
    @Override
    public IFishMediaType getFishMediaType() {
        return FishMediaType.WATER;
    }

    /**
     * 浮标是否算"浮"在该流体上（决定 {@link FishHookState#FLYING} → {@link FishHookState#BOBBING} 的切换、
     * 以及是否触发咬钩状态机）。默认沿用原版判据：液面高度 &gt; 0。
     *
     * @param fluidHeight 该位置流体的高度（不在可垂钓水体中时调用方传 0）
     */
    protected boolean isFloatingInFluid(FluidState fluidState, float fluidHeight) {
        return getFishMediaType().isFloatingInFluid(fluidState, fluidHeight);
    }

    /**
     * 什么样的流体算"开阔水面"的一部分（原版要求是水且为水源）。换水体类型时一并重写。
     */
    protected boolean isOpenWaterFluid(BlockPos pos, FluidState fluidState) {
        return getFishMediaType().isOpenWaterFluid(pos, fluidState);
    }

    /**
     * 开水面判定里"水面之下的方块"用的是不是本鱼钩的流体（原版这里写死 {@code Blocks.WATER}）。
     */
    protected boolean isFishingFluidBlock(BlockPos pos, BlockState state) {
        return getFishMediaType().isFishingFluidBlock(this, pos, state);
    }

    /**
     * 每刻的进度倍率：值越大，引鱼 / 咬钩倒计时减得越快。基础 1，**下雨 +1，看不见天空 -1**，都是确定性的（本类不再像原版那样掷骰：原版雨天只有 25% 概率 +1、被遮挡只有 50% 概率 -1）。
     * 返回值可用小数：0.5 = 每刻只减一半（等待翻倍），1.5 = 快一半。
     * 注意 {@code 1 + [下雨] - [看不见天空]} 在下雨且被遮挡时得 1、**不下雨且被遮挡时为 0**（当刻不推进，地下/室内要有别的加成才推进）。
     */
    protected float getLureRate(BlockPos posAbove) {
        float rate = 1.0F;
        if (this.level().isRainingAt(posAbove)) {
            rate += 0.25F;
        }
        if (!this.level().canSeeSky(posAbove)) {
            rate -= 0.5F;
        }
        return rate;
    }

    /**
     * 第一次引鱼的等待区间（原版 100~600 刻）。
     */
    protected int getMinLureTicks() {
        return 100;
    }

    /**
     * 第一次引鱼的等待区间（原版 100~600 刻）。
     */
    protected int getMaxLureTicks() {
        return 600;
    }

    /**
     * 引鱼完成到咬钩的区间（原版 20~80 刻）。
     */
    protected int getMinHookedTicks() {
        return 20;
    }

    /**
     * 引鱼完成到咬钩的区间（原版 20~80 刻）。
     */
    protected int getMaxHookedTicks() {
        return 80;
    }

    /**
     * 咬钩后允许玩家反应的区间（原版 20~40 刻，超时鱼跑掉）。
     */
    protected int getMinNibbleTicks() {
        return 20;
    }

    /**
     * 咬钩后允许玩家反应的区间（原版 20~40 刻，超时鱼跑掉）。
     */
    protected int getMaxNibbleTicks() {
        return 40;
    }

    /**
     * 连续多少刻不在水体中就不再算开阔水面（原版 10）。
     */
    protected int getMaxOutOfWaterTicks() {
        return 10;
    }

    /**
     * 鱼钩卡在地面后多少刻自动消失（原版 1200）。
     */
    protected int getMaxIdleTicks() {
        return 1200;
    }

    /**
     * 与拥有者超过这个距离平方就自动收竿（原版 1024，即 32 格）。
     */
    protected double getMaxOwnerDistanceSqr() {
        return 1024.0;
    }

    /**
     * 每钓上一件物品给的经验（原版 {@code random.nextInt(6) + 1}）。
     */
    protected int getExperiencePerCatch() {
        return this.random.nextInt(6) + 1;
    }

    /**
     * 该物品是否计入 {@code Stats.FISH_CAUGHT}（原版只算鱼类标签）。
     */
    protected boolean countsAsFish(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    /**
     * 摇取战利品。默认走原版 {@link LootTable#getRandomItems(LootParams)}；
     * 需要按水体 / 鱼饵 / 时间改写掉落时重写本方法。
     */
    protected List<ItemStack> rollLoot(ServerLevel level, LootTable lootTable, LootParams params,
                                       Player player, ItemStack rod, ItemStack bait) {
        return lootTable.getRandomItems(params);
    }

    /**
     * 把摇到的战利品丢给玩家：原版是"生成 {@link ItemEntity} 朝玩家飞 + 每件物品一颗经验球 + 鱼类计数"。
     */
    protected void spawnLoot(Player player, ItemStack drop) {
        ItemEntity itemEntity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), drop);
        double dx = player.getX() - this.getX();
        double dy = player.getY() - this.getY();
        double dz = player.getZ() - this.getZ();
        itemEntity.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        this.level().addFreshEntity(itemEntity);
        player.level().addFreshEntity(new ExperienceOrb(player.level(), player.getX(), player.getY() + 0.5, player.getZ() + 0.5, this.getExperiencePerCatch()));
        if (this.countsAsFish(drop)) {
            player.awardStat(Stats.FISH_CAUGHT, 1);
        }
    }

    /**
     * 收竿时是否再处理一次鱼饵。
     *
     * <p>默认什么都不做：本模块的 {@code FishingEvents#EntityJoinLevelEvent} 已经在抛竿（实体进入世界）时
     * 通过 {@link IFishingRod#consumeBait} 消耗过鱼饵，这里再扣一次会重复消耗。
     * 想让鱼饵"收竿时才消耗"的子类可以重写本方法，并让抛竿阶段跳过消耗。
     */
    protected void consumeBaitOnRetrieve(Player player, ItemStack rod, ItemStack bait) {
    }

    /**
     * 是否停止钓鱼（玩家死 / 收起了鱼竿 / 拉得太远）。返回 true 时已经 {@code discard} 掉自己。
     */
    protected boolean shouldStopFishing(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean mainCanCast = mainHand.canPerformAction(ItemAbilities.FISHING_ROD_CAST);
        boolean offCanCast = offHand.canPerformAction(ItemAbilities.FISHING_ROD_CAST);
        if (!player.isRemoved() && player.isAlive() && (mainCanCast || offCanCast) && !(this.distanceToSqr(player) > this.getMaxOwnerDistanceSqr())) {
            return false;
        }
        this.discard();
        return true;
    }

    // ------------------------------------------------------------------
    // 附件读写
    // ------------------------------------------------------------------

    /**
     * 附件里记录的鱼竿（空表示还没记录）。
     */
    protected ItemStack getRodStack() {
        return this.getData(ArcanumAttachments.FISHING_HOOK_DATA).getRod();
    }

    /**
     * 附件里记录的鱼饵（抛竿时由 {@code FishingEvents} 写入）。
     */
    protected ItemStack getBaitStack() {
        return this.getData(ArcanumAttachments.FISHING_HOOK_DATA).getBait();
    }

    /**
     * 记录鱼竿到附件；{@code FishingEvents} 会据此找鱼饵并派发两个事件。
     */
    protected void storeRod(ItemStack rod) {
        this.getData(ArcanumAttachments.FISHING_HOOK_DATA).setRod(rod);
    }

    /**
     * 从玩家手上找鱼竿：主手优先，其次副手（与 FishingHookMixin 对原版鱼钩的做法一致）。
     */
    protected ItemStack findRodStack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof IFishingRod) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return offHand.getItem() instanceof IFishingRod ? offHand : ItemStack.EMPTY;
    }

    /**
     * 收竿结算用哪根鱼竿：附件里记录的优先，没有记录时退回调用方传进来的。
     */
    protected ItemStack resolveRodStack(ItemStack fallback) {
        ItemStack stored = this.getRodStack();
        return stored.isEmpty() ? fallback : stored;
    }

    // ------------------------------------------------------------------
    // 生命周期
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        // 固定种子，让客户端与服务端的咬钩动画一致
        this.syncronizedRandom.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level().getGameTime());
        super.tick();
        Player player = this.getPlayerOwner();
        if (player == null) {
            this.discard();
        } else if (this.level().isClientSide || !this.shouldStopFishing(player)) {
            if (this.onGround()) {
                ++this.life;
                if (this.life >= this.getMaxIdleTicks()) {
                    this.discard();
                    return;
                }
            } else {
                this.life = 0;
            }

            BlockPos blockPos = this.blockPosition();
            FluidState fluidState = this.level().getFluidState(blockPos);
            boolean inMedium = this.canFishAt(this.level(), player, blockPos, fluidState);
            float fluidHeight = inMedium ? fluidState.getHeight(this.level(), blockPos) : 0.0F;
            boolean floating = inMedium && this.isFloatingInFluid(fluidState, fluidHeight);

            if (this.currentState == FishHookState.FLYING) {
                if (this.hookedIn != null) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.currentState = FishHookState.HOOKED_IN_ENTITY;
                    return;
                }
                if (floating) {
                    // 入水：速度骤减，进入漂浮状态
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.2, 0.3));
                    this.currentState = FishHookState.BOBBING;
                    return;
                }
                this.checkCollision();
            } else {
                if (this.currentState == FishHookState.HOOKED_IN_ENTITY) {
                    if (this.hookedIn != null) {
                        if (!this.hookedIn.isRemoved() && this.hookedIn.level().dimension() == this.level().dimension()) {
                            this.setPos(this.hookedIn.getX(), this.hookedIn.getY(0.8), this.hookedIn.getZ());
                        } else {
                            this.setHookedEntity(null);
                            this.currentState = FishHookState.FLYING;
                        }
                    }
                    return;
                }
                if (this.currentState == FishHookState.BOBBING) {
                    Vec3 movement = this.getDeltaMovement();
                    // 让浮标稳定浮在液面：朝液面方向做阻尼修正
                    double offset = this.getY() + movement.y - (double) blockPos.getY() - (double) fluidHeight;
                    if (Math.abs(offset) < 0.01) {
                        offset += Math.signum(offset) * 0.1;
                    }
                    this.setDeltaMovement(movement.x * 0.9, movement.y - offset * (double) this.random.nextFloat() * 0.2, movement.z * 0.9);
                    if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                        this.openWater = true;
                    } else {
                        this.openWater = this.openWater && this.outOfWaterTime < this.getMaxOutOfWaterTicks() && this.calculateOpenWater(blockPos);
                    }
                    if (inMedium) {
                        this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                        if (this.biting) {
                            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.1 * (double) this.syncronizedRandom.nextFloat() * (double) this.syncronizedRandom.nextFloat(), 0.0));
                        }
                        if (!this.level().isClientSide) {
                            this.catchingFish(blockPos);
                        }
                    } else {
                        this.outOfWaterTime = Math.min(this.getMaxOutOfWaterTicks(), this.outOfWaterTime + 1);
                    }
                }
            }
            if (!inMedium) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.updateRotation();
            if (this.currentState == FishHookState.FLYING && (this.onGround() || this.horizontalCollision)) {
                this.setDeltaMovement(Vec3.ZERO);
            }
            double friction = 0.92;
            this.setDeltaMovement(this.getDeltaMovement().scale(friction));
            this.reapplyPosition();
        }
    }

    /**
     * 原版鱼钩刻意不存档（读档也不读任何字段），区块卸载即消失；需要持久化的子类请重写并调用 {@code super} 以保留 Owner。
     */
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        // 只在服务端记录鱼竿：客户端的钩子由网络包重建，记录后会重复走一遍消耗鱼饵的流程
        if (!this.level().isClientSide && owner instanceof Player player && this.getRodStack().isEmpty()) {
            ItemStack rod = this.findRodStack(player);
            if (!rod.isEmpty()) {
                this.storeRod(rod);
            }
        }
    }

    // ------------------------------------------------------------------
    // 抛竿与收竿
    // ------------------------------------------------------------------

    /**
     * 抛竿：把实体放到玩家眼睛前方，并按视线方向给一个带随机散布的初速度。
     */
    protected void castFrom(Player player) {
        float xRot = player.getXRot();
        float yRot = player.getYRot();
        float f2 = Mth.cos(-yRot * ((float) Math.PI / 180.0F) - (float) Math.PI);
        float f3 = Mth.sin(-yRot * ((float) Math.PI / 180.0F) - (float) Math.PI);
        float f4 = -Mth.cos(-xRot * ((float) Math.PI / 180.0F));
        float f5 = Mth.sin(-xRot * ((float) Math.PI / 180.0F));
        double x = player.getX() - (double) f3 * 0.3;
        double y = player.getEyeY();
        double z = player.getZ() - (double) f2 * 0.3;
        this.moveTo(x, y, z, yRot, xRot);
        Vec3 movement = new Vec3(-f3, Mth.clamp(-(f5 / f4), -5.0F, 5.0F), -f2);
        double length = movement.length();
        movement = movement.multiply(
                0.6 / length + this.random.triangle(0.5, 0.0103365),
                0.6 / length + this.random.triangle(0.5, 0.0103365),
                0.6 / length + this.random.triangle(0.5, 0.0103365));
        this.setDeltaMovement(movement);
        this.setYRot((float) (Mth.atan2(movement.x, movement.z) * 180.0 / Math.PI));
        this.setXRot((float) (Mth.atan2(movement.y, movement.horizontalDistance()) * 180.0 / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    /**
     * 收竿结算，语义与原版完全一致：返回应当对鱼竿造成的耐久损耗（0 表示不该收竿）。
     *
     * @param rod 调用方手上的鱼竿；附件里记录过鱼竿时以附件为准
     */
    public int retrieve(ItemStack rod) {
        Player player = this.getPlayerOwner();
        if (this.level().isClientSide || player == null || this.shouldStopFishing(player)) {
            return 0;
        }
        ItemStack rodStack = this.resolveRodStack(rod);
        ItemStack bait = this.getBaitStack();
        int rodDamage = 0;
        if (this.hookedIn != null) {
            this.pullEntity(this.hookedIn);
            // 让客户端把被钩住的玩家往拥有者方向拉
            this.level().broadcastEntityEvent(this, (byte) 31);
            rodDamage = this.hookedIn instanceof ItemEntity ? 3 : 5;
        } else if (this.nibble > 0) {
            this.consumeBaitOnRetrieve(player, rodStack, bait);
            rodDamage = this.settleCatch(player, rodStack, bait);
        }
        if (this.onGround()) {
            rodDamage = 2;
        }
        this.discard();
        return rodDamage;
    }

    /**
     * 摇取并发放战利品，返回耐久损耗（原版钓上鱼是 1）。
     */
    protected int settleCatch(Player player, ItemStack rod, ItemStack bait) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        BlockPos blockPos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(blockPos);
        LootTable lootTable = this.getLootTable(serverLevel, blockPos, fluidState);
        LootParams params = this.buildLootParams(player, rod, bait);
        for (ItemStack drop : this.rollLoot(serverLevel, lootTable, params, player, rod, bait)) {
            this.spawnLoot(player, drop);
        }
        return 1;
    }

    /**
     * 把被钩住的实体往拥有者方向拽（原版同款）。
     */
    protected void pullEntity(Entity entity) {
        Entity owner = this.getOwner();
        if (owner != null) {
            Vec3 pull = new Vec3(owner.getX() - this.getX(), owner.getY() - this.getY(), owner.getZ() - this.getZ()).scale(0.1);
            entity.setDeltaMovement(entity.getDeltaMovement().add(pull));
        }
    }

    // ------------------------------------------------------------------
    // 咬钩状态机
    // ------------------------------------------------------------------

    /**
     * 只有在服务端调用。倒计时推进 + 粒子表现：等待 → 引鱼 → 咬钩 → 脱钩。
     */
    protected void catchingFish(BlockPos pos) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        float rate = this.getLureRate(pos.above());
        if (this.nibble > 0) {
            --this.nibble;
            if (this.nibble <= 0) {
                // 玩家没来得及收竿，鱼跑了
                this.timeUntilLured = 0.0F;
                this.timeUntilHooked = 0.0F;
                this.getEntityData().set(DATA_BITING, false);
            }
        } else if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= rate;
            if (this.timeUntilHooked > 0) {
                // 引鱼中：绕着浮标甩出气泡与钓鱼粒子（朝向随机游走）
                this.fishAngle += (float) this.random.triangle(0.0, 9.188);
                float angle = this.fishAngle * ((float) Math.PI / 180.0F);
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                double x = this.getX() + (double) (sin * this.timeUntilHooked * 0.1F);
                double y = (double) ((float) Mth.floor(this.getY()) + 1.0F);
                double z = this.getZ() + (double) (cos * this.timeUntilHooked * 0.1F);
                BlockPos waterPos = BlockPos.containing(x, y - 1.0, z);
                if (this.isFishingFluidBlock(waterPos, serverLevel.getBlockState(waterPos))) {
                    if (this.random.nextFloat() < 0.15F) {
                        serverLevel.sendParticles(this.getBubbleParticle(), x, y - (double) 0.1F, z, 1, sin, 0.1, cos, 0.0);
                    }
                    float f3 = sin * 0.04F;
                    float f4 = cos * 0.04F;
                    serverLevel.sendParticles(this.getFishingParticle(), x, y, z, 0, f4, 0.01, -f3, 1.0);
                    serverLevel.sendParticles(this.getFishingParticle(), x, y, z, 0, -f4, 0.01, f3, 1.0);
                }
            } else {
                // 咬钩：水花声 + 一圈气泡/钓鱼粒子，并进入 nibble 阶段
                this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                double y = this.getY() + 0.5;
                serverLevel.sendParticles(this.getBubbleParticle(), this.getX(), y, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
                serverLevel.sendParticles(this.getFishingParticle(), this.getX(), y, this.getZ(), (int) (1.0F + this.getBbWidth() * 20.0F), this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
                this.nibble = Mth.nextInt(this.random, this.getMinNibbleTicks(), this.getMaxNibbleTicks());
                this.getEntityData().set(DATA_BITING, true);
            }
        } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= rate;
            // 越接近引鱼成功，水花越频繁
            float chance = 0.15F;
            if (this.timeUntilLured < 20) {
                chance += (20 - this.timeUntilLured) * 0.05F;
            } else if (this.timeUntilLured < 40) {
                chance += (40 - this.timeUntilLured) * 0.02F;
            } else if (this.timeUntilLured < 60) {
                chance += (60 - this.timeUntilLured) * 0.01F;
            }
            if (this.random.nextFloat() < chance) {
                float angle = Mth.nextFloat(this.random, 0.0F, 360.0F) * ((float) Math.PI / 180.0F);
                float distance = Mth.nextFloat(this.random, 25.0F, 60.0F);
                double x = this.getX() + (double) (Mth.sin(angle) * distance) * 0.1;
                double y = (double) ((float) Mth.floor(this.getY()) + 1.0F);
                double z = this.getZ() + (double) (Mth.cos(angle) * distance) * 0.1;
                BlockPos particlePos = BlockPos.containing(x, y - 1.0, z);
                if (this.isFishingFluidBlock(particlePos, serverLevel.getBlockState(particlePos))) {
                    serverLevel.sendParticles(this.getSplashParticle(), x, y, z, 2 + this.random.nextInt(2), 0.1F, 0.0, 0.1F, 0.0);
                }
            }
            if (this.timeUntilLured <= 0) {
                this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
                this.timeUntilHooked = Mth.nextInt(this.random, this.getMinHookedTicks(), this.getMaxHookedTicks());
            }
        } else {
            // 刚入水：起一个引鱼倒计时，饵钓（fishSpeed）直接扣减它
            this.timeUntilLured = Mth.nextInt(this.random, this.getMinLureTicks(), this.getMaxLureTicks());
            // 钳到至少 1：初值被一次性扣成 ≤ 0 时，下一 tick 会重新掷一个倒计时再扣，形成永不咬钩的死循环；
            // 钳制后高等级只表现为"几乎立刻上钩"，而不附魔等级再高也仍然能钓。
            this.timeUntilLured = Math.max(1.0F, this.timeUntilLured - this.getFishSpeed());
        }
    }

    // ------------------------------------------------------------------
    // 开阔水面
    // ------------------------------------------------------------------

    /**
     * 开阔水面判定。判定内容在 {@link IFishMediaType} 里（每种水体各自决定"什么算开阔"），
     * 这里只转发，方便子类按鱼钩覆盖（例如整片区域强制算/不算开阔）。
     */
    protected boolean calculateOpenWater(BlockPos pos) {
        return this.getFishMediaType().calculateOpenWater(this.level(), this, pos);
    }

    /**
     * 一片区域内所有方块的水面类型必须一致，否则视为无效。判定在 {@link IFishMediaType}。
     */
    protected IFishMediaType.OpenWaterType getOpenWaterTypeForArea(BlockPos firstPos, BlockPos secondPos) {
        return this.getFishMediaType().getOpenWaterTypeForArea(this.level(), this, firstPos, secondPos);
    }

    /**
     * 单个方块的水面类型。判定在 {@link IFishMediaType}。
     */
    protected IFishMediaType.OpenWaterType getOpenWaterTypeForBlock(BlockPos pos) {
        return this.getFishMediaType().getOpenWaterTypeForBlock(this.level(), this, pos);
    }

    // ------------------------------------------------------------------
    // 命中判定
    // ------------------------------------------------------------------

    private void checkCollision() {
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() == HitResult.Type.MISS || !EventHooks.onProjectileImpact(this, hitResult)) {
            this.onHit(hitResult);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) || target.isAlive() && target instanceof ItemEntity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            this.setHookedEntity(result.getEntity());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        // 撞墙后只保留贴墙方向上的速度
        this.setDeltaMovement(this.getDeltaMovement().normalize().scale(result.distanceTo(this)));
    }

    /**
     * 设置被钩住的实体（会同步给客户端）。子类想扩展钩住逻辑时可以直接调用。
     */
    protected void setHookedEntity(@Nullable Entity hookedEntity) {
        this.hookedIn = hookedEntity;
        this.getEntityData().set(DATA_HOOKED_ENTITY, hookedEntity == null ? 0 : hookedEntity.getId() + 1);
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    /**
     * 被钩住的实体（可能是玩家、生物或掉落物）。
     */
    @Nullable
    public Entity getHookedIn() {
        return this.hookedIn;
    }

    /**
     * 拥有者是不是玩家，是则返回他。
     */
    @Nullable
    public Player getPlayerOwner() {
        Entity owner = this.getOwner();
        return owner instanceof Player player ? player : null;
    }

    /**
     * 是否已经咬钩（浮标下沉，玩家现在收竿就能上鱼）。
     */
    public boolean isBiting() {
        return this.biting;
    }

    /**
     * 是否符合"开阔水面"（原版战利品表与成就用它区分小水池与大海）。
     */
    public boolean isOpenWaterFishing() {
        return this.openWater;
    }

    /**
     * 当前状态机状态。
     */
    public FishHookState getHookState() {
        return this.currentState;
    }

    // ------------------------------------------------------------------
    // 表现与网络
    // ------------------------------------------------------------------

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    /**
     * 原版刻意留空：鱼钩不做位置插值，避免浮标在客户端被平滑拖拽。
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 31 && this.level().isClientSide && this.hookedIn instanceof Player player && player.isLocalPlayer()) {
            this.pullEntity(this.hookedIn);
        }
        super.handleEntityEvent(id);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    /**
     * 让客户端能顺着包里的 data 找到拥有者；没有拥有者时退回自己的 id。
     */
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        Entity owner = this.getOwner();
        return new ClientboundAddEntityPacket(this, serverEntity, owner == null ? this.getId() : owner.getId());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        if (this.getPlayerOwner() == null) {
            int ownerId = packet.getData();
            ArcanumLib.LOGGER.error("Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.", this.level().getEntity(ownerId), ownerId);
            this.kill();
        }
    }

    /**
     * 原版鱼钩状态机：飞行 / 钩在实体上 / 漂浮钓鱼。
     */
    public enum FishHookState {
        FLYING,
        HOOKED_IN_ENTITY,
        BOBBING
    }

}
