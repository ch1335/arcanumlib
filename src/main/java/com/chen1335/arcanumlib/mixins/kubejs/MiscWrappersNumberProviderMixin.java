package com.chen1335.arcanumlib.mixins.kubejs;

import dev.latvian.mods.kubejs.plugin.builtin.wrapper.MiscWrappers;
import dev.latvian.mods.rhino.Context;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 补 KubeJS 的一个缺口：内置的 {@code NumberProvider} 包装器只认「数字 / 数字列表 / map」，
 * **不认「已经构造好的 {@code NumberProvider}」** —— 同一个文件里的 {@code wrapVec3} 有
 * {@code case Vec3 vec -> vec;} 这种幂等短路，{@code NumberProvider} 偏偏没有。
 *
 * <p>后果：脚本里把本模组 Helper 造出来的 provider 再传给别的 Helper 时会抛
 * {@code Failed to read NumberProvider from ConstantValue[...]: Expected a number, list of numbers, or a supported map format}，
 * 例如 {@code LootConditionHelper.randomChance(NumberProviderHelper.constant(0.35))}。
 *
 * <p>这里在方法入口补一个幂等短路：传进来的已经是 {@code NumberProvider} 就直接原样返回，其余分支完全不变。
 *
 * <p>{@link MiscWrappers} 本身是接口（KubeJS 拿它当静态方法命名空间），所以本 mixin 也声明为接口，
 * 让 Mixin 的子类型检查（Standard / Interface）能对上。加载由 {@code ArcanumLibMixinPlugin} 按 KubeJS 是否在场控制。
 */
@Mixin(value = MiscWrappers.class, remap = false)
public interface MiscWrappersNumberProviderMixin {

    @Inject(method = "wrapNumberProvider", at = @At("HEAD"), cancellable = true, require = 0)
    private static void arcanumlib$acceptExistingProvider(Context cx, Object value, CallbackInfoReturnable<NumberProvider> cir) {
        if (value instanceof NumberProvider provider) {
            cir.setReturnValue(provider);
        }
    }
}