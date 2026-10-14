package com.chen1335.arcanumlib.modules.holder;

import com.chen1335.arcanumlib.mixins.ItemStackAccessor;
import com.chen1335.arcanumlib.registry.ArcanumDataComponents;
import com.chen1335.arcanumlib.registry.ArcanumItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 包装了一个实体的物品：名字直接用被包装实体的本地化名称，
 * 图标由客户端自定义渲染画成那个实体（见 modules/holder/client）。
 */
public class EntityHolderItem extends Item {
    public EntityHolderItem(Item.Properties properties) {
        super(properties);
    }

    /** 有数据组件时显示实体名（苦力怕 / Creeper），没有组件就退回普通物品名。 */
    @Override
    public Component getName(ItemStack stack) {
        EntityHolderPayload payload = payloadOf(stack);

        return payload != null ? Component.translatable(payload.entity().getDescriptionId()) : super.getName(stack);
    }

    /**
     * 战利品标记：**0 数量**的包装物品。
     *
     * <p>0 数量在原版语义里就是「空」（{@code ItemStack#isEmpty()} 判 {@code count <= 0}），所以它
     * 不会被玩家看见、不会被漏斗搬走、也写不进存档；但战利品摇取的产出列表里**不会过滤空物品**，
     * 我们自己的钩子因此能读到它 —— 带组件、带数量，比布尔标记信息量更大。
     */
    public static ItemStack marker(EntityType<?> entity, CompoundTag nbt, int count) {
        ItemStack stack = new ItemStack(ArcanumItems.ENTITY_HOLDER.get(), 0);
        stack.set(ArcanumDataComponents.HELD_ENTITY.get(), new EntityHolderPayload(entity, nbt, Math.max(1, count)));

        return stack;
    }

    /** 造一个正常数量的包装物品（给 /give、数据包直接发放用）。 */
    public static ItemStack of(EntityType<?> entity, CompoundTag nbt, int count) {
        ItemStack stack = new ItemStack(ArcanumItems.ENTITY_HOLDER.get(), Math.max(1, count));
        stack.set(ArcanumDataComponents.HELD_ENTITY.get(), new EntityHolderPayload(entity, nbt, Math.max(1, count)));

        return stack;
    }

    /**
     * 判断是不是 EntityHolderItem。
     *
     * <p>**不能用 {@code stack.getItem()}**：0 数量的标记在原版眼里是空 stack，而
     * {@code getItem()} 对空 stack 返回 {@code Items.AIR}，instanceof 判断会永远为假。
     */
    public static boolean isHolder(ItemStack stack) {
        return stack != null && ((ItemStackAccessor) (Object) stack).arcanumlib$item() instanceof EntityHolderItem;
    }

    /**
     * 读取包装的实体；没包装就返回 null。
     *
     * <p>同样不能用 {@code stack.get(...)}：{@code getComponents()} 在 {@code isEmpty()} 时返回 EMPTY。
     * 两个读取入口都用 accessor 绕过。
     */
    public static @Nullable EntityHolderPayload payloadOf(ItemStack stack) {
        if (!isHolder(stack)) {
            return null;
        }

        return ((ItemStackAccessor) (Object) stack).arcanumlib$components().get(ArcanumDataComponents.HELD_ENTITY.get());
    }
}