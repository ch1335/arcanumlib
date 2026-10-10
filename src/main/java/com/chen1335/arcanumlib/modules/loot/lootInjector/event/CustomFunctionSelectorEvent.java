package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:custom} 的函数选择器时触发。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomFunctionSelectorEvent extends Event {
    private final String id;
    private final int index;
    private final LootItemFunction function;
    private boolean result;

    public CustomFunctionSelectorEvent(String id, int index, LootItemFunction function) {
        this.id = id;
        this.index = index;
        this.function = function;
    }

    public String getId() {
        return this.id;
    }

    /** 函数在它所属列表里的下标。 */
    public int getIndex() {
        return this.index;
    }

    public LootItemFunction getFunction() {
        return this.function;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public static boolean post(String id, int index, LootItemFunction function) {
        CustomFunctionSelectorEvent event = new CustomFunctionSelectorEvent(id, index, function);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
