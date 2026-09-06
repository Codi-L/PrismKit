package com.codi.prismkit;

import com.google.gson.JsonObject;
import org.joml.Vector2d;

/**
 * PrismKit JSON 数据的通用转换工具。
 */
public final class JsonKit {
    private JsonKit() {
    }

    /**
     * 将二维向量序列化为包含 x、y 的 JSON 对象。
     */
    public static JsonObject serializeVector2d(Vector2d vec) {
        JsonObject json = new JsonObject();
        json.addProperty("x", vec.x);
        json.addProperty("y", vec.y);
        return json;
    }

    /**
     * 从 JSON 对象读取二维向量。
     *
     * @param json 包含 x、y 字段的 JSON 对象
     * @return 解析后的二维向量
     */
    public static Vector2d deserializeVector2d(JsonObject json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON 对象为空");
        }
        return new Vector2d(
                json.get("x").getAsDouble(),
                json.get("y").getAsDouble()
        );
    }
}
