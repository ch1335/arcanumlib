package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 读 {@code data/<ns>/loot_injecter/*.json}，但只存原始 json、不在这里解析。
 * 条件/函数要解析注册表引用，而本方法跑在 reload 阶段、那时 ServerLifecycleHooks 还是 null（实测 NPE），所以解析推迟到 applier。
 */
public class LootInjectorReloadListener extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "loot_injecter";
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, JsonElement> rawJson = Map.of();

    public LootInjectorReloadListener() {
        super(GSON, DIRECTORY);
    }

    public static Map<ResourceLocation, JsonElement> rawJson() {
        return rawJson;
    }

    /** 解析完就没用了：JsonElement 树是这里唯一的大块内存，reload 时会重新填上。 */
    public static void clearRaw() {
        rawJson = Map.of();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        rawJson = Map.copyOf(objects);

        // 标签与表实例都会在 reload 后变化，缓存和"已应用"记录必须一起清掉
        TableCondition.clearCaches();
        LootInjectorApplier.clearApplied();
    }
}
