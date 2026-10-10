package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 自定义池选择器求值时触发，用 setResult 决定是否命中。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomPoolConditionEvent extends Event {
    private final String id;
    private final int index;
    private final LootPool pool;
    private boolean result;

    public CustomPoolConditionEvent(String id, int index, LootPool pool) {
        this.id = id;
        this.index = index;
        this.pool = pool;
    }

    public String getId() {
        return this.id;
    }

    /** 池在它所属表里的下标。 */
    public int getIndex() {
        return this.index;
    }

    public LootPool getPool() {
        return this.pool;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public static boolean post(String id, int index, LootPool pool) {
        CustomPoolConditionEvent event = new CustomPoolConditionEvent(id, index, pool);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
