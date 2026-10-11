package com.chen1335.arcanumlib.compat.astages;

import com.alessandro.astages.api.plugin.AStagesPlugin;
import net.minecraft.resources.ResourceLocation;

/**
 * AStages 联动入口：AStages 构造期扫描 scan data，实例化"直接 implements AStagesPlugin 且有公开无参构造"的类；
 * 日志会打印 Registered plugin。本类必须零依赖 —— 它在本模组注册表建好之前就被类加载。
 */
public class ArcanumAStagesPlugin implements AStagesPlugin {

    @Override
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath("arcanumlib", "astages_compat");
    }
}

