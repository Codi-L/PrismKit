package com.codi.prismkit;

import com.google.gson.JsonObject;
import org.joml.Vector2d;

public class JsonKit {
    public static JsonObject serializeVector2d(Vector2d vec) {
        JsonObject json = new JsonObject();
        json.addProperty("x", vec.x);
        json.addProperty("y", vec.y);
        return json;
    }

    public static Vector2d deserializeVector2d(JsonObject json) {
        if (!(json == null)) {
            Vector2d vec = new Vector2d();
            vec.x = json.get("x").getAsDouble();
            vec.y = json.get("y").getAsDouble();
            return vec;
        } else {
            throw new IllegalArgumentException("JSON 对象为空");
        }
    }
}
