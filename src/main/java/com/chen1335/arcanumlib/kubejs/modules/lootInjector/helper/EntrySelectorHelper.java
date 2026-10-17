package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code EntrySelector} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code EntrySelectorHelper}）用法例如：
 * <pre>{@code EntrySelectorHelper.any}</pre>
 */
public final class EntrySelectorHelper {

    private EntrySelectorHelper() {
    }

    /** 对应记录 {@code EntrySelector.Any}（json 类型见该类的注册项）。 */
    @Info("Matches every entry (the default).")
    public static EntrySelector any() {
        return new EntrySelector.Any();
    }

    /** 对应记录 {@code EntrySelector.ByIndex}（json 类型见该类的注册项）。 */
    @Info("Matches the entry at that index inside its pool, starting from 0.")
    public static EntrySelector byIndex(int index) {
        return new EntrySelector.ByIndex(index);
    }

    /** 对应记录 {@code EntrySelector.IsItem}（json 类型见该类的注册项）。 */
    @Info("Matches minecraft:item entries that drop exactly that item.")
    public static EntrySelector isItem(ResourceLocation item) {
        return new EntrySelector.IsItem(item);
    }

    /** 对应记录 {@code EntrySelector.IsTag}（json 类型见该类的注册项）。 */
    @Info("Matches minecraft:tag entries whose tag equals the given tag.")
    public static EntrySelector isTag(TagKey<Item> tag) {
        return new EntrySelector.IsTag(tag);
    }

    /** 对应记录 {@code EntrySelector.IsItemWithTag}（json 类型见该类的注册项）。 */
    @Info("Matches minecraft:item entries whose item belongs to the given tag.")
    public static EntrySelector isItemWithTag(TagKey<Item> tag) {
        return new EntrySelector.IsItemWithTag(tag);
    }

    /** 对应记录 {@code EntrySelector.IsType}（json 类型见该类的注册项）。 */
    @Info("Matches entries of that json type, such as minecraft:item or minecraft:alternatives.")
    public static EntrySelector isType(ResourceLocation entryType) {
        return new EntrySelector.IsType(entryType);
    }

    /** 对应记录 {@code EntrySelector.WeightInRange}（json 类型见该类的注册项）。 */
    @Info("Matches entries whose weight falls inside the closed interval; composite entries use their whole subtree weight.")
    public static EntrySelector weightInRange(int min, int max) {
        return new EntrySelector.WeightInRange(min, max);
    }

    /** 对应记录 {@code EntrySelector.WeightShareInRange}（json 类型见该类的注册项）。 */
    @Info("Matches entries whose share of their pool's total weight falls inside the closed interval from 0 to 1.")
    public static EntrySelector weightShareInRange(float min, float max) {
        return new EntrySelector.WeightShareInRange(min, max);
    }

    /** 对应记录 {@code EntrySelector.HasCondition}（json 类型见该类的注册项）。 */
    @Info("Matches entries that carry a condition of that type.")
    public static EntrySelector hasCondition(ResourceLocation conditionType) {
        return new EntrySelector.HasCondition(conditionType);
    }

    /** 对应记录 {@code EntrySelector.HasFunction}（json 类型见该类的注册项）。 */
    @Info("Matches entries that carry a function of that type.")
    public static EntrySelector hasFunction(ResourceLocation functionType) {
        return new EntrySelector.HasFunction(functionType);
    }

    /** 对应记录 {@code EntrySelector.HasChild}（json 类型见该类的注册项）。 */
    @Info("Matches composite entries that have a matching direct child (not recursive).")
    public static EntrySelector hasChild(EntrySelector child) {
        return new EntrySelector.HasChild(child);
    }

    /** 对应记录 {@code EntrySelector.Not}（json 类型见该类的注册项）。 */
    @Info("Inverts the wrapped selector.")
    public static EntrySelector not(EntrySelector child) {
        return new EntrySelector.Not(child);
    }

    /** 对应记录 {@code EntrySelector.AnyOf}（json 类型见该类的注册项）。 */
    @Info("Matches when at least one of the listed selectors matches.")
    public static EntrySelector anyOf(List<EntrySelector> children) {
        return new EntrySelector.AnyOf(children);
    }

    /** 对应记录 {@code EntrySelector.AllOf}（json 类型见该类的注册项）。 */
    @Info("Matches when every listed selector matches.")
    public static EntrySelector allOf(List<EntrySelector> children) {
        return new EntrySelector.AllOf(children);
    }

    /** 对应记录 {@code EntrySelector.Custom}（json 类型见该类的注册项）。 */
    @Info("Hands the decision to a listener registered for that custom_id.")
    public static EntrySelector custom(String id, @Nullable CompoundTag data) {
        return new EntrySelector.Custom(id, Optional.ofNullable(data));
    }
}
