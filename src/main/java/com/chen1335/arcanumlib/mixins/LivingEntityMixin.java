package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IAttributeMapExtension;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
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

}
