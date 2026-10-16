package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IArcanumLibLivingEntityExtension;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeMapExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
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
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IArcanumLibLivingEntityExtension {
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
        DamageContext context = new DamageContext(self, source);
        // 必须重新读一次：原版这两个实参在调用前就求值完了，直接透传的话临时调整进不去。
        // 护甲保留原版的 (int) 截断（原版传的是 int getArmorValue()）；想让护甲吃小数就把 (int) 去掉。
        return original.call(entity, damage, source,
                (float) (int) AdjustmentHelper.getAttributeWithAdjusted(self, Attributes.ARMOR, context),
                (float) AdjustmentHelper.getAttributeWithAdjusted(self, Attributes.ARMOR_TOUGHNESS, context));
    }

    @Invoker("dropCustomDeathLoot")
    @Override
    public abstract void arcanumlib$dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit);


    @Accessor("lastHurtByPlayerTime")
    @Override
    public abstract int arcanumlib$lastHurtByPlayerTime();

}