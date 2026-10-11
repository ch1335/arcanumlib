package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorApplyEvent;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 解析注入器定义并应用到战利品表上。时机在服务端启动 / 数据包 reload 之后，那时 registry access 一定可用。 */
public final class LootInjectorApplier {

    /** 已应用记录：表实例 → 已经改过它的注入器 id。LootTable 没重写 equals，所以按实例身份比较；reload 会产生新实例。 */
    private static final Map<LootTable, Set<ResourceLocation>> APPLIED = new HashMap<>();

    private LootInjectorApplier() {
    }

    public static void clearApplied() {
        APPLIED.clear();
    }

    /** 已经至少被一个注入器作用过的表（按实例身份，reload 后重新计）。 */
    public static Set<LootTable> appliedTables() {
        return Set.copyOf(APPLIED.keySet());
    }

    public static void applyAll(MinecraftServer server) {
        List<LootInjector> injectors = parse(server);

        // 解析完 JsonElement 树就没用了；它是这里唯一的大块内存，reload 时会重新填
        LootInjectorReloadListener.clearRaw();

        if (injectors.isEmpty()) {
            return;
        }

        // 口径与 ALI 一致：Holder.get() 才是 HolderLookup.Provider，lookup 返回 Optional
        HolderLookup.RegistryLookup<LootTable> lookup = server.reloadableRegistries().get()
                .lookup(Registries.LOOT_TABLE).orElse(null);
        if (lookup == null) {
            ArcanumLib.LOGGER.error("Cannot resolve the loot table registry, skipping loot injection");
            return;
        }

        // op 可能要读注册表（ReplaceEntry 靠 codec 往返造新实例），一次性取好传下去
        HolderLookup.Provider registries = server.registryAccess();

        long start = System.nanoTime();
        int tables = 0;
        int applications = 0;

        for (Holder.Reference<LootTable> holder : lookup.listElements().toList()) {
            LootTable table = holder.value();
            tables++;

            for (LootInjector injector : injectors) {
                if (!injector.tableSelector().test(holder.key().location(), table, registries)) {
                    continue;
                }

                if (!APPLIED.computeIfAbsent(table, key -> new HashSet<>()).add(injector.id())) {
                    continue;
                }

                Optional<LootInjector> effective = LootInjectorApplyEvent.post(injector, holder.key().location(), table, registries);
                if (effective.isEmpty()) {
                    continue;
                }

                effective.get().ops().forEach(op -> op.apply(table, registries));
                applications++;
            }
        }

        ArcanumLib.LOGGER.info("Loot injection: {} injectors, {} loot tables, {} applications in {} ms",
                injectors.size(), tables, applications, (System.nanoTime() - start) / 1000000.0D);
    }

    /** 解析推迟到这里：只有这时才拿得到可用的 registry access。 */
    private static List<LootInjector> parse(MinecraftServer server) {
        Map<ResourceLocation, JsonElement> raw = LootInjectorReloadListener.rawJson();
        if (raw.isEmpty()) {
            return List.of();
        }

        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
        List<LootInjector> parsed = new ArrayList<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : raw.entrySet()) {
            LootInjector.CODEC.parse(ops, entry.getValue())
                    .resultOrPartial(error -> ArcanumLib.LOGGER.error(
                            "Failed to parse loot injector {}: {}", entry.getKey(), error))
                    .ifPresent(injector -> parsed.add(injector.withId(entry.getKey())));
        }

        parsed.sort(Comparator.comparingInt(LootInjector::priority));

        return parsed;
    }
}
