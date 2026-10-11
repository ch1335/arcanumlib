package com.chen1335.arcanumlib.compat.astages;

import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierType;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;

import java.util.List;
import java.util.Optional;

/**
 * 阶段加工器：命中阶段就依次过 {@code success} 链，否则依次过 {@code fail} 链；两条链各自是一串普通加工器。
 *
 * <p>依赖 AStages（软依赖）：没装时等同"未命中"，只会走 {@code fail}。
 */
public record StageChanceModifier(StagePattern stage, Optional<String> desc, boolean checkServerStages,
                                  List<ChanceModifier> success, List<ChanceModifier> fail) implements ChanceModifier {

    public static final MapCodec<StageChanceModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StagePattern.CODEC.fieldOf("stage").forGetter(StageChanceModifier::stage),
            Codec.STRING.optionalFieldOf("desc").forGetter(StageChanceModifier::desc),
            Codec.BOOL.optionalFieldOf("check_server_stages", false).forGetter(StageChanceModifier::checkServerStages),
            ChanceModifier.CODEC.listOf().optionalFieldOf("success", List.<ChanceModifier>of())
                    .forGetter(StageChanceModifier::success),
            ChanceModifier.CODEC.listOf().optionalFieldOf("fail", List.<ChanceModifier>of())
                    .forGetter(StageChanceModifier::fail)
    ).apply(instance, StageChanceModifier::new));

    @Override
    public ChanceModifierType getType() {
        return ChanceModifierTypes.STAGE.get();
    }

    @Override
    public double apply(double chance, LootContext context) {
        List<ChanceModifier> chain = AStagesCompat.matches(context, this.checkServerStages, this.stage)
                ? this.success
                : this.fail;
        double result = chance;

        for (ChanceModifier modifier : chain) {
            result = modifier.apply(result, context);
        }

        return result;
    }
}
