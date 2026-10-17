package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.ILootContextExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentScope;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.LootInfoContext;
import com.chen1335.arcanumlib.modules.holder.EntityHolderHandler;
import com.chen1335.arcanumlib.modules.holder.EntityHolderItem;
import com.chen1335.arcanumlib.modules.holder.EntityHolderPayload;
import com.chen1335.arcanumlib.modules.loot.bonusLoot.BonusLoot;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * 给符合条件的战利品表追加额外掉落，逻辑见 {@link BonusLoot}。
 */
@Mixin(LootTable.class)
@Debug(export = true)
public abstract class LootTableMixin {

    /**
     * 本次战利品结算期间打开的调整作用域；表里引用表（嵌套结算）时按栈弹出。
     */
    @Unique
    private final Deque<AdjustmentScope> arcanumlib$lootScopes = new ArrayDeque<>();

    /**
     * 在 {@code getRandomItems(LootContext)} 开头打开调整作用域：本次战利品结算里读到的属性
     * （时运、抢夺、额外战利品等）都会带上监听方给出的调整，上下文为 {@link LootInfoContext}。
     *
     * <p>用栈而不是单个字段：战利品表可以引用别的表，嵌套调用时内外两层作用域要各自弹出。
     */
    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("HEAD")
    )
    private void arcanumlib$openLootAdjustments(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        this.arcanumlib$lootScopes.push(AdjustmentScope.open(new LootInfoContext(context, ((ILootContextExtension) context).arcanumlib$getParamSet())));
    }

    /**
     * 返回时关闭作用域：弹出最内层那一个，按引用移除本次挂上的全部调整。
     */
    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            order = 1100
    )
    private void arcanumlib$closeLootAdjustments(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (!this.arcanumlib$lootScopes.isEmpty()) {
            this.arcanumlib$lootScopes.pop().close();
        }
    }


    @ModifyReturnValue(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN")
    )
    private ObjectArrayList<ItemStack> arcanumlib$bonusLoot(ObjectArrayList<ItemStack> original, LootContext context) {
        return BonusLoot.append((LootTable) (Object) this, original, context);
    }


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
