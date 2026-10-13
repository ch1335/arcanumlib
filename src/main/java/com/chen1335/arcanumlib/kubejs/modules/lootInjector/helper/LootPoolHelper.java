package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import net.minecraft.world.level.storage.loot.LootPool;

public class LootPoolHelper {
    public static LootPool.Builder builder() {
        return new LootPool.Builder();
    }
}
