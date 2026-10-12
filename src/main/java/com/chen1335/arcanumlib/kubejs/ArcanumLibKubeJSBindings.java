package com.chen1335.arcanumlib.kubejs;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;

/**
 * 脚本里的 {@code ArcanumLib} 绑定（只读辅助）。
 *
 * <p>用静态方法暴露：脚本侧 {@code ArcanumLib.id("foo")} / {@code ArcanumLib.lootTable("minecraft:blocks/oak_leaves")}。
 * 取表相关的方法在服务端 / 启动脚本里可用，客户端阶段会返回 null。
 */
public final class ArcanumLibKubeJSBindings {

    private ArcanumLibKubeJSBindings() {
    }

    /** 本模组 id。 */
    public static String modId() {
        return ArcanumLib.MODID;
    }

    /** 拼一个属于本模组的 id：{@code ArcanumLib.id("common_chests")}。 */
    public static ResourceLocation id(String path) {
        return ArcanumLib.id(path);
    }

    /** 按 id 取战利品表；表不存在或当前没有服务端时返回 null。 */
    public static LootTable lootTable(String tableId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return null;
        }

        return server.reloadableRegistries().get()
                .registryOrThrow(Registries.LOOT_TABLE)
                .get(ResourceLocation.parse(tableId));
    }

    /** 当前已加载的全部战利品表 id（形如 {@code minecraft:blocks/oak_leaves}）。 */
    public static List<String> lootTableIds() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return List.of();
        }

        return server.reloadableRegistries().get()
                .registryOrThrow(Registries.LOOT_TABLE)
                .keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }
}