package com.chen1335.arcanumlib.modules.attribute.events;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 读取属性时触发：由监听方自行决定“给哪个实体、哪条属性、调多少”。
 *
 * <p>事件本身不绑定实体与属性——一次触发里，监听方可以通过
 * {@link #getAdjustment(LivingEntity, Holder)} 给任意实体、任意多条属性分别写入调整量；
 * 调用方再按自己手里的实体去取那一条。
 */
public class LivingGetAttributeAdjustmentEvent extends Event {
    private final IContext context;

    /** 监听方写入的临时调整：实体 → 属性 → 调整；没写入的组合不会出现在里面。 */
    private final Map<LivingEntity, Map<Holder<Attribute>, AttributeAdjustment>> adjustments = new IdentityHashMap<>();

    public LivingGetAttributeAdjustmentEvent(IContext context) {
        this.context = context;
    }

    /**
     * 取（必要时创建）某实体在某属性上的调整载体：监听方用它决定“给谁、改哪条属性、改多少”。
     */
    public AttributeAdjustment getAdjustment(LivingEntity entity, Holder<Attribute> attribute) {
        return this.adjustments
                .computeIfAbsent(entity, key -> new HashMap<>())
                .computeIfAbsent(attribute, key -> new AttributeAdjustment());
    }

    /** 某实体已经被写入的调整，按属性索引；没有则返回空表。 */
    public Map<Holder<Attribute>, AttributeAdjustment> getAdjustments(LivingEntity entity) {
        return this.adjustments.getOrDefault(entity, Map.of());
    }

    /** 全部调整：实体 → 属性 → 调整（可能为空）。 */
    public Map<LivingEntity, Map<Holder<Attribute>, AttributeAdjustment>> getAdjustments() {
        return this.adjustments;
    }

    public IContext getContext() {
        return context;
    }

    /** 该属性在指定实体上的实例；没挂上这条属性时返回 null。 */
    @Nullable
    public AttributeInstance getInstance(LivingEntity entity, Holder<Attribute> attribute) {
        return entity.getAttributes().getInstance(attribute);
    }

    public interface IContext {

    }

    /** 触发事件，返回监听方写入的全部调整（可能为空）。 */
    public static Map<LivingEntity, Map<Holder<Attribute>, AttributeAdjustment>> post(IContext context) {
        return NeoForge.EVENT_BUS.post(new LivingGetAttributeAdjustmentEvent(context)).adjustments;
    }
}