package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorApplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 数据包 reload 完成后重新应用战利品注入：reload 会把表重新反序列化成全新实例、之前注入的内容全丢，必须重做。
 * 和 ALI 挂在同一个点（reloadResources 的 TAIL）：priority 更小就先应用；同一服务端执行器下提交顺序即执行顺序。
 */
@Mixin(value = MinecraftServer.class, priority = 900)
public class MixinMinecraftServer {

    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void arcanumlib$reapplyLootInjectors(Collection<String> packs, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        CompletableFuture<Void> future = cir.getReturnValue();
        if (future == null) {
            return;
        }

        MinecraftServer server = (MinecraftServer) (Object) this;
        future.handleAsync((value, throwable) -> {
            if (throwable == null) {
                LootInjectorApplier.applyAll(server);
            }
            return value;
        }, server);
    }
}
