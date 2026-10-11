package com.chen1335.arcanumlib.compat.astages;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.fml.ModList;

/**
 * AStages 软依赖入口。本类**不允许**出现任何 AStages 类型：只有确认装了 astages 才会去加载
 * {@link AStagesStageLookup}（JVM 延迟解析符号引用），否则调用它就会 NoClassDefFoundError。
 */
public final class AStagesCompat {

    private static Boolean loaded;
    private static boolean warned;

    public static boolean matches(LootContext context, boolean checkServerStages, StagePattern stage) {
        if (!isLoaded()) {
            if (!warned) {
                warned = true;
                ArcanumLib.LOGGER.warn("arcanumlib:has_stage was used, but AStages is not installed - this condition will never pass");
            }

            return false;
        }

        return AStagesStageLookup.matches(context, checkServerStages, stage);
    }

    /** 第一次调用时才查 ModList：太早加载本类时 FML 可能还没就绪。 */
    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("astages");
        }

        return loaded;
    }

    private AStagesCompat() {
    }
}
