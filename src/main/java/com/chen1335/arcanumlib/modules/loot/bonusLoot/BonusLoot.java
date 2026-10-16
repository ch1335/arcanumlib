package com.chen1335.arcanumlib.modules.loot.bonusLoot;

import com.chen1335.arcanumlib.modules.attribute.ArcanumAttributes;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.LootInfoContext;
import com.chen1335.arcanumlib.modules.attribute.AttributeConfig;
import com.chen1335.arcanumlib.modules.loot.bonusLoot.event.BonusLootCheckEvent;
import com.chen1335.arcanumlib.modules.loot.bonusLoot.event.ModifyBonusLootEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 额外战利品：按 {@code arcanumlib:bonus_loot} 属性给出的倍率，对同一张战利品表<b>额外摇取若干份</b>，
 * 结果追加到第一次的结果里。之所以是「重新摇取再追加」而不是「把第一次的结果复制一份」，
 * 是因为表里的概率条目（{@code minecraft:alternatives}、带 {@code random_chance} 的池）每次摇取结果都不同 ——
 * 重新摇取才符合「额外掉落」的直觉，也才让「+25%」这个数字有意义。
 *
 * <p><b>默认只对实体掉落与箱子战利品生效</b>：按战利品表 id 的路径前缀判断
 * （默认 {@code entities/} 与 {@code chests/}），前缀列表可在 {@code config/arcanumlib/attribute.toml} 里改。
 *
 * <p>倍率来源：先看 {@code ATTACKING_ENTITY}（击杀者），没有再看 {@code THIS_ENTITY}
 * （开箱子的是它）—— 两者都是玩家时以击杀者为准。
 *
 * <p>额外摇取复用同一个 {@link RandomSource}（见 {@link LootContext.Builder#Builder(LootContext)}），
 * 所以世界随机序列连续推进，可复现，没有引入额外的随机源。
 *
 * <p>调用点见 {@code mixins/LootTableMixin}（挂在 {@code LootTable#getRandomItems(LootContext)} 上）。
 */
public final class BonusLoot {

    /**
     * 额外掉落倍率的硬上限：属性值再大也只按 20 倍算。
     */
    private static final float MAX_RATIO = 20.0F;

    /**
     * 二次摇取的重入哨兵，挂在 {@link LootContext#pushVisitedElement} 的展开栈上。
     *
     * <p>二次摇取走的是非 Raw 版本（必须如此，否则全局战利品修改器不生效），而它内部会经过本模组
     * mixin 的注入点，于是又回调到 {@link #append} —— 靠这个哨兵放行，避免无限递归。
     *
     * <p>value 选 {@link LootTable#EMPTY} 是刻意的：它是 {@code LootDataType.TABLE} 的 defaultValue，
     * 只在「条件为假」时充当占位符，永远不会作为真实战利品表被展开，因此不会与任何一次真实的 push 相撞。
     * 同理也不能拿 {@code table} 自己当 value —— 那会让 {@code getRandomItemsRaw} 里的
     * {@code pushVisitedElement} 失败，直接走「检测到无限循环」分支返回空列表。
     */
    private static final LootContext.VisitedEntry<LootTable> SECOND_PASS =
            LootContext.createVisitedEntry(LootTable.EMPTY);

    private BonusLoot() {
    }

    /**
     * 按 {@code context} 追加额外掉落。
     *
     * <p>{@code original} 与返回值是同一个列表对象（原地追加），返回值只为了方便调用方直接返回。
     *
     * @param table    正在产出战利品的表，用于与 {@code context} 携带的表 ID 交叉校验
     * @param original 第一次摇取的结果
     * @param context  该次摇取的上下文
     */
    public static ObjectArrayList<ItemStack> append(LootTable table, ObjectArrayList<ItemStack> original, LootContext context) {
        if (context.hasVisitedElement(SECOND_PASS)) {
            return original;
        }

        if (original.isEmpty()) {
            return original;
        }

        ResourceLocation lootTableId = context.getQueriedLootTableId();
        if (LootTableIdCondition.UNKNOWN_LOOT_TABLE.equals(lootTableId) || !lootTableId.equals(table.getLootTableId())) {
            return original;
        }

        if (!BonusLootCheckEvent.post(context, lootTableId, isConfigTarget(lootTableId)).getResult()) {
            return original;
        }

        float ratio = ratioFor(context);

        // 即将增幅：给监听者一次修正倍率（或整个取消）的机会
        ModifyBonusLootEvent event = ModifyBonusLootEvent.post(context, lootTableId, ratio);
        if (event.isCanceled()) {
            return original;
        }

        ratio = Mth.clamp(event.getRatio(), 0.0F, MAX_RATIO);
        if (ratio <= 0.0F) {
            return original;
        }

        addExtraRolls(table, original, context, ratio);

        return original;
    }

    /**
     * 配置里的前缀规则：默认只有实体掉落与箱子战利品吃增幅，前缀可在配置里改。
     *
     * <p>这只是**默认判定** —— 最终要不要增幅由 {@link BonusLootCheckEvent} 决定，
     * 那个事件的初值就是这里的结果，监听者可以覆盖它（见 {@link #append}）。
     */
    public static boolean isConfigTarget(ResourceLocation lootTableId) {
        String path = lootTableId.getPath();

        for (String prefix : AttributeConfig.bonusLootPrefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 取本次增幅的倍率：先看击杀者（{@code ATTACKING_ENTITY}），没有再看 {@code THIS_ENTITY}。
     * 都没有实体（例如纯方块掉落）时返回 0，也就是不增幅。
     */
    public static float ratioFor(LootContext context) {
        LivingEntity source = null;

        if (context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof LivingEntity killer) {
            source = killer;
        } else if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity owner) {
            source = owner;
        }

        if (source == null) {
            return 0.0F;
        }

        AttributeInstance attribute = source.getAttribute(ArcanumAttributes.BONUS_LOOT);

        return attribute == null ? 0.0F : (float) AdjustmentHelper.getAttributeWithAdjusted(source, ArcanumAttributes.BONUS_LOOT, new LootInfoContext(context));
    }

    /**
     * 按 {@code ratio} 往 {@code original} 里追加若干份额外摇取。
     */
    private static void addExtraRolls(LootTable table, ObjectArrayList<ItemStack> original, LootContext context, float ratio) {
        // ── 整数部分：整份摇取并全部收取 ──────────────────────────────────────
        // ratio = 1.5 时摇 1 次，2.3 时摇 2 次；每份都用独立 context，但共享同一个 RandomSource，
        // 随机序列连续推进，因此几份战利品不会摇出完全相同的内容。
        for (int pass = Mth.floor(ratio); pass > 0; pass--) {
            table.getRandomItems(secondaryContext(context), original::add);
        }

        // ── 小数部分：再摇一份，拆成单件后按剩余比例取前 N 件 ──────────────────
        float remainder = Mth.frac(ratio);
        if (remainder <= 0.0F) {
            return;
        }

        ObjectArrayList<ItemStack> draw = new ObjectArrayList<>();
        table.getRandomItems(secondaryContext(context), draw::add);

        // 抽样前把堆叠拆成单件：口径应当是「物品个数」而不是「条目数」，
        // 否则 10 个铁锭堆成一格只算一份，反而比 10 个各占一格的表掉得少。
        ObjectArrayList<ItemStack> singles = new ObjectArrayList<>();

        for (ItemStack stack : draw) {
            int count = stack.getCount();

            for (int i = 0; i < count; i++) {
                singles.add(stack.copyWithCount(1));
            }
        }

        sampleInto(singles, remainder, context.getLevel().getRandom(), original::add);
    }

    /**
     * 按 {@code ratio} 算出该额外产出多少份，基数由调用方给定。
     *
     * @param unitCount 本次实际掉了多少份，作为倍率的基数
     * @param ratio     额外倍率
     */
    public static int rollCount(int unitCount, float ratio, RandomSource random) {
        float expected = unitCount * ratio;
        int count = Mth.floor(expected);
        float fraction = Mth.frac(expected);

        if (fraction > 0.0F && random.nextFloat() < fraction) {
            count++;
        }

        return count;
    }

    /**
     * 从 {@code pool} 里按 {@code ratio} 抽样，把抽中的元素交给 {@code sink}。
     *
     * <p>抽取个数由 {@link #rollCount} 算出，并以 {@code pool} 的大小为上限
     * （{@code ratio > 1} 时不会重复抽同一件）；抽样前会打乱 {@code pool}，避免总是偏向靠前的条目。
     */
    private static void sampleInto(ObjectArrayList<ItemStack> pool, float ratio, RandomSource random, Consumer<ItemStack> sink) {
        int keep = rollCount(pool.size(), ratio, random);
        if (keep <= 0) {
            return;
        }

        Util.shuffle(pool, random);

        for (ItemStack stack : pool.subList(0, Math.min(keep, pool.size()))) {
            sink.accept(stack);
        }
    }

    /**
     * 造一个用于额外摇取的 context。
     *
     * <p><b>每次摇取都必须新建一个</b>：{@code visitedElements} 是整次摇取共享的展开栈，
     * 复用会让 {@code getRandomItemsRaw} 里的 {@code pushVisitedElement} 误判成「检测到战利品表无限循环」
     * 并静默返回空列表；挂上去的 {@link #SECOND_PASS} 哨兵也限定了「一份 context 只服务一次额外摇取」。
     *
     * <p>{@code Builder(context)} 会复制 params / random / queriedLootTableId，所以
     * {@code THIS_ENTITY} 等参数完整保留，随机数也在同一个 {@link RandomSource} 上连续推进。
     *
     * <p>用非 Raw 版本摇取是必须的：它内部经过 {@code CommonHooks#modifyLoot}，
     * 全局战利品修改器才会生效；该重载还会自动套上 {@code createStackSplitter} 的堆叠上限处理。
     */
    private static LootContext secondaryContext(LootContext context) {
        LootContext fresh = new LootContext.Builder(context).create(Optional.empty());
        fresh.pushVisitedElement(SECOND_PASS);

        return fresh;
    }
}
