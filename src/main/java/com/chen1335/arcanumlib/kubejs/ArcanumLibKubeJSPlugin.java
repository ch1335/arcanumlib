package com.chen1335.arcanumlib.kubejs;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.ConditionOperatorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.ConditionSelectorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.EntryOperatorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.EntrySelectorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.FunctionOperatorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.FunctionSelectorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.LootConditionHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.LootEntryHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.LootFunctionHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.NumberProviderHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.PoolOperatorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.PoolSelectorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.TableOperatorHelper;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper.TableSelectorHelper;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.TypeWrapperRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

/**
 * ArcanumLib 的 KubeJS 插件入口。
 *
 * <p>注册方式是 jar 根目录的 {@code kubejs.plugins.txt}（里面写本类的全限定名），不是注解 ——
 * 所以 KubeJS 不在场时本类根本不会被加载，也就不需要额外的存在性判断。
 *
 * <p>三个覆写点的现状：
 * <ul>
 *     <li>{@link #registerEvents(EventGroupRegistry)} —— 已注册脚本事件组：注入器事件与九个自定义钩子
 *         （{@code table_custom} / {@code pool_custom} / {@code entry_custom} 及五个自定义选择器）；</li>
 *     <li>{@link #registerBindings(BindingRegistry)} —— 已注册 {@code ArcanumLib} 与全部 13 个 Helper
 *         （操作 5 层 + 选择器 5 族 + 条件 / 函数 / 数字提供器）；</li>
 *     <li>{@link #registerTypeWrappers(TypeWrapperRegistry)} —— 仍待补：让 JS 侧能直接传
 *         {@code ItemStack} / 权重等参数给本模组的类型。</li>
 * </ul>
 */
public class ArcanumLibKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void init() {
        ArcanumLib.LOGGER.info("ArcanumLib: KubeJS plugin loaded");
    }

    /** 脚本事件：把本模组的注入器事件与自定义钩子注册进 KubeJS 的事件系统。 */
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(ArcanumLibKubeJSEvents.GROUP);
    }

    /**
     * 全局绑定：脚本里可直接用的辅助对象。
     *
     * <p>Helper 类里全是静态工厂方法，每个方法都带 KubeJS 的 {@code @Info}（功能）与
     * {@code @Param}（参数作用），可为空的参数标了 {@code @Nullable}。
     * 绑定名与类名一致，脚本里直接写 {@code TableOperatorHelper.addPool(...)} 这样即可。
     */
    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type().isServer() || bindings.type().isStartup()) {
            bindings.add("ArcanumLib", ArcanumLibKubeJSBindings.class);

            // 操作：表 / 池 / 条目 / 函数 / 条件 五层
            bindings.add("TableOperatorHelper", TableOperatorHelper.class);
            bindings.add("PoolOperatorHelper", PoolOperatorHelper.class);
            bindings.add("EntryOperatorHelper", EntryOperatorHelper.class);
            bindings.add("FunctionOperatorHelper", FunctionOperatorHelper.class);
            bindings.add("ConditionOperatorHelper", ConditionOperatorHelper.class);

            // 选择器：表 / 池 / 条目 / 函数 / 条件 五族
            bindings.add("TableSelectorHelper", TableSelectorHelper.class);
            bindings.add("PoolSelectorHelper", PoolSelectorHelper.class);
            bindings.add("EntrySelectorHelper", EntrySelectorHelper.class);
            bindings.add("FunctionSelectorHelper", FunctionSelectorHelper.class);
            bindings.add("ConditionSelectorHelper", ConditionSelectorHelper.class);

            // 条件 / 函数 / 数字提供器 / 条目：覆盖本模组条件与全部原版条目
            bindings.add("LootConditionHelper", LootConditionHelper.class);
            bindings.add("LootFunctionHelper", LootFunctionHelper.class);
            bindings.add("NumberProviderHelper", NumberProviderHelper.class);
            bindings.add("LootEntryHelper", LootEntryHelper.class);
        }
    }

    /**
     * 类型包装：让 JS 侧的值能直接喂给需要本模组类型的参数。
     *
     * <p>两类包装：
     * <ul>
     *     <li><b>我们的选择器 / 操作</b>：十个接口都有按名派发的 {@code CODEC}，
     *         用 {@code registerCodec} 一挂，脚本就能直接写 json 对象
     *         （如 {@code {type: 'item', item: 'minecraft:diamond'}}）当参数；</li>
     *     <li><b>战利品条目</b>：KubeJS 自己包了 {@code ItemStack} / {@code ResourceLocation} 等，
     *         但没有 {@code LootPoolEntryContainer}，所以在这里补 ——
     *         写物品 id 字符串或 {@code ItemStack} 都能变成一条 item 条目。</li>
     * </ul>
     */
    @Override
    public void registerTypeWrappers(TypeWrapperRegistry registry) {
        // 选择器 5 族
        registry.registerCodec(TableSelector.class, TableSelector.CODEC);
        registry.registerCodec(PoolSelector.class, PoolSelector.CODEC);
        registry.registerCodec(EntrySelector.class, EntrySelector.CODEC);
        registry.registerCodec(FunctionSelector.class, FunctionSelector.CODEC);
        registry.registerCodec(ConditionSelector.class, ConditionSelector.CODEC);

        // 整条注入器也能直接写 json（registerLootInjector 事件里用得上）
        registry.registerCodec(LootInjector.class, LootInjector.CODEC);

        // 操作 5 层
        registry.registerCodec(TableOperator.class, TableOperator.CODEC);
        registry.registerCodec(PoolOperator.class, PoolOperator.CODEC);
        registry.registerCodec(EntryOperator.class, EntryOperator.CODEC);
        registry.registerCodec(FunctionOperator.class, FunctionOperator.CODEC);
        registry.registerCodec(ConditionOperator.class, ConditionOperator.CODEC);

        // 便捷写法：'minecraft:diamond' 或 ItemStack -> 一条 item 条目
        registry.register(LootPoolEntryContainer.class,
                (from, target) -> from instanceof String || from instanceof ItemStack,
                (cx, from) -> {
                    if (from instanceof ItemStack stack) {
                        return LootItem.lootTableItem(stack.getItem()).build();
                    }

                    return LootItem.lootTableItem(BuiltInRegistries.ITEM.get(ResourceLocation.parse(from.toString()))).build();
                });
    }
}
