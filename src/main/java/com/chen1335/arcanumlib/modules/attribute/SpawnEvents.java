package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.mixins.IWeightAccessor;
import com.chen1335.arcanumlib.mixins.IntrusiveBaseWeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 寻觅：按范围内最高的寻觅值提高「稀有」生物的刷怪权重。
 *
 * <p>做法是往表里【追加】一条额外权重的同类型条目，而不是替换原条目——原实例始终留在表里，
 * 所以「抽到原实例」这条路径在任何坐标都能过 {@code canSpawnMobAt} 的 contains 校验；
 * 追加项按原实例缓存、引用永不改变，只在权重变化时原地改它，于是「抽到追加项」也能靠 identity 过。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class SpawnEvents {

    /**
     * 与 {@code NaturalSpawner.SPAWN_DISTANCE_BLOCK} 一致：刷怪只发生在玩家 128 格内。
     */
    public static final double SEEKING_RADIUS = 128.0D;

    /**
     * 原实例 → 追加项。引用必须稳定，换实例 contains 校验就过不了。
     */
    private static final Map<MobSpawnSettings.SpawnerData, MobSpawnSettings.SpawnerData> EXTRA_ENTRIES =
            new IdentityHashMap<>();

    /**
     * 每 tick 刷新一次：玩家 → 寻觅值。刷怪事件里只读这里，不再每次去读属性。
     *
     * <p>刷怪尝试每 tick 有数千次，而 {@code getInstance} + {@code getValue} 虽不贵，乘上
     * 「事件次数 × 全服玩家数」就成了热点，所以统一挪到每 tick 一次。
     */
    private static final Map<ServerPlayer, Double> SEEKING_CACHE = new IdentityHashMap<>();

    /** 本 tick 是否有任何玩家带寻觅值；没有时刷怪事件直接返回，连玩家列表都不遍历。 */
    private static boolean hasAnySeeking = false;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        List<MobSpawnSettings.SpawnerData> list = event.getSpawnerDataList();
        double threshold = AttributeConfig.threshold();
        if (list.isEmpty() || threshold <= 0.0D) {
            return;
        }

        double seeking = seeking(level, event.getPos());
        if (seeking <= 0.0D) {
            return;
        }

        // 阈值必须用【原始】总权重算：事件拿到的是群系原表的副本，边改边算会让占比漂移
        int total = 0;
        for (MobSpawnSettings.SpawnerData data : list) {
            total += effectiveWeight(data);
        }
        if (total <= 0) {
            return;
        }

        // addSpawnerData 只往尾部追加，所以用索引循环就够了
        for (MobSpawnSettings.SpawnerData data : list) {
            // 稀有判定按「权重 × 平均数量」：只刷 1 只的怪不该和一次刷 4 只的一样常见
            if (!isRare(effectiveWeight(data), total, threshold)) {
                continue;
            }

            // 概率取整：整数部分必定加，小数部分按该比例再随机 +1（长期平均正好等于 权重 × 寻觅值）
            double bonus = data.getWeight().asInt() * seeking;
            int extra = (int) Math.floor(bonus);
            if (level.random.nextDouble() < bonus - extra) {
                extra++;
            }
            if (extra <= 0) {
                continue;
            }

            // lambda 捕获要求 effectively final，所以复制一份
            int boostedWeight = extra;
            MobSpawnSettings.SpawnerData boosted = EXTRA_ENTRIES.computeIfAbsent(data,
                    key -> {
                        MobSpawnSettings.SpawnerData spawnerData = new MobSpawnSettings.SpawnerData(key.type, 0, key.minCount, key.maxCount);
                        ((IWeightAccessor) ((IntrusiveBaseWeightAccessor) spawnerData).arcanumlib$getWeight()).arcanumlib$setValue(boostedWeight);
                        return spawnerData;
                    });

            if (boosted.getWeight().asInt() != extra) {
                ((IWeightAccessor) ((IntrusiveBaseWeightAccessor) boosted).arcanumlib$getWeight()).arcanumlib$setValue(extra);
            }

            event.addSpawnerData(boosted);
        }
    }

    /**
     * 每 tick 刷新寻觅缓存：只有这一步读属性，刷怪事件里只剩距离比较。
     *
     * <p>用 {@code Pre} 而不是 {@code Post}：缓存要在本 tick 的刷怪开始之前就绪。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        SEEKING_CACHE.clear();
        hasAnySeeking = false;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                // 走 getInstance 而不是 getAttributeValue：后者对没挂上的属性会抛异常
                AttributeInstance instance = player.getAttributes().getInstance(ArcanumAttributes.SEEKING);
                double value = instance == null ? 0.0D : instance.getValue();
                if (value > 0.0D) {
                    SEEKING_CACHE.put(player, value);
                    hasAnySeeking = true;
                }
            }
        }
    }

    /**
     * 生物实例随世界加载变化，服停时清一次，免得跨存档留着旧引用。
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EXTRA_ENTRIES.clear();
        SEEKING_CACHE.clear();
        hasAnySeeking = false;
    }

    /**
     * 取范围内所有玩家的寻觅值里最高的那个：刷怪没有「贡献者」概念，只能按最强值算。
     *
     * <p>只读 {@link #SEEKING_CACHE}；缓存值不大于当前最高值时直接跳过，连距离都不比。
     */
    private static double seeking(ServerLevel level, BlockPos pos) {
        if (!hasAnySeeking) {
            return 0.0D;
        }

        double radiusSqr = SEEKING_RADIUS * SEEKING_RADIUS;
        double best = 0.0D;

        for (ServerPlayer player : level.players()) {
            Double cached = SEEKING_CACHE.get(player);
            if (cached == null || cached <= best) {
                continue;
            }

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > radiusSqr) {
                continue;
            }

            best = cached;
        }

        return best;
    }

    /**
     * 有效权重 = 权重 × 平均每次刷新的数量，向上取整；用整数算避免浮点误差多进一位。
     */
    private static int effectiveWeight(MobSpawnSettings.SpawnerData data) {
        int weight = Math.max(data.getWeight().asInt(), 0);
        int sum = data.minCount + data.maxCount;

        return (weight * sum + 1) / 2;
    }

    /**
     * 该生物的有效权重占整张表的比例是否低于阈值。
     */
    private static boolean isRare(int weight, int total, double threshold) {
        return threshold > 0.0D && total > 0 && (double) Math.max(weight, 0) / total < threshold;
    }
}
