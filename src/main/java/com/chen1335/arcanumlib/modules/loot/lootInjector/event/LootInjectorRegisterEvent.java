package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 收集注入器时触发：除了数据包里的 json，还能在**运行时**用代码或脚本注册注入器。
 *
 * <p>触发点在「数据包 json 已解析完、还没应用到战利品表」之间，所以这里注册的注入器与 json 里的
 * 一视同仁：一样按 {@code priority} 排序、一样遵守「同一 id 对同一张表只改一次」、一样会触发
 * {@link LootInjectorApplyEvent}。
 *
 * <p>注册时用的是 id → 注入器的映射，所以**同 id 会覆盖**（脚本可以借此顶掉数据包里的某条）。
 */
public class LootInjectorRegisterEvent extends Event {

    private final HolderLookup.Provider registries;
    private final Map<ResourceLocation, LootInjector> injectors = new LinkedHashMap<>();
    private final Set<ResourceLocation> dataPackIds = new HashSet<>();

    public LootInjectorRegisterEvent(HolderLookup.Provider registries, List<LootInjector> loaded) {
        this.registries = registries;

        for (LootInjector injector : loaded) {
            this.injectors.put(injector.id(), injector);
            this.dataPackIds.add(injector.id());
        }
    }

    /** 解析注册表（附魔、属性、战利品表等）用；与选择器 / 操作符收到的是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    /** 当前全部注入器（数据包里的 + 已经注册的），顺序与注册顺序一致，还没排序。 */
    public List<LootInjector> getInjectors() {
        return List.copyOf(this.injectors.values());
    }

    /** 这个 id 是不是数据包里已经有的，用来判断自己是否会覆盖掉数据包的定义。 */
    public boolean isDataPack(ResourceLocation id) {
        return this.dataPackIds.contains(id);
    }

    /** 注册一条注入器；同 id 覆盖已有的那条。 */
    public void register(ResourceLocation id, LootInjector injector) {
        Objects.requireNonNull(id, "Loot injector id");
        Objects.requireNonNull(injector, "Loot injector");

        this.injectors.put(id, id.equals(injector.id()) ? injector : injector.withId(id));
    }

    /** 注册一条注入器，优先级用默认值 1000（与 json 的默认一致）。 */
    public void register(ResourceLocation id, TableSelector tableSelector, List<TableOperator> ops) {
        register(id, tableSelector, 1000, ops);
    }

    /** 注册一条注入器：选哪些表、什么顺序、改什么。 */
    public void register(ResourceLocation id, TableSelector tableSelector, int priority, List<TableOperator> ops) {
        Objects.requireNonNull(tableSelector, "Table selector");
        Objects.requireNonNull(ops, "Table operators");

        register(id, new LootInjector(id, tableSelector, priority, List.copyOf(ops)));
    }

    /**
     * 触发事件，并按优先级给出最终要应用的注入器列表。
     *
     * <p>{@code List.sort} 是稳定排序，所以同优先级保持注册顺序（数据包按 id 顺序在前）。
     *
     * @return 最终列表；没有任何注入器时为空表。
     */
    public static List<LootInjector> post(HolderLookup.Provider registries, List<LootInjector> loaded) {
        LootInjectorRegisterEvent event = new LootInjectorRegisterEvent(registries, loaded);
        NeoForge.EVENT_BUS.post(event);

        List<LootInjector> result = new ArrayList<>(event.injectors.values());
        result.sort(Comparator.comparingInt(LootInjector::priority));

        return result;
    }
}
