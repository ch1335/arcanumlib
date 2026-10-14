package com.chen1335.arcanumlib.modules.loot.lootEntry;

import com.chen1335.arcanumlib.modules.holder.EntityHolderItem;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

import java.util.List;
import java.util.function.Consumer;

/**
 * 不直接刷怪，而是产出一个 **0 数量的 {@code EntityHolderItem} 标记**（原版语义＝空物品，玩家看不见、
 * 漏斗搬不走、也写不进存档），标记里带着"要生成哪个实体 / 几只 / 什么 nbt"。
 *
 * <p>后续由两个钩子接管，生成过程对齐 {@code /summon}：
 * <ul>
 *   <li>{@code mixins/LootTableFillMixin}：灌进容器时把这个标记拦下，不让它占槽位；</li>
 *   <li>{@code modules/holder/EntityHolderEvents}：它以掉落物形态进入世界时拒绝该掉落物，</li>
 * </ul>
 * 并在对应位置生成实体（{@code EntityHolderHandler}，未给 nbt 时补 {@code finalizeMobSpawn}）。
 *
 * <p>权重、品质、条件沿用 {@link LootPoolSingletonContainer} 的那套。
 */
public class SummonMobEntry extends LootPoolSingletonContainer {
    public static final MapCodec<SummonMobEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("mob").forGetter(SummonMobEntry::mob),
            NumberProviders.CODEC.optionalFieldOf("count", ConstantValue.exactly(1.0F)).forGetter(SummonMobEntry::count),
            CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(SummonMobEntry::nbt)
    ).and(LootPoolSingletonContainer.singletonFields(instance)).apply(instance, SummonMobEntry::new));

    private final EntityType<?> mob;
    private final NumberProvider count;
    private final CompoundTag nbt;

    private SummonMobEntry(EntityType<?> mob, NumberProvider count, CompoundTag nbt, int weight, int quality,
                           List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.mob = mob;
        this.count = count;
        this.nbt = nbt;
    }

    /** 要生成哪一个实体类型。 */
    public EntityType<?> mob() {
        return this.mob;
    }

    /** 生成几只；数字提供器，所以也能写区间（例如 {@code {"type":"minecraft:uniform","min":1,"max":3}}）。 */
    public NumberProvider count() {
        return this.count;
    }

    /** 附加的自定义 NBT，写法与 {@code /summon} 的 nbt 参数一致；空标签表示不附加。 */
    public CompoundTag nbt() {
        return this.nbt;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootEntryTypes.SUMMON_MOB.get();
    }

    @Override
    public void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
        // 不再当场刷怪：产出一个 0 数量的 EntityHolderItem 当标记（原版语义＝空物品，不会被玩家看到）。
        // 之后的处理交给两个钩子：LootTableFillMixin 拦住"灌进容器"，EntityHolderEvents 拦住"掉进世界"并生成实体。
        consumer.accept(EntityHolderItem.marker(this.mob, this.nbt, this.count.getInt(context)));
    }

    public static Builder summonMob(EntityType<?> mob) {
        return new Builder(mob);
    }

    public static class Builder extends LootPoolSingletonContainer.Builder<Builder> {
        private final EntityType<?> mob;
        private NumberProvider count = ConstantValue.exactly(1.0F);
        private CompoundTag nbt = new CompoundTag();

        private Builder(EntityType<?> mob) {
            this.mob = mob;
        }

        /** 生成数量。 */
        public Builder count(NumberProvider count) {
            this.count = count;
            return this;
        }

        /** 附加自定义 NBT（写法同 {@code /summon}）。 */
        public Builder nbt(CompoundTag nbt) {
            this.nbt = nbt;
            return this;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new SummonMobEntry(this.mob, this.count, this.nbt, this.weight, this.quality,
                    this.getConditions(), this.getFunctions());
        }
    }
}