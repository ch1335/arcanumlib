package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 一次"临时调整"的载体：三种运算各存一个数值。
 *
 * <p>用三个 double 字段而不是 {@code Map<Operation, MutableDouble>}：这东西每次属性重算都会读，
 * 原来的写法每次都要多分配 1 个 Map + 3 个 MutableDouble，而且每次取值都是一次 HashMap 查表。
 *
 * <p>中性值与事件里的约定一致：{@code ADD_VALUE} 与 {@code ADD_MULTIPLIED_BASE} 是 0，
 * {@code ADD_MULTIPLIED_TOTAL} 是 1（因为它是直接相乘）。
 */
public final class AttributeAdjustment {
    private double addValue;
    private double addMultipliedBase;
    private double addMultipliedTotal = 1.0D;

    /** 读某一种运算当前的调整值。 */
    public double get(AttributeModifier.Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> this.addValue;
            case ADD_MULTIPLIED_BASE -> this.addMultipliedBase;
            case ADD_MULTIPLIED_TOTAL -> this.addMultipliedTotal;
        };
    }

    /** 覆盖某一种运算的调整值。 */
    public void set(AttributeModifier.Operation operation, double value) {
        switch (operation) {
            case ADD_VALUE -> this.addValue = value;
            case ADD_MULTIPLIED_BASE -> this.addMultipliedBase = value;
            case ADD_MULTIPLIED_TOTAL -> this.addMultipliedTotal = value;
        }
    }

    /** 累加某一种运算的调整值（监听方最常用的写法）。 */
    public void add(AttributeModifier.Operation operation, double value) {
        this.set(operation, this.get(operation) + value);
    }
}