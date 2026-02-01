package com.codi.prismkit.math.curve;

import com.codi.prismkit.JsonKit;
import com.codi.prismkit.PrismKit;
import com.google.gson.*;
import org.joml.Vector2d;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * PrismCurve 的 JSON 序列化/反序列化适配器
 * 将曲线对象转换为紧凑的 JSON 格式，并支持从 JSON 加载
 * 
 * 设计意图：
 * - 使用 GSON 库实现自动化序列化
 * - JSON 格式需要平衡可读性和文件大小
 * - 支持手动编辑 JSON 文件（开发者友好）
 * - 同时支持单段曲线和多段曲线的序列化
 * 
 * 单段曲线 JSON 格式示例：
 * {
 *   "curve_name": "fade_in_smooth",
 *   "control_points": [
 *     {"x": 0.0, "y": 0.0},
 *     {"x": 0.3, "y": 0.1},
 *     {"x": 0.7, "y": 0.9},
 *     {"x": 1.0, "y": 1.0}
 *   ],
 *   "clamp_mode": "CLAMP"
 * }
 * 
 * 多段曲线 JSON 格式示例：
 * {
 *   "curve_name": "mountain",
 *   "segments": [
 *     {
 *       "anchor_start": {"x": 0.0, "y": 0.0},
 *       "handle_start_out": {"x": 0.2, "y": 0.2},
 *       "handle_end_in": {"x": 0.3, "y": 0.9},
 *       "anchor_end": {"x": 0.5, "y": 1.0}
 *     },
 *     {
 *       "anchor_start": {"x": 0.5, "y": 1.0},
 *       "handle_start_out": {"x": 0.7, "y": 0.9},
 *       "handle_end_in": {"x": 0.8, "y": 0.2},
 *       "anchor_end": {"x": 1.0, "y": 0.0}
 *     }
 *   ],
 *   "clamp_mode": "CLAMP"
 * }
 */
public class PrismCurveCodec implements JsonSerializer<PrismCurve>, JsonDeserializer<PrismCurve> {

    /**
     * 序列化：将 PrismCurve 对象转换为 JSON
     */
    @Override
    public JsonElement serialize(PrismCurve curve, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        
        // 添加曲线名称
        json.addProperty("curve_name", curve.getName());

        JsonArray pivotPointArray = new JsonArray();
        for (CurvePivotPoint pivotPoint : curve.getPivotPoints()) {
            pivotPointArray.add(serializePivotPoint(pivotPoint));
        }
        json.add("pivot_points", pivotPointArray);

        // 添加边界模式
        json.addProperty("clamp_mode", curve.getClampMode().name());
        
        return json;
    }

    /**
     * 反序列化：从 JSON 构建 PrismCurve 对象
     */
    @Override
    public PrismCurve deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        
        // 读取曲线名称
        String name = jsonObject.get("curve_name").getAsString();
        
        // 读取边界模式（如果不存在则使用默认值 CLAMP）
        CurveClampMode clampMode = CurveClampMode.CLAMP;
        if (jsonObject.has("clamp_mode")) {
            try {
                clampMode = CurveClampMode.valueOf(jsonObject.get("clamp_mode").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("未知的 clamp_mode: " + jsonObject.get("clamp_mode").getAsString());
            }
        }

        if (jsonObject.has("pivot_points")) {
            JsonArray pivotPoints_jsonArray = jsonObject.getAsJsonArray("pivot_points");
            if (!pivotPoints_jsonArray.isEmpty()) {
                List<CurvePivotPoint> pivotPoints = new ArrayList<>();
                for (JsonElement pivotPointElement : pivotPoints_jsonArray) {
                    pivotPoints.add(deserializePivotPoint(pivotPointElement.getAsJsonObject()));
                }
                return new PrismCurve(name, pivotPoints, clampMode);
            } else {
                throw new JsonParseException("PrismCurve JSON 必须包含至少一个枢纽点");
            }
        } else {
            throw new JsonParseException("PrismCurve JSON 必须包含 'pivot_points' 字段");
        }
    }

    /**
     * 辅助方法：将 CurvePivotPoint 序列化为 JSON 对象
     */
    private JsonObject serializePivotPoint(CurvePivotPoint point) {
        JsonObject json = new JsonObject();
        json.addProperty("point_mode", point.getPointMode().name());
        json.addProperty("x", point.getX());
        json.addProperty("y", point.getY());
        json.add("tangent_in", JsonKit.serializeVector2d(point.getTangentIn()));
        json.add("tangent_out", JsonKit.serializeVector2d(point.getTangentOut()));
        return json;
    }

    public CurvePivotPoint deserializePivotPoint(JsonObject json) {
        if (json.has("point_mode")) {
            String mode = json.get("point_mode").getAsString();
            float x = json.get("x").getAsFloat();
            float y = json.get("y").getAsFloat();

            switch (mode) {
                case "SMOOTH":
                    if (json.has("tangent_out") || json.has("tangent_in")) {
                        PrismKit.LOGGER.warn("SMOOTH模式的枢纽点仅需设置一侧切线，默认读取tangent_out,tangent_in将被忽略");
                    } else if (json.has("tangent_out")) {
                        Vector2d tangentOut = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_out"));
                        return CurvePivotPoint.createSmoothPivotPoint(x, y, tangentOut);
                    } else if (json.has("tangent_in")) { // SMOOTH模式恢复时应使用 tangent_out，此处检测用户若使用了tangent_in,则进行反转自动适配
                        Vector2d tangentIn = JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_in"));
                        return CurvePivotPoint.createSmoothPivotPoint(x, y, tangentIn.negate());
                    } else {
                        throw new JsonParseException("SMOOTH 模式枢纽点必须包含 'tangent_out' 或 'tangent_in' 字段");
                    }
                case "LINEAR":
                    return CurvePivotPoint.createLinearPivotPoint(x, y);
                case "SPLIT":
                    if (json.has("tangent_out") || json.has("tangent_in")) {
                        return CurvePivotPoint.createSplitPivotPoint(x, y,
                                JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_in")),
                                JsonKit.deserializeVector2d(json.getAsJsonObject("tangent_out")));
                    } else {
                        throw new JsonParseException("SPLIT 模式枢纽点必须包含 'tangent_out' 和 'tangent_in' 字段");
                    }
                default:
                    throw new JsonParseException("未知的枢纽点模式: " + mode);
            }
        } else {
            throw new JsonParseException("枢纽点 JSON 缺失 'point_mode' 字段");
        }
    }

    /**
     * 辅助方法：从 JSON 对象反序列化 ControlPoint
     */
    private CurveControlPoint deserializePoint(JsonObject json) {
        float x = json.get("x").getAsFloat();
        float y = json.get("y").getAsFloat();
        return new CurveControlPoint(x, y);
    }

    /**
     * 辅助方法：从 JSON 对象反序列化 CurveSegment
     */
    private CurveSegment deserializeSegment(JsonObject json) {
        CurveControlPoint anchorStart = deserializePoint(json.getAsJsonObject("anchor_start"));
        CurveControlPoint handleStartOut = deserializePoint(json.getAsJsonObject("handle_start_out"));
        CurveControlPoint handleEndIn = deserializePoint(json.getAsJsonObject("handle_end_in"));
        CurveControlPoint anchorEnd = deserializePoint(json.getAsJsonObject("anchor_end"));
        
        return new CurveSegment(anchorStart, handleStartOut, handleEndIn, anchorEnd);
    }

    /**
     * 工厂方法：创建配置好的 Gson 实例
     * 这个方法提供了一个开箱即用的 JSON 解析器
     * 
     * @return 配置了 PrismCurve 序列化器的 Gson 实例
     */
    public static Gson createGson() {
        return new GsonBuilder()
            .registerTypeAdapter(PrismCurve.class, new PrismCurveCodec())
            .setPrettyPrinting() // 启用格式化输出（方便人类阅读）
            .create();
    }
}
