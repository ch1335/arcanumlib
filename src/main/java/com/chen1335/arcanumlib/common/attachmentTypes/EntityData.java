package com.chen1335.arcanumlib.common.attachmentTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

public class EntityData implements INBTSerializable<CompoundTag> {
    @Nullable
    public IFishingHook fishing;

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {

    }


    public static EntityData get(Entity entity){
        return entity.getData(ArcanumAttachments.PLAYER_DATA);
    }
}
