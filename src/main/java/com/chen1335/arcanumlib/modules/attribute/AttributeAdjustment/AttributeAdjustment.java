package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

/**
 * 一次“临时调整”的载体：每种运算各存一个数值。
 *
 * <p>用独立字段而不是 {@code Map<Operation, MutableDouble>}：这东西每次属性重算都会读，
 * 原来的写法每次都要多分配 1 个 Map + 若干 MutableDouble，而且每次取值都是一次 HashMap 查表。
 *
 * <p>中性值与事件里的约定一致：{@code ADD_VALUE}、{@code ADD_MULTIPLIED_BASE} 与 {@code ADD_FINAL} 是 0，
 * {@code ADD_MULTIPLIED_TOTAL} 是 1（直接相乘），{@code SET_VALUE} 用 {@code NaN} 表示“不覆盖”。
 *
 * <p>五个运算均已接入：{@code ADD_FINAL} 在总倍率之后叠加，{@code SET_VALUE} 直接覆盖当前值，
 * 两者都发生在属性自身取值范围夹取之前。
 */
public final class AttributeAdjustment {
    private double addValue;
    private double addMultipliedBase;
    private double addMultipliedTotal = 1.0D;
    private double addFinal = 0;
    private double setValue = Double.NaN;

    /**
     * 读某一种运算当前的调整值。
     */
    public double get(Operation operation) {
        return switch (operation) {
            case ADD_VALUE -> this.addValue;
            case ADD_MULTIPLIED_BASE -> this.addMultipliedBase;
            case ADD_MULTIPLIED_TOTAL -> this.addMultipliedTotal;
            case ADD_FINAL -> this.addFinal;
            case SET_VALUE -> this.setValue;
        };
    }

    /**
     * 覆盖某一种运算的调整值。
     */
    public void set(Operation operation, double value) {
        switch (operation) {
            case ADD_VALUE -> this.addValue = value;
            case ADD_MULTIPLIED_BASE -> this.addMultipliedBase = value;
            case ADD_MULTIPLIED_TOTAL -> this.addMultipliedTotal = value;
            case ADD_FINAL -> this.addFinal = value;
            case SET_VALUE -> this.setValue = value;
        }
    }

    /**
     * 累加某一种运算的调整值（监听方最常用的写法）。
     */
    public void add(Operation operation, double value) {
        this.set(operation, this.get(operation) + value);
    }

    /** 属性修正支持的运算；前三个与原版 {@code AttributeModifier.Operation} 同名同义，后两个是本模组扩展的阶段。 */
    public enum Operation {
        ADD_VALUE,
        ADD_MULTIPLIED_BASE,
        ADD_MULTIPLIED_TOTAL,
        ADD_FINAL,
        SET_VALUE
    }
}