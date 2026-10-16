package com.chen1335.arcanumlib.mixins.apothic_attributes;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.AttackingContext;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让 ApothicAttributes 的暴击结算里的属性读取同样吃属性修正。
 *
 * <p>{@code apothCriticalStrike} 读过两个属性（暴击几率、暴击伤害），都是攻击方身上的，
 * 因此上下文用 {@link DamageContext}：受伤方、伤害来源与本次伤害数值都能从事件里拿到。
 *
 * <p>有两点与 {@link ALCombatRulesMixin} 不同，最容易写错：
 * <ul>
 *   <li>目标是<strong>实例方法</strong>，所以这里的 handler 必须是实例方法；那边是静态方法，handler 必须 static；</li>
 *   <li>事件本身需要用到（受伤方 / 伤害来源 / 伤害数值），用 {@code @Local(argsOnly = true)} 取参数，
 *       不要去猜局部变量名。</li>
 * </ul>
 *
 * <p>锚点：字节码里这两处调用的 owner 是局部变量声明的静态类型 {@code LivingEntity}
 * （javap 已确认），与 {@link ALCombatRulesMixin} 一致。
 *
 * <p>同一个文件里的 {@code vanillaCritDmg}（原版暴击判定）也读暴击伤害，已一并接入：该处上下文用 {@link AttackingContext}。
 */
@Debug(export = true)
@Mixin(AttributeEvents.class)
public class AttributeEventsMixin {

    @WrapOperation(
            method = "apothCriticalStrike",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
    )
    private double arcanumlib$adjustCritAttributes(LivingEntity instance, Holder<Attribute> attribute, Operation<Double> original,
                                                   @Local(argsOnly = true) LivingIncomingDamageEvent event) {
        return AdjustmentHelper.getAttributeWithAdjusted(instance, attribute, () -> original.call(instance, attribute),
                new DamageContext(event.getEntity(), event.getSource(), event.getAmount()));
    }

    /**
     * 原版暴击判定（{@code CriticalHitEvent}）里同样会读暴击伤害，这里用 {@link AttackingContext}。
     *
     * <p>该处<strong>只能</strong>用 {@link AttackingContext}：{@code CriticalHitEvent} 只提供攻击者
     * （{@code getEntity()}）与被攻击目标（{@code getTarget()}），没有伤害来源与伤害数值，
     * 而 {@link DamageContext} 需要 {@code (target, source, damage)} 三样，构造不出来。
     *
     * <p>注意 owner 与上面那处<strong>不同</strong>：这条语句是
     * {@code e.getEntity().getAttributeValue(...)}，{@code getEntity()} 的静态类型是 {@link Player}，
     * 所以字节码里是 {@code invokevirtual Player.getAttributeValue:(...Holder;)D}（javap 已确认）；
     * 写成 {@code LivingEntity} 会一个注入点都匹配不到。
     */
    @WrapOperation(
            method = "vanillaCritDmg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
    )
    private double arcanumlib$adjustVanillaCritDamage(Player instance, Holder<Attribute> attribute, Operation<Double> original,
                                                      @Local(argsOnly = true) CriticalHitEvent event) {
        return AdjustmentHelper.getAttributeWithAdjusted(instance, attribute, () -> original.call(instance, attribute),
                new AttackingContext(instance, event.getTarget()));
    }

    /**
     * 投掷物伤害（{@code projDmg}）读的是发射者的投射物伤害属性，上下文用 {@link DamageContext}
     * （受伤方、伤害来源与本次伤害数值都能从事件里拿到，与其上方的 {@code apothCriticalStrike} 一致）。
     *
     * <p>锚点：该语句是 {@code projOwner.getAttributeValue(...)}，{@code projOwner} 声明为
     * {@code LivingEntity}，所以 owner 与 {@code apothCriticalStrike} 那处相同（javap 已确认该方法里只有这一处读取）。
     */
    @WrapOperation(
            method = "projDmg",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
    )
    private double arcanumlib$adjustProjectileDamage(LivingEntity instance, Holder<Attribute> attribute, Operation<Double> original,
                                                     @Local(argsOnly = true) LivingIncomingDamageEvent event) {
        return AdjustmentHelper.getAttributeWithAdjusted(instance, attribute, () -> original.call(instance, attribute),
                new DamageContext(event.getEntity(), event.getSource(), event.getAmount()));
    }
}