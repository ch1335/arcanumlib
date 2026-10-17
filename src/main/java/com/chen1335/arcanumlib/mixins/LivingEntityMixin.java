package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IArcanumLibLivingEntityExtension;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeMapExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentScope;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.LivingDamageContext;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.LivingHurtContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

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



    /** 本次 {@code hurt} 期间打开的调整作用域（上下文 {@link LivingHurtContext}）；RETURN 时关闭。 */
    @Unique
    @Nullable
    private AdjustmentScope arcanumlib$hurtScope = null;

    /**
     * 在 {@code hurt} 开头打开调整作用域：整段受伤流程里读到的属性都会带上监听方给出的调整。
     *
     * <p>与下面 {@code actuallyHurt} 那个作用域是**两段**而非二选一：本段用 {@link LivingHurtContext}，
     * 覆盖受伤流程前半段（无敌帧、闪避、盾牌、以及没进入伤害结算就返回的路径）；伤害结算那段用
     * {@link LivingDamageContext}。监听方按上下文区分，不要在两个上下文里重复加同一份调整。
     *
     * <p>作用域挂在实体自身上，所以同一实体若被重入（mod 在受伤回调里再次调用 {@code hurt}），
     * 会先把上一次的作用域关掉，避免残留的调整一直留在属性表里。
     */
    @Inject(method = "hurt", at = @At("HEAD"))
    private void arcanumlib$openHurtAdjustments(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.arcanumlib$hurtScope != null) {
            this.arcanumlib$hurtScope.close();
        }

        this.arcanumlib$hurtScope = AdjustmentScope.open(
                new LivingHurtContext((LivingEntity) (Object) this, source, amount));
    }

    /** 在 {@code hurt} 返回时关闭作用域：按引用移除本次挂上的全部调整。 */
    @Inject(method = "hurt", at = @At("RETURN"))
    private void arcanumlib$closeHurtAdjustments(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.arcanumlib$hurtScope != null) {
            this.arcanumlib$hurtScope.close();
            this.arcanumlib$hurtScope = null;
        }
    }

    /**
     * 在 {@code hurt} 调用 {@code actuallyHurt} 的前后套一个调整作用域：真正结算伤害这一段里读到的属性
     * 都会带上监听方给出的调整，上下文为 {@link LivingDamageContext}。
     *
     * <p>只包 {@code actuallyHurt} 而不是整个 {@code hurt}：受伤流程前半段（无敌帧、闪避、盾牌判定等）
     * 并不结算伤害，没必要触发事件；{@code hurt} 里有两处调用，都会被这同一个钩子包住。
     */
    @WrapOperation(method = "hurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arcanumlib$wrapActuallyHurt(LivingEntity instance, DamageSource source, float amount, Operation<Void> original) {
        try (AdjustmentScope scope = AdjustmentScope.open(new LivingDamageContext(instance, source, amount))) {
            original.call(instance, source, amount);
        }
    }

    @Invoker("dropCustomDeathLoot")
    @Override
    public abstract void arcanumlib$dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit);


    @Accessor("lastHurtByPlayerTime")
    @Override
    public abstract int arcanumlib$lastHurtByPlayerTime();

}
