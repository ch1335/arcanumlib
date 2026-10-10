package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * 把"模板"与"候选"的 json 做子集比对，语义对齐 {@code DataComponentIngredient} 的组件匹配：
 * 模板里写出来的 key 必须在候选里存在且值一致，没写的 key 不参与。
 *
 * <p>数组按【有序参数】看待，要求长度一致、逐元素匹配——否则 {@code chances: [0.1, 0.5]} 这种
 * 会被当成无序集合，"第几个"就失去意义了。
 */
public final class JsonSubset {

    /** @param expected 模板（通常来自 json，保留原始形态） @param actual 候选序列化后的 json */
    public static boolean matches(JsonElement expected, JsonElement actual) {
        if (expected.isJsonObject()) {
            if (!actual.isJsonObject()) {
                return false;
            }

            JsonObject actualObject = actual.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : expected.getAsJsonObject().entrySet()) {
                JsonElement actualValue = actualObject.get(entry.getKey());
                if (actualValue == null || !matches(entry.getValue(), actualValue)) {
                    return false;
                }
            }

            return true;
        }

        if (expected.isJsonArray()) {
            if (!actual.isJsonArray()) {
                return false;
            }

            JsonArray expectedArray = expected.getAsJsonArray();
            JsonArray actualArray = actual.getAsJsonArray();
            if (expectedArray.size() != actualArray.size()) {
                return false;
            }

            for (int i = 0; i < expectedArray.size(); i++) {
                if (!matches(expectedArray.get(i), actualArray.get(i))) {
                    return false;
                }
            }

            return true;
        }

        return expected.equals(actual);
    }

    private JsonSubset() {
    }
}
