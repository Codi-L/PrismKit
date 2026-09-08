package com.codi.prismkit.math.curve;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PrismCurve 当前枢纽点模型的单元测试。
 * 覆盖线性段、JSON 往返以及内置曲线资源的加载。
 */
class PrismCurveTest {
    private static final float EPSILON = 0.0001f;
    private final Gson gson = PrismCurveCodec.createGson();

    @Test
    void linearPivotPointsCreateLinearSegment() {
        PrismCurve curve = new PrismCurve(
                "linear",
                List.of(
                        CurvePivotPoint.createLinearPivotPoint(0.0f, 0.0f),
                        CurvePivotPoint.createLinearPivotPoint(1.0f, 1.0f)
                )
        );

        assertEquals(0.25f, curve.getValue(0.25f), EPSILON);
        assertEquals(0.5f, curve.getValue(0.5f), EPSILON);
        assertEquals(0.75f, curve.getValue(0.75f), EPSILON);
    }

    @Test
    void clampModesHandleNegativeCycleBoundaries() {
        assertEquals(0.0f, CurveClampMode.REPEAT.apply(-1.0f), EPSILON);
        assertEquals(0.7f, CurveClampMode.REPEAT.apply(-0.3f), EPSILON);
        assertEquals(1.0f, CurveClampMode.MIRROR.apply(-1.0f), EPSILON);
        assertEquals(0.3f, CurveClampMode.MIRROR.apply(-0.3f), EPSILON);
    }

    @Test
    void smoothCurveRoundTripUsesCanonicalJson() {
        PrismCurve original = new PrismCurve(
                "smooth",
                List.of(
                        CurvePivotPoint.createSmoothPivotPoint(
                                0.0f, 0.0f, new Vector2d(0.3, 0.1)
                        ),
                        CurvePivotPoint.createSmoothPivotPoint(
                                1.0f, 1.0f, new Vector2d(0.3, 0.1)
                        )
                ),
                CurveClampMode.CLAMP
        );

        String json = gson.toJson(original);
        JsonObject firstPivot = gson.fromJson(json, JsonObject.class)
                .getAsJsonArray("pivot_points")
                .get(0)
                .getAsJsonObject();
        assertFalse(firstPivot.has("tangent_in"));

        PrismCurve restored = gson.fromJson(json, PrismCurve.class);
        assertEquals(original.getName(), restored.getName());
        assertEquals(original.getClampMode(), restored.getClampMode());
        assertEquals(original.getValue(0.5f), restored.getValue(0.5f), EPSILON);
    }

    @Test
    void bundledCurvesUseCurrentPivotPointFormat() throws IOException {
        assertBundledCurve("bounce", 1);
        assertBundledCurve("fade_in_smooth", 1);
        assertBundledCurve("mountain", 2);
        assertBundledCurve("pulse", 1);
    }

    /**
     * 加载并验证一条打包在主资源集中的曲线。
     */
    private void assertBundledCurve(String name, int expectedSegments) throws IOException {
        String resourcePath = "/data/prismkit/curves/" + name + ".json";
        try (InputStream stream = PrismCurveTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "缺少内置曲线资源: " + resourcePath);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            PrismCurve curve = gson.fromJson(json, PrismCurve.class);

            assertEquals(name, curve.getName());
            assertEquals(expectedSegments, curve.getSegmentCount());
        }
    }
}
