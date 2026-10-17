package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.List;

/**
 * {@code FunctionOperator} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code FunctionOperatorHelper}）用法例如：
 * <pre>{@code FunctionOperatorHelper.removeFunction}</pre>
 */
public final class FunctionOperatorHelper {

    private FunctionOperatorHelper() {
    }

    /** 对应记录 {@code FunctionOperator.RemoveFunction}（json 类型见该类的注册项）。 */
    @Info("Deletes the current function.")
    public static FunctionOperator removeFunction() {
        return new FunctionOperator.RemoveFunction();
    }

    /** 对应记录 {@code FunctionOperator.ReplaceFunction}（json 类型见该类的注册项）。 */
    @Info("Replaces the current function with a new one.")
    public static FunctionOperator replaceFunction(LootItemFunction function) {
        return new FunctionOperator.ReplaceFunction(function);
    }

    /** 对应记录 {@code FunctionOperator.ModifyCondition}（json 类型见该类的注册项）。 */
    @Info("Runs the child condition ops on the conditions the current function carries itself; functions without conditions are skipped.")
    public static FunctionOperator modifyCondition(ConditionSelector conditionSelector, List<ConditionOperator> ops) {
        return new FunctionOperator.ModifyCondition(conditionSelector, ops);
    }
}
