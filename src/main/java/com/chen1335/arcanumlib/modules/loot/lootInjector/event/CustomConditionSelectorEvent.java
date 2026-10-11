package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:custom} 的条件选择器时触发。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomConditionSelectorEvent extends Event {
    private final String id;
    private final int index;
    private final LootItemCondition condition;
    private final HolderLookup.Provider registries;
    private boolean result;

    public CustomConditionSelectorEvent(String id, int index, LootItemCondition condition, HolderLookup.Provider registries) {
        this.id = id;
        this.index = index;
        this.condition = condition;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    /** 条件在它所属列表里的下标。 */
    public int getIndex() {
        return this.index;
    }

    public LootItemCondition getCondition() {
        return this.condition;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    /** 求值时可用的注册表访问：需要按 codec 反序列化 / 反查注册项时用。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public static boolean post(String id, int index, LootItemCondition condition, HolderLookup.Provider registries) {
        CustomConditionSelectorEvent event = new CustomConditionSelectorEvent(id, index, condition, registries);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
