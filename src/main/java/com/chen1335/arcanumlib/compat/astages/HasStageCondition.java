package com.chen1335.arcanumlib.compat.astages;

import com.chen1335.arcanumlib.modules.loot.lootCondition.LootConditionTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Optional;

/**
 * 阶段条件：掉落上下文里的玩家（或全服）持有匹配的阶段才通过。
 *
 * <p>字段：{@code stage}（字符串简写或 pattern/type 对象）、{@code desc}（本地化显示名，仅供 ALI 展示）、
 * {@code check_server_stages}（true 查服务器级阶段，false 查玩家级，默认 false）。
 *
 * <p>依赖 AStages（软依赖）：没装时恒不通过，不会抛错。
 */
public record HasStageCondition(StagePattern stage, Optional<String> desc, boolean checkServerStages)
        implements LootItemCondition {

    public static final MapCodec<HasStageCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StagePattern.CODEC.fieldOf("stage").forGetter(HasStageCondition::stage),
            Codec.STRING.optionalFieldOf("desc").forGetter(HasStageCondition::desc),
            Codec.BOOL.optionalFieldOf("check_server_stages", false).forGetter(HasStageCondition::checkServerStages)
    ).apply(instance, HasStageCondition::new));

    public static LootItemCondition.Builder builder(StagePattern stage) {
        return () -> new HasStageCondition(stage, Optional.empty(), false);
    }

    /** desc 是本地化显示名（只在 ALI 里用），可为 null。 */
    public static LootItemCondition.Builder builder(StagePattern stage, String desc, boolean checkServerStages) {
        return () -> new HasStageCondition(stage, Optional.ofNullable(desc), checkServerStages);
    }

    @Override
    public LootItemConditionType getType() {
        return LootConditionTypes.HAS_STAGE.get();
    }

    @Override
    public boolean test(LootContext context) {
        return AStagesCompat.matches(context, this.checkServerStages, this.stage);
    }
}
