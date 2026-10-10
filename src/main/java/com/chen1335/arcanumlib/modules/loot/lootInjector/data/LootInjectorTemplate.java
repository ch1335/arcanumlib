package com.chen1335.arcanumlib.modules.loot.lootInjector.data;

import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOp;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorReloadListener;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 写注入器的模板基类：子类只给"哪张表掉哪些条目、各自多少概率"，其余骨架都在这里。
 * {@link LootInjectorProvider} 只管编码落盘；概率条件属于模组自己的注册表内容，留成抽象钩子。
 */
public abstract class LootInjectorTemplate extends LootInjectorProvider {

    /** 默认优先级：越小越先应用。同名注入器按 id 去重，同表重复应用由 applier 兜住。 */
    protected static final int DEFAULT_PRIORITY = 1000;

    protected LootInjectorTemplate(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                   String name) {
        this(output, registries, LootInjectorReloadListener.DIRECTORY, name);
    }

    protected LootInjectorTemplate(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                   String directory, String name) {
        super(output, registries, directory, name);
    }

    /**
     * 一条掉落：基础概率 + 每级抢夺加成 + 条目本身。
     * {@code entry} 必须是【工厂】而非 Builder 实例：{@code when()} 往自己身上追加条件，共享实例会让条件越积越多。
     */
    public record Drop(double chance, double perLooting, Supplier<LootPoolEntryContainer.Builder<?>> entry) {

        public Drop(double chance, Supplier<LootPoolEntryContainer.Builder<?>> entry) {
            this(chance, 0.0D, entry);
        }
    }

    // ---- 子类必须提供 ----------------------------------------------------

    /** 注入器 id 的命名空间，最终落在 {@code data/<ns>/<directory>/<path>.json}。 */
    protected abstract ResourceLocation id(String path);

    /** 这条掉落的概率条件；基础概率与每级抢夺加成都在 {@link Drop} 里，怎么表达随你。 */
    protected abstract LootItemCondition.Builder chanceCondition(Drop drop);

    // ---- 模板提供的骨架 --------------------------------------------------

    /**
     * "被抽中一次就把所有孩子都产出"的条目，默认用 {@link AllOfEntry}，一般不用覆写。
     * 不能用原版 {@code minecraft:group}：它只把权重摊平，{@code addRandomItem} 仍按权重抽一个，"N 个条目"会退化成"每次只出 1 个"。
     */
    protected LootPoolEntryContainer.Builder<?> allChildren(List<LootPoolEntryContainer.Builder<?>> children) {
        return AllOfEntry.allOf(children);
    }

    /** 一张普通战利品表一个注入器：按表 id 精确匹配。 */
    protected LootInjector merged(String path, ResourceKey<LootTable> table, Drop... drops) {
        return injector(path, new TableCondition.ById(table.location()), drops);
    }

    /** 一个生物一个注入器：按它的默认战利品表匹配。 */
    protected LootInjector boss(String path, EntityType<?> type, Drop... drops) {
        return injector(path, new TableCondition.ById(type.getDefaultLootTable().location()), drops);
    }

    /** 需要别的表选择器（命名空间、路径前缀、标签…）时直接用这个。 */
    protected LootInjector injector(String path, TableCondition tableCondition, Drop... drops) {
        return new LootInjector(id(path), tableCondition, DEFAULT_PRIORITY, pools(drops));
    }

    /**
     * 一个池 + 一个"全出"条目：池掷一次必中它，它被抽中后把所有孩子都产出去。
     * 概率挂在孩子上，所以各自独立判定、彼此不抢权重；池里只有一个条目，故权重占比恒为 1。
     */
    protected List<LootInjectorOp> pools(Drop... drops) {
        List<LootPoolEntryContainer.Builder<?>> children = new ArrayList<>();

        for (Drop drop : drops) {
            children.add(drop.entry().get().when(chanceCondition(drop)));
        }

        return List.of(new LootInjectorOp.AddPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(allChildren(children))
                .build()));
    }

    /** 抢夺让数量上限 +0~1。 */
    protected static LootItemFunction.Builder lootingCount(HolderLookup.Provider provider) {
        return EnchantedCountIncreaseFunction.lootingMultiplier(provider, UniformGenerator.between(0.0F, 1.0F));
    }
}
