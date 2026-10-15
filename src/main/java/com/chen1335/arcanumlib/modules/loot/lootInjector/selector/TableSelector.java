package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.google.common.collect.Sets;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomTableSelectorEvent;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战利品表选择器：把表和 id 传进来就地判断，与 LootContext 无关，所以数据包加载阶段就能用。
 * 注入器按「表数 × 注入器数」逐个测试，叶子条件要尽量便宜；标签条件需遍历注册表反查，已加缓存。
 */
public interface TableSelector {

    /** 按注册表名分发，别的模组注册进 {@link LootInjectorOps#TABLE_SELECTOR_KEY} 就能加自己的选择器。 */
    Codec<TableSelector> CODEC = LootInjectorOps.TABLE_SELECTORS.byNameCodec()
            .dispatch("type", TableSelector::getType, TableSelectorType::codec);

    /** 类型注册表项，json 里写成 命名空间:路径。 */
    TableSelectorType getType();

    boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries);

    /** 标签在数据包 reload 时会变，派生出来的表 id 集合必须跟着失效。 */
    static void clearCaches() {
        TagTableCache.clear();
    }

    /** 匹配所有表。 */
    record All() implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_ALL.get();
        }
        public static final MapCodec<All> CODEC = MapCodec.unit(All::new);
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return true;
        }
    }

    /** 按表 id 精确匹配。 */
    record ById(ResourceLocation id) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_BY_ID.get();
        }
        public static final MapCodec<ById> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(ById::id)).apply(instance, ById::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return this.id.equals(id);
        }
    }

    /** 按命名空间匹配，例如某个模组命名空间下的全部表。 */
    record Namespace(String namespace) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_NAMESPACE.get();
        }
        public static final MapCodec<Namespace> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("namespace").forGetter(Namespace::namespace)).apply(instance, Namespace::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return this.namespace.equals(id.getNamespace());
        }
    }

    /** 按路径前缀匹配，例如 {@code entities/} 就是所有实体表。 */
    record PathPrefix(String prefix) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_PATH_PREFIX.get();
        }
        public static final MapCodec<PathPrefix> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("prefix").forGetter(PathPrefix::prefix)).apply(instance, PathPrefix::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return id.getPath().startsWith(this.prefix);
        }
    }

    /** 匹配"某个实体标签下所有实体的默认战利品表"。注意表是多对一的：不在标签里的实体可能共用同一张表，改动会一起生效。 */
    record EntityTagTable(TagKey<EntityType<?>> tag) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_ENTITY_TAG.get();
        }
        public static final MapCodec<EntityTagTable> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(EntityTagTable::tag))
                .apply(instance, EntityTagTable::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return TagTableCache.entityTables(this.tag).contains(id);
        }
    }

    /** 匹配"某个方块标签下所有方块的战利品表"，同样有多对一的共享问题。 */
    record BlockTagTable(TagKey<Block> tag) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_BLOCK_TAG.get();
        }
        public static final MapCodec<BlockTagTable> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.BLOCK).fieldOf("tag").forGetter(BlockTagTable::tag))
                .apply(instance, BlockTagTable::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return TagTableCache.blockTables(this.tag).contains(id);
        }
    }
    /** 按实体类型精确匹配它自己的默认战斗利品表（对照按标签的 {@link EntityTagTable}），不做标签展开。 */
    record ByEntityType(EntityType<?> entity) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_ENTITY_TYPE.get();
        }

        public static final MapCodec<ByEntityType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("target").forGetter(ByEntityType::entity))
                .apply(instance, ByEntityType::new));

        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return this.entity.getDefaultLootTable().location().equals(id);
        }
    }

    /** 按方块精确匹配它自己的战利品表（对照按标签的 {@link BlockTagTable}）；没有战利品表的方块永远不命中。 */
    record ByBlockType(Block block) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_BLOCK_TYPE.get();
        }

        public static final MapCodec<ByBlockType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(ByBlockType::block))
                .apply(instance, ByBlockType::new));

        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            ResourceKey<LootTable> lootTable = this.block.getLootTable();

            return lootTable != null && lootTable.location().equals(id);
        }
    }

    /** 取反。 */
    record Not(TableSelector child) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TableSelector.CODEC.fieldOf("condition").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return !this.child.test(id, table, registries);
        }
    }

    /** 表里任意一个池满足给定的池选择器。 */
    record HasPool(PoolSelector pool) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_HAS_POOL.get();
        }
        public static final MapCodec<HasPool> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolSelector.CODEC.fieldOf("pool").forGetter(HasPool::pool)).apply(instance, HasPool::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            List<LootPool> pools = table.pools;

            for (int index = 0; index < pools.size(); index++) {
                if (this.pool.test(index, pools.get(index), registries)) {
                    return true;
                }
            }

            return false;
        }
    }
    /** 任一满足；JSON 里写成数组时等价于 {@link AllOf}。 */
    record AnyOf(List<TableSelector> children) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TableSelector.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::children)).apply(instance, AnyOf::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return this.children.stream().anyMatch(child -> child.test(id, table, registries));
        }
    }

    /** 全部满足。 */
    record AllOf(List<TableSelector> children) implements TableSelector {
        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.TABLE_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TableSelector.CODEC.listOf().fieldOf("conditions").forGetter(AllOf::children)).apply(instance, AllOf::new));
        @Override
        public boolean test(ResourceLocation id, LootTable table, HolderLookup.Provider registries) {
            return this.children.stream().allMatch(child -> child.test(id, table, registries));
        }
    }

    /**
     * 自定义选择器：json 里只给一个 id，判断交给监听 CustomTableSelectorEvent 的代码。
     * 注入器会拿每张表逐个问一遍，触发次数是 注入器数 × 表数，监听者别做重活。
     */
    record Custom(String id, Optional<CompoundTag> data) implements TableSelector {
        /** 只给 id 的简写：不带附加数据。 */
        public Custom(String id) {
            this(id, Optional.empty());
        }
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id),
                LootInjectorOps.CUSTOM_DATA.forGetter(Custom::data)).apply(instance, Custom::new));

        @Override
        public TableSelectorType getType() {
            return LootInjectorOps.CUSTOM_TABLE.get();
        }

        @Override
        public boolean test(ResourceLocation tableId, LootTable table, HolderLookup.Provider registries) {
            return CustomTableSelectorEvent.post(this.id, this.data.orElseGet(CompoundTag::new), tableId, table, registries);
        }
    }
}

/** 标签 → 表 id 集合的缓存。反查要遍历整个注册表，很贵；reload 时必须清空，否则标签改了不生效。 */
final class TagTableCache {
    private static final Map<TagKey<?>, Set<ResourceLocation>> CACHE = new ConcurrentHashMap<>();

    private TagTableCache() {
    }

    static void clear() {
        CACHE.clear();
    }

    static Set<ResourceLocation> entityTables(TagKey<EntityType<?>> tag) {
        return CACHE.computeIfAbsent(tag, key -> BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type.builtInRegistryHolder().is(tag))
                .map(EntityType::getDefaultLootTable)
                .map(ResourceKey::location)
                .collect(Sets::newHashSet, Set::add, Set::addAll));
    }

    static Set<ResourceLocation> blockTables(TagKey<Block> tag) {
        return CACHE.computeIfAbsent(tag, key -> BuiltInRegistries.BLOCK.stream()
                .filter(block -> block.defaultBlockState().is(tag))
                .map(Block::getLootTable)
                .filter(table -> table != null)
                .map(ResourceKey::location)
                .collect(Sets::newHashSet, Set::add, Set::addAll));
    }
}
