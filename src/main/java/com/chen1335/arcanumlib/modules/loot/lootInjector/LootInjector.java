package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.List;

/**
 * 一个注入器 = 数据包里的一个 json。{@code id} 由文件名给出、不写进 json，用来保证同一注入器对同一张表只改一次。
 *
 * <p>{@code conditions} 是 NeoForge 的条件列表（json 键 {@code neoforge:conditions}）：
 * 只要有一条不满足，这个注入器就当作**没加载**，不会作用到任何表上。默认空列表 = 无条件。
 */
public record LootInjector(ResourceLocation id, TableSelector tableSelector, int priority,
                           List<TableOperator> ops, List<ICondition> conditions) {

    public static final Codec<LootInjector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TableSelector.CODEC.optionalFieldOf("tableSelector", new TableSelector.All())
                    .forGetter(LootInjector::tableSelector),
            Codec.INT.optionalFieldOf("priority", 1000).forGetter(LootInjector::priority),
            TableOperator.CODEC.listOf().fieldOf("ops").forGetter(LootInjector::ops),
            ICondition.LIST_CODEC.optionalFieldOf("neoforge:conditions", List.of()).forGetter(LootInjector::conditions)
    ).apply(instance, (tableSelector, priority, ops, conditions) ->
            new LootInjector(null, tableSelector, priority, ops, conditions)));

    /** 不带条件的便捷构造：等价于 {@code conditions = List.of()}。 */
    public LootInjector(ResourceLocation id, TableSelector tableSelector, int priority, List<TableOperator> ops) {
        this(id, tableSelector, priority, ops, List.of());
    }

    public LootInjector withId(ResourceLocation newId) {
        return new LootInjector(newId, this.tableSelector, this.priority, this.ops, this.conditions);
    }

    /** 条件是否全部满足；不满足就代表这个注入器没加载。 */
    public boolean conditionsMatch(ICondition.IContext context) {
        for (ICondition condition : this.conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }
}
