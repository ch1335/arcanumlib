package com.chen1335.arcanumlib.data;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.registry.ArcanumTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * 生成本模组的属性标签。
 *
 * <p>目前只有 {@code arcanumlib:ignore_attribute_multiplier}：打上它的属性不参与「全属性修正」。
 */
public class ArcanumAttributeTagsProvider extends IntrinsicHolderTagsProvider<Attribute> {

    public ArcanumAttributeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                        ExistingFileHelper existingFileHelper) {
        super(output, Registries.ATTRIBUTE, lookupProvider,
                attribute -> BuiltInRegistries.ATTRIBUTE.getResourceKey(attribute).orElseThrow(),
                ArcanumLib.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 移动速度与跳跃高度不参与全属性修正：这两条一旦被放大，走位与跳跃手感会直接失控
        this.tag(ArcanumTags.Attribute.IGNORE_ATTRIBUTE_MULTIPLIER)
                .add(Attributes.MOVEMENT_SPEED.value())
                .add(Attributes.JUMP_STRENGTH.value());
    }
}