package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;


public final class FishingHookData implements INBTSerializable<CompoundTag> {
    private ItemStack rod = ItemStack.EMPTY;
    private ItemStack bait = ItemStack.EMPTY;
    private boolean modified = false;

    public FishingHookData() {

    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put("rod", rod.saveOptional(provider));
        compoundTag.put("bait", bait.saveOptional(provider));
        compoundTag.putBoolean("modified", modified);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        rod = ItemStack.parseOptional(provider, nbt.getCompound("rod"));
        bait = ItemStack.parseOptional(provider, nbt.getCompound("bait"));
        modified = nbt.getBoolean("modified");
    }

    public ItemStack getRod() {
        return rod;
    }

    public void setRod(ItemStack rod) {
        this.rod = rod;
    }

    public ItemStack getBait() {
        return bait;
    }

    public void setBait(ItemStack bait) {
        this.bait = bait;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}