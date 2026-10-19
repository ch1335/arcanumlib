package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import java.util.function.ToDoubleBiFunction;

/**
 * 一次“临时调整”的载体：每种运算各存一个数值。
 *
 * <p>用独立字段而不是 {@code Map<Operation, MutableDouble>}：这东西每次属性重算都会读，
 * 原来的写法每次都要多分配 1 个 Map + 若干 MutableDouble，而且每次取值都是一次 HashMap 查表。
 *
 * <p>中性值与事件里的约定一致：{@code ADD_VALUE}、{@code ADD_MULTIPLIED_BASE}、{@code ADD_MULTIPLIED_TOTAL}
 * 与 {@code ADD_FINAL} 都是 0；{@code ADD_MULTIPLIED_TOTAL} 的数值按分数合并（{@code 0.5} 即 +50%），
 * {@code SET_VALUE} 用 {@code NaN} 表示“不覆盖”。
 *
 * <p>五个运算均已接入：{@code ADD_FINAL} 在总倍率之后叠加，{@code SET_VALUE} 直接覆盖当前值，
 * 两者都发生在属性自身取值范围夹取之前。
 */
public final class AttributeAdjustment {
    private double addValue = 0;
    private double addMultipliedBase = 0;
    private double addMultipliedTotal = 0D;
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
        this.set(operation, operation.get(this.get(operation), value));
    }

    /**
     * 属性修正支持的运算；前三个与原版 {@code AttributeModifier.Operation} 同名同义，后两个是本模组扩展的阶段。
     */
    public enum Operation {
        ADD_VALUE(Double::sum),
        ADD_MULTIPLIED_BASE(Double::sum),
        ADD_MULTIPLIED_TOTAL((d1, d2) -> (1 + d1) * (1 + d2) - 1),
        ADD_FINAL(Double::sum),
        SET_VALUE((d1, d2) -> d2);
        private final ToDoubleBiFunction<Double, Double> function;

        Operation(ToDoubleBiFunction<Double, Double> function) {
            this.function = function;
        }

        public double get(double a, double b) {
            return function.applyAsDouble(a, b);
        }
    }
}