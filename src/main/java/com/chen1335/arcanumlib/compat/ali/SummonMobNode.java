package com.chen1335.arcanumlib.compat.ali;

import com.chen1335.arcanumlib.ArcanumLib;
import com.yanny.aci.api.NumberExpr;
import com.yanny.aci.tooltip.TooltipNode;
import com.yanny.ali.api.IClientUtils;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.IServerUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

/**
 * summon_mob 的 ALI 节点：不带物品，只带"要渲染的实体"和"数量"，让客户端把条目直接画成实体。
 * 节点必须自带编码，否则客户端的 {@code registerDataNode} 认不出来。
 */
public class SummonMobNode implements IDataNode {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArcanumLib.MODID, "summon_mob_entry");

    private final EntityType<?> mob;
    private final NumberExpr count;
    private final TooltipNode tooltip;
    private final float chance;

    public SummonMobNode(EntityType<?> mob, NumberExpr count, TooltipNode tooltip, float chance) {
        this.mob = mob;
        this.count = count;
        this.tooltip = tooltip;
        this.chance = chance;
    }

    public SummonMobNode(IClientUtils utils, RegistryFriendlyByteBuf buf) {
        this.mob = BuiltInRegistries.ENTITY_TYPE.get(buf.readResourceLocation());
        this.count = NumberExpr.decode(buf);
        this.tooltip = utils.getTooltipCache().getNodeById(buf.readVarInt());
        this.chance = buf.readFloat();
    }

    /** 要渲染的实体类型。 */
    public EntityType<?> getMob() {
        return this.mob;
    }

    /** 生成数量。 */
    public NumberExpr getCount() {
        return this.count;
    }

    @Override
    public void encode(IServerUtils utils, RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(BuiltInRegistries.ENTITY_TYPE.getKey(this.mob));
        this.count.encode(buf);
        buf.writeVarInt(utils.getTooltipCache().getNodeId(this.tooltip));
        buf.writeFloat(this.chance);
    }

    @Override
    @NotNull
    public TooltipNode getTooltip() {
        return this.tooltip;
    }

    @Override
    @NotNull
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public float getChance() {
        return this.chance;
    }
}