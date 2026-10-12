package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import dev.latvian.mods.kubejs.typings.Info;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code ConditionOperator} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code ConditionOperatorHelper}）用法例如：
 * <pre>{@code ConditionOperatorHelper.removeCondition}</pre>
 */
public final class ConditionOperatorHelper {

    private ConditionOperatorHelper() {
    }

    /** 对应记录 {@code ConditionOperator.RemoveCondition}（json 类型见该类的注册项）。 */
    @Info("Deletes the current condition.")
    public static ConditionOperator removeCondition() {
        return new ConditionOperator.RemoveCondition();
    }

    /** 对应记录 {@code ConditionOperator.ReplaceCondition}（json 类型见该类的注册项）。 */
    @Info("Replaces the current condition with a new one.")
    public static ConditionOperator replaceCondition(LootItemCondition condition) {
        return new ConditionOperator.ReplaceCondition(condition);
    }
}
