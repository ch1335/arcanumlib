package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorRegisterEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 脚本事件 {@code ArcanumLibEvents.registerLootInjector}：在运行时注册注入器。
 *
 * <p>触发时数据包 json 已经解析完、还没应用到战利品表，所以这里注册的注入器与 json 里的一视同仁。
 * 注册对象本身也可以直接写 json（{@link LootInjector} 的 codec 已注册为类型包装），
 * 里面的选择器 / 操作同理：直接写 {@code {type: '...', ...}} 即可。
 */
public class RegisterLootInjectorKubeEvent implements KubeEvent {

    private final LootInjectorRegisterEvent event;

    public RegisterLootInjectorKubeEvent(LootInjectorRegisterEvent event) {
        this.event = event;
    }

    /** 解析注册表（附魔、属性、战利品表等）用，与 Java 侧事件收到的是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.event.getRegistries();
    }

    /** 当前全部注入器（数据包里的 + 已经注册的）。 */
    public List<LootInjector> getInjectors() {
        return this.event.getInjectors();
    }

    /** 这个 id 是不是数据包里已经有的，用来判断自己是否会覆盖掉数据包的定义。 */
    public boolean isDataPack(ResourceLocation id) {
        return this.event.isDataPack(id);
    }

    /** 注册一条注入器：选哪些表、什么优先级、改什么。优先级用默认值 1000（与 json 默认一致）。 */
    public LootInjector register(ResourceLocation id, TableSelector tableSelector, List<TableOperator> ops) {
        return register(id, tableSelector, 1000, ops);
    }

    /** 注册一条注入器：选哪些表、什么优先级、改什么。 */
    public LootInjector register(ResourceLocation id, TableSelector tableSelector, int priority, List<TableOperator> ops) {
        this.event.register(id, tableSelector, priority, ops);

        return new LootInjector(id, tableSelector, priority, List.copyOf(ops));
    }

    /** 注册一条现成的注入器（同 id 覆盖数据包里的那条），返回真正生效的那一份。 */
    public LootInjector register(ResourceLocation id, LootInjector injector) {
        this.event.register(id, injector);

        return id.equals(injector.id()) ? injector : injector.withId(id);
    }
}
