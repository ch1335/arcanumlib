package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:custom} 的条件选择器时触发。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomConditionSelectorEvent extends Event {
    private final String id;
    private final int index;
    private final LootItemCondition condition;
    private boolean result;

    public CustomConditionSelectorEvent(String id, int index, LootItemCondition condition) {
        this.id = id;
        this.index = index;
        this.condition = condition;
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

    public static boolean post(String id, int index, LootItemCondition condition) {
        CustomConditionSelectorEvent event = new CustomConditionSelectorEvent(id, index, condition);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
