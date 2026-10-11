package com.chen1335.arcanumlib.compat.astages;

import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Set;

/**
* 唯一触碰 AStages 类的部分（由 {@link AStagesCompat} 确认装了才加载）。AStages 调用都集中在这里，
* 将来它 3.0.0 改接口只需改这个文件。
*/
final class AStagesStageLookup {

    static boolean matches(LootContext context, boolean checkServerStages, StagePattern pattern) {
        if (checkServerStages) {
            return matchesAny(AStagesUtils.getStages(AHolder.server()), pattern);
        }

        Player player = playerOf(context);

        return player != null && matchesAny(AStagesUtils.getStages(AHolder.player(player)), pattern);
    }

    /** 与 AStages 本体一致的取人顺序：最后伤害玩家 → 攻击者 → 本实体；取不到就算不通过（不猜最近玩家）。 */
    private static Player playerOf(LootContext context) {
        if (context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof Player player) {
            return player;
        }
        if (context.getParamOrNull(LootContextParams.ATTACKING_ENTITY) instanceof Player player) {
            return player;
        }
        if (context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof Player player) {
            return player;
        }

        return null;
    }

    private static boolean matchesAny(Set<String> stages, StagePattern pattern) {
        return stages.stream().anyMatch(pattern::matches);
    }

    private AStagesStageLookup() {
    }
}
