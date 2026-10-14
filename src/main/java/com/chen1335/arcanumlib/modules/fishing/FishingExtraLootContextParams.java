package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

public class FishingExtraLootContextParams {
    public static final LootContextParam<ItemStack> FISH_BAIT = create("fish_bait");

    private static <T> LootContextParam<T> create(String id) {
        return new LootContextParam<>(ArcanumLib.id(id));
    }
}
