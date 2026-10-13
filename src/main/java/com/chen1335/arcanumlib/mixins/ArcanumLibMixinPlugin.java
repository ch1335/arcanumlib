package com.chen1335.arcanumlib.mixins;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * mixin 加载控制器。
 *
 * <p>规则只有一条：包名形如 {@code ...mixins.<模组 id>.<Xxx>Mixin} 的，只有该模组在场才加载；
 * 直接放在 {@code ...mixins} 下的（本模组自己的补丁）一律加载。
 *
 * <p>这样针对别的模组的 mixin（例如 KubeJS 的）在对方缺失时会安静地跳过，而不是找不到目标类报错。
 */
public class ArcanumLibMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String[] parts = mixinClassName.split("\\.");
        for (int i = 0; i + 2 < parts.length; i++) {
            if (parts[i].equals("mixins")) {
                // mixins 后面还有两层以上，说明这一段是模组命名空间：该模组在场才加载
                return FMLLoader.getLoadingModList().getModFileById(parts[i + 1]) != null;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
