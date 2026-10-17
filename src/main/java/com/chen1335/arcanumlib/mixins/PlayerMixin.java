package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentScope;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.AttackingContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 攻击流程的调整作用域：在 {@code Player#attack(Entity)} 开头触发事件（{@link AttackingContext}），
 * 返回时移除调整。
 *
 * <p>因此这次攻击里读到的属性（攻击伤害、暴击相关等）都会带上监听方给出的调整。
 * 用栈而不是单个字段：攻击可能因模组逻辑重入，内外两层要各自弹出。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    /** 本次攻击期间打开的调整作用域；重入时按栈弹出。 */
    @Unique
    private final Deque<AdjustmentScope> arcanumlib$attackScopes = new ArrayDeque<>();

    /** 攻击开始时打开作用域：整段 {@code attack} 里读到的属性都会带上监听方的调整。 */
    @Inject(method = "attack", at = @At("HEAD"))
    private void arcanumlib$openAttackAdjustments(Entity target, CallbackInfo ci) {
        this.arcanumlib$attackScopes.push(AdjustmentScope.open(
                new AttackingContext((Player) (Object) this, target)));
    }

    /** 攻击返回时关闭作用域：弹出最内层那一个，按引用移除本次挂上的全部调整。 */
    @Inject(method = "attack", at = @At("RETURN"))
    private void arcanumlib$closeAttackAdjustments(Entity target, CallbackInfo ci) {
        if (!this.arcanumlib$attackScopes.isEmpty()) {
            this.arcanumlib$attackScopes.pop().close();
        }
    }
}