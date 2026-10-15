package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IAttributeMapExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Shadow
    @Final
    private AttributeMap attributes;

    /**
     * 在 {@code this.attributes = new AttributeMap(DefaultAttributes.getSupplier(entityType));} 之后，
     * 立刻把这张属性表的 owner 指回实体自己。
     *
     * <p>锚点用 {@code attributes} 字段的 {@code PUTFIELD} 而不是 {@code AttributeMap.<init>} 的 INVOKE：
     * 字节码顺序是 new → dup → invokespecial &lt;init&gt; → putfield，
     * 若在 INVOKE 之后就取 {@code this.attributes}，此时字段还是 null，会直接 NPE。
     */
    @Inject(
            method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/world/entity/LivingEntity;attributes:Lnet/minecraft/world/entity/ai/attributes/AttributeMap;",
                    shift = At.Shift.AFTER
            )
    )
    private void init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        ((IAttributeMapExtension) this.attributes).arcanumlib$setOwner((LivingEntity) (Object) this);
    }

    /**
     * 护甲与护甲韧性走同一套临时调整：实现见 {@link AttributeHooks#beforeGetArmorAndToughness}，
     * 这里只负责包住 {@code getDamageAfterArmorAbsorb} 里那一次 {@code CombatRules.getDamageAfterAbsorb(...)} 调用。
     *
     * <p>两个要点：
     * <ul>
     *   <li>调整必须"注册进属性表"再读属性，所以顺序是：先 {@code post} 让监听方填值 → 注册 → 再调用原逻辑；</li>
     *   <li>原版的两个实参（护甲值、护甲韧性）在调用前就已经求值完毕，所以这里必须**重新读一次**
     *       {@code getArmorValue()} 与 {@code getAttributeValue(ARMOR_TOUGHNESS)} 再传给 {@code original.call}，
     *       否则临时调整根本进不去；</li>
     * </ul>
     *
     * <p>弹出用按位置 {@code removeLast()}（和 {@code AttributeEvents} 一致）：按值移除在重入时会删错那一条。
     * 锚点在那次调用上，因此 {@code BYPASSES_ARMOR} 的伤害不会触发本逻辑。
     */
    @WrapOperation(
            method = "getDamageAfterArmorAbsorb",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"
            )
    )
    private float arcanumlib$adjustArmorAndToughness(LivingEntity entity, float damage, DamageSource source,
                                                     float armorValue, float armorToughness, Operation<Float> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        AttributeHooks.beforeGetArmorAndToughness(self, source);
        try {
            // 必须重新读一次：原版这两个实参在调用前就求值完了，直接透传的话临时调整进不去
            return original.call(entity, damage, source,
                    (float) self.getArmorValue(),
                    (float) self.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        } finally {
            AttributeHooks.afterGetArmorAndToughness(self);
        }
    }

}
