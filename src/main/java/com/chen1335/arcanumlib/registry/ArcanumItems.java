package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.holder.EntityHolderItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

/**
 * 全模组共用的物品注册表：任何模块的物品都注册到这里，避免每个特性各建一个 DeferredRegister。
 * 主类只需 {@code ArcanumItems.ITEMS.register(modEventBus)} 一次。
 */
public final class ArcanumItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcanumLib.MODID);


    /** 包装了一个实体的物品：名字与图标都跟着被包装的实体走。 */
    public static final DeferredItem<Item> ENTITY_HOLDER = register("entity_holder",
            EntityHolderItem::new, new Item.Properties().stacksTo(16));

    private ArcanumItems() {
    }

    /** 通用注册：名字自动落在本模组命名空间下。 */
    public static <T extends Item> DeferredItem<T> register(String name, Function<Item.Properties, T> factory,
                                                            Item.Properties properties) {
        return ITEMS.registerItem(name, factory, properties);
    }
}