package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** 一个注入器 = 数据包里的一个 json。{@code id} 由文件名给出、不写进 json，用来保证同一注入器对同一张表只改一次。 */
public record LootInjector(ResourceLocation id, TableCondition tableCondition, int priority, List<TableOperator> ops) {

    public static final Codec<LootInjector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TableCondition.CODEC.optionalFieldOf("tableCondition", new TableCondition.All())
                    .forGetter(LootInjector::tableCondition),
            Codec.INT.optionalFieldOf("priority", 1000).forGetter(LootInjector::priority),
            TableOperator.CODEC.listOf().fieldOf("ops").forGetter(LootInjector::ops)
    ).apply(instance, (tableCondition, priority, ops) -> new LootInjector(null, tableCondition, priority, ops)));

    public LootInjector withId(ResourceLocation newId) {
        return new LootInjector(newId, this.tableCondition, this.priority, this.ops);
    }
}
