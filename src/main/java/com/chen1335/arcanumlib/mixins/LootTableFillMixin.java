package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.holder.EntityHolderHandler;
import com.chen1335.arcanumlib.modules.holder.EntityHolderItem;
import com.chen1335.arcanumlib.modules.holder.EntityHolderPayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Iterator;

/**
 * **只作用于 {@code LootTable#fill}**：把这一次摇取结果里的 EntityHolderItem 摘掉，
 * 改为就地生成它包装的实体。
 *
 * <p>用 {@code @WrapOperation} 包 {@code fill} 内部那一次 {@code getRandomItems(LootContext)} 调用，
 * 于是拿到的是"洗牌之前"的列表 —— {@code shuffleAndSplitItems} 会把空物品直接摘掉（holder 恒为
 * {@code isEmpty()}），锚在它之后就拿不到了；同时因为是包调用点，其它摇取路径完全不受影响。
 *
 * <p>生成位置取容器自己的位置（方块容器＝方块中心，容器实体＝实体位置），与开箱上下文的
 * {@code ORIGIN} 一致。这条路径没有 {@code ItemEntity}，所以不继承动量。
 */
@Mixin(LootTable.class)
public class LootTableFillMixin {
    @WrapOperation(
            method = "fill",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
            )
    )
    private ObjectArrayList<ItemStack> arcanumlib$extractEntityHolders(LootTable table, LootContext context,
                                                                       Operation<ObjectArrayList<ItemStack>> original) {
        ObjectArrayList<ItemStack> generated = original.call(table, context);
        if (generated.isEmpty()) {
            return generated;
        }

        Iterator<ItemStack> iterator = generated.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            if (!EntityHolderItem.isHolder(stack)) {
                continue;
            }

            // 先摘掉：它不参与接下来的洗牌 / 拆分，也不占槽位
            iterator.remove();

            EntityHolderPayload payload = EntityHolderItem.payloadOf(stack);
            Vec3 paramOrNull = context.getParamOrNull(LootContextParams.ORIGIN);
            if (payload != null && paramOrNull != null) {
                EntityHolderHandler.spawn(context.getLevel(), payload, paramOrNull.add(0, 0.5, 0));
            }
        }

        return generated;
    }
}