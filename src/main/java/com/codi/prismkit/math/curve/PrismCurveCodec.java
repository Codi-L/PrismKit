package com.codi.prismkit.math.curve;

import com.codi.prismkit.JsonKit;
import com.codi.prismkit.PrismKit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.joml.Vector2d;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PrismCurve 的 JSON 编解码器。
 * 使用 pivot_points 数组保存枢纽点，并根据点模式保存所需的切线数据。
 *
 * 设计意图：
 * - JSON 结构与当前枢纽点曲线模型保持一致
 * - SMOOTH 模式只保存一侧切线，另一侧在加载时自动镜像
 * - LINEAR 模式不保存切线，SPLIT 模式分别保存两侧切线
 * - 文件保持可读，便于数据包和配置目录手动编辑
 */
public class PrismCurveCodec implements JsonSerializer<PrismCurve>, JsonDeserializer<PrismCurve> {

    /**
     * 将曲线序列化为枢纽点 JSON。
     *
     * @param curve 待序列化的曲线
     * @param typeOfSrc Gson 提供的源类型
     * @param context Gson 序列化上下文
     * @return 曲线对应的 JSON 对象
     */
    @Override
    public JsonElement serialize(PrismCurve curve, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("curve_name", curve.getName());

        JsonArray pivotPointArray = new JsonArray();
        for (CurvePivotPoint pivotPoint : curve.getPivotPoints()) {
            pivotPointArray.add(serializePivotPoint(pivotPoint));
        }
        json.add("pivot_points", pivotPointArray);
        json.addProperty("clamp_mode", curve.getClampMode().name());

        return json;
    }

    /**
     * 从枢纽点 JSON 构建曲线。
     *
     * @param json 曲线 JSON
     * @param typeOfT Gson 提供的目标类型
     * @param context Gson 反序列化上下文
     * @return 解析后的曲线
     * @throws JsonParseException JSON 结构或枚举值无效时抛出
     */
    @Override
    public PrismCurve deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String name = jsonObject.get("curve_name").getAsString();

        CurveClampMode clampMode = CurveClampMode.CLAMP;
        if (jsonObject.has("clamp_mode")) {
            String modeName = jsonObject.get("clamp_mode").getAsString();
            try {
                clampMode = CurveClampMode.valueOf(modeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("未知的 clamp_mode: " + modeName, e);
            }
        }

        if (!jsonObject.has("pivot_points")) {
            throw new JsonParseException("PrismCurve JSON 必须包含 'pivot_points' 字段");
        }

        JsonArray pivotPointArray = jsonObject.getAsJsonArray("pivot_points");
        if (pivotPointArray.size() < 2) {
            throw new JsonParseException("PrismCurve JSON 必须包含至少两个枢纽点");
        }

        List<CurvePivotPoint> pivotPoints = new ArrayList<>();
        for (JsonElement pivotPointElement : pivotPointArray) {
            pivotPoints.add(deserializePivotPoint(pivotPointElement.getAsJsonObject()));
        }
        return new PrismCurve(name, pivotPoints, clampMode);
    }

    /**
     * 根据点模式序列化枢纽点。
     */
    private JsonObject serializePivotPoint(CurvePivotPoint point) {
        JsonObject json = new JsonObject();
        json.addProperty("point_mode", point.getPointMode().name());
        json.addProperty("x", point.getX());
        json.addProperty("y", point.getY());

        switch (point.getPointMode()) {
            case SMOOTH -> json.add("tangent_out", JsonKit.serializeVector2d(point.getTangentOut()));
            case SPLIT -> {
                json.add("tangent_in", JsonKit.serializeVector2d(point.getTangentIn()));
                json.add("tangent_out", JsonKit.serializeVector2d(point.getTangentOut()));
            }
            case LINEAR -> {
                // 线性段的控制点由相邻枢纽点位置计算，无需持久化占位切线
            }
        }
        return json;
    }

    /**
     * 根据 point_mode 反序列化枢纽点。
     */
    private CurvePivotPoint deserializePivotPoint(JsonObject json) {
        if (!json.has("point_mode")) {
            throw new JsonParseException("枢纽点 JSON 缺失 'point_mode' 字段");
        }

        String mode = json.get("point_mode").getAsString().toUpperCase(Locale.ROOT);
        float x = json.get("x").getAsFloat();
        float y = json.get("y").getAsFloat();

        return switch (mode) {
            case "SMOOTH" -> deserializeSmoothPivotPoint(json, x, y);
            case "LINEAR" -> CurvePivotPoint.createLinearPivotPoint(x, y);
            case "SPLIT" -> deserializeSplitPivotPoint(json, x, y);
            default -> throw new JsonParseException("未知的枢纽点模式: " + mode);
        };
    }

    /**
     * 读取平滑枢纽点；同时提供两侧切线时以 tangent_out 为准。
     */
    private CurvePivotPoint deserializeSmoothPivotPoint(JsonObject json, float x, float y) {
        if (json.has("tangent_out")) {
            if (json.has("tangent_in")) {
                PrismKit.LOGGER.warn("SMOOTH 模式仅需一侧切线，将忽略 tangent_in");
            }
            Vector2d tangentOut = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_out"));
            return CurvePivotPoint.createSmoothPivotPoint(x, y, tangentOut);
        }
        if (json.has("tangent_in")) {
            Vector2d tangentIn = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_in"));
            return CurvePivotPoint.createSmoothPivotPoint(x, y, tangentIn.negate());
        }
        throw new JsonParseException("SMOOTH 模式枢纽点必须包含 'tangent_out' 或 'tangent_in' 字段");
    }

    /**
     * 读取两侧切线彼此独立的拆分枢纽点。
     */
    private CurvePivotPoint deserializeSplitPivotPoint(JsonObject json, float x, float y) {
        if (!json.has("tangent_in") || !json.has("tangent_out")) {
            throw new JsonParseException("SPLIT 模式枢纽点必须同时包含 'tangent_in' 和 'tangent_out' 字段");
        }

        Vector2d tangentIn = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_in"));
        Vector2d tangentOut = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_out"));
        return CurvePivotPoint.createSplitPivotPoint(x, y, tangentIn, tangentOut);
    }

    /**
     * 创建注册了 PrismCurve 编解码器的 Gson 实例。
     *
     * @return 可直接读写 PrismCurve 的 Gson 实例
     */
    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(PrismCurve.class, new PrismCurveCodec())
                .setPrettyPrinting()
                .create();
    }
}
