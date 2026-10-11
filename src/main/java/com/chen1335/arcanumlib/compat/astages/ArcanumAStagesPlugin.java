package com.chen1335.arcanumlib.compat.astages;

import com.alessandro.astages.api.plugin.AStagesPlugin;
import net.minecraft.resources.ResourceLocation;

/**
 * AStages 联动入口。AStages 在自己构造期扫描所有 mod 的 scan data，把"直接 implements AStagesPlugin
 * 且有公开无参构造"的类实例化并注册（见其 PluginScanner），日志会打印 Registered plugin。
 *
 * <p>两条约束：必须是**直接**实现（走父类扫不到），且本类必须**零依赖** —— 它在 AStages 构造期
 * 被 Class.forName 加载，那时本模组的注册表还没建好，所以这里不引用任何本模组类型。
 */
public class ArcanumAStagesPlugin implements AStagesPlugin {

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath("arcanumlib", "astages_compat");
    }
}
