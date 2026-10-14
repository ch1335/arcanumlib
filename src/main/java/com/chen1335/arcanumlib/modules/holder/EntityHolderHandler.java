package com.chen1335.arcanumlib.modules.holder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 把 EntityHolderPayload 变成实体，生成过程对齐 /summon：把实体 id 写进 nbt 副本交给
 * {@link EntityType#loadEntityRecursive}，位置统一改成传入坐标；没给 nbt 时再补一次
 * {@code EventHooks.finalizeMobSpawn}（等价于 /summon 的 randomizeProperties）。
 */
public final class EntityHolderHandler {
    /** 单次最多 64 只，防止配置写飞。 */
    private static final int MAX_COUNT = 64;

    private EntityHolderHandler() {
    }

    public static void spawn(ServerLevel level, EntityHolderPayload payload, Vec3 origin) {
        spawn(level, payload, origin, Vec3.ZERO);
    }

    /**
     * @param motion 继承自 ItemEntity 的动量；长度接近 0 时不覆盖（保留 nbt 里可能写的 Motion）
     */
    public static void spawn(ServerLevel level, EntityHolderPayload payload, Vec3 origin, Vec3 motion) {
        for (int i = 0; i < Math.min(Math.max(1, payload.count()), MAX_COUNT); i++) {
            spawnOne(level, payload, origin, motion);
        }
    }

    private static void spawnOne(ServerLevel level, EntityHolderPayload payload, Vec3 origin, Vec3 motion) {
        BlockPos pos = BlockPos.containing(origin);
        if (!Level.isInSpawnableBounds(pos)) {
            return;
        }

        CompoundTag tag = payload.nbt().copy();
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(payload.entity()).toString());

        Entity entity = EntityType.loadEntityRecursive(tag, level, created -> {
            created.moveTo(origin.x, origin.y, origin.z, created.getYRot(), created.getXRot());
            return created;
        });
        if (entity == null) {
            return;
        }

        // 继承 ItemEntity 的动量：掉落物被拒之后，实体应当沿着同样的轨迹飞出去。
        // 长度接近 0 就不覆盖，免得把 nbt 里写好的 Motion 抹成 0。
        if (motion.lengthSqr() > 1.0E-6D) {
            entity.setDeltaMovement(motion);
            entity.hurtMarked = true;
        }

        // /summon 的 randomizeProperties：没给 nbt 时才补 finalizeSpawn
        if (payload.nbt().isEmpty() && entity instanceof Mob mob) {
            EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()),
                    MobSpawnType.COMMAND, null);
        }

        level.addFreshEntityWithPassengers(entity);
    }
}