package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;

import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/**
 * {@code TableSelector} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code TableSelectorHelper}）用法例如：
 * <pre>{@code TableSelectorHelper.all}</pre>
 */
public final class TableSelectorHelper {

    private TableSelectorHelper() {
    }

    /** 对应记录 {@code TableSelector.All}（json 类型见该类的注册项）。 */
    @Info("Matches every loot table (the default).")
    public static TableSelector all() {
        return new TableSelector.All();
    }

    /** 对应记录 {@code TableSelector.ById}（json 类型见该类的注册项）。 */
    @Info("Matches the table whose id equals the given one.")
    public static TableSelector byId(ResourceLocation id) {
        return new TableSelector.ById(id);
    }

    /** 对应记录 {@code TableSelector.Namespace}（json 类型见该类的注册项）。 */
    @Info("Matches tables in the given namespace.")
    public static TableSelector namespace(String namespace) {
        return new TableSelector.Namespace(namespace);
    }

    /** 对应记录 {@code TableSelector.PathPrefix}（json 类型见该类的注册项）。 */
    @Info("Matches tables whose path starts with the given prefix.")
    public static TableSelector pathPrefix(String prefix) {
        return new TableSelector.PathPrefix(prefix);
    }

    /** 对应记录 {@code TableSelector.EntityTagTable}（json 类型见该类的注册项）。 */
    @Info("Matches tables rolled for an entity that carries the given entity type tag.")
    public static TableSelector entityTagTable(TagKey<EntityType<?>> tag) {
        return new TableSelector.EntityTagTable(tag);
    }

    /** 对应记录 {@code TableSelector.BlockTagTable}（json 类型见该类的注册项）。 */
    @Info("Matches tables rolled for a block that carries the given block tag.")
    public static TableSelector blockTagTable(TagKey<Block> tag) {
        return new TableSelector.BlockTagTable(tag);
    }

    /** 对应记录 {@code TableSelector.ByEntityType}（json 类型见该类的注册项）。 */
    @Info("Matches tables rolled for the given entity type.")
    public static TableSelector byEntityType(EntityType<?> entity) {
        return new TableSelector.ByEntityType(entity);
    }

    /** 对应记录 {@code TableSelector.ByBlockType}（json 类型见该类的注册项）。 */
    @Info("Matches tables rolled for the given block.")
    public static TableSelector byBlockType(Block block) {
        return new TableSelector.ByBlockType(block);
    }

    /** 对应记录 {@code TableSelector.Not}（json 类型见该类的注册项）。 */
    @Info("Inverts the wrapped selector.")
    public static TableSelector not(TableSelector child) {
        return new TableSelector.Not(child);
    }

    /** 对应记录 {@code TableSelector.HasPool}（json 类型见该类的注册项）。 */
    @Info("Matches tables that have a pool matching the given pool selector.")
    public static TableSelector hasPool(PoolSelector pool) {
        return new TableSelector.HasPool(pool);
    }

    /** 对应记录 {@code TableSelector.AnyOf}（json 类型见该类的注册项）。 */
    @Info("Matches when at least one of the listed selectors matches.")
    public static TableSelector anyOf(List<TableSelector> children) {
        return new TableSelector.AnyOf(children);
    }

    /** 对应记录 {@code TableSelector.AllOf}（json 类型见该类的注册项）。 */
    @Info("Matches when every listed selector matches.")
    public static TableSelector allOf(List<TableSelector> children) {
        return new TableSelector.AllOf(children);
    }

    /** 对应记录 {@code TableSelector.Custom}（json 类型见该类的注册项）。 */
    @Info("Hands the decision to a listener registered for that custom_id.")
    public static TableSelector custom(String id, @Nullable CompoundTag data) {
        return new TableSelector.Custom(id, Optional.ofNullable(data));
    }
}
