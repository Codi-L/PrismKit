package com.codi.prismkit.math.curve;

import com.codi.prismkit.PrismKit;
import org.joml.Vector2d;

import java.io.IOException;
import java.util.List;

/**
 * PrismCurve 使用示例。
 * 展示查询内置曲线以及使用枢纽点 API 创建自定义曲线的方式。
 */
public final class PrismCurveUsageExample {

    private PrismCurveUsageExample() {
    }

    /**
     * 使用平滑淡入曲线计算透明度。
     */
    public static void exampleFadeIn() {
        float progress = 0.5f;
        float opacity = PrismKit.getCurveValue("fade_in_smooth", progress);
        System.out.println("进度: " + progress + " -> 不透明度: " + opacity);
    }

    /**
     * 使用弹跳曲线计算粒子速度倍数。
     *
     * @param ticksElapsed 已经过的 tick 数
     * @param totalTicks 动画总 tick 数
     * @return 应用于基础速度的倍率
     */
    public static float exampleParticleSpeed(int ticksElapsed, int totalTicks) {
        float progress = (float) ticksElapsed / totalTicks;
        return PrismKit.getCurveValue("bounce", progress);
    }

    /**
     * 使用循环曲线计算脉冲亮度。
     *
     * @param worldTime 当前世界时间
     * @return 当前周期的亮度
     */
    public static float examplePulsingLight(long worldTime) {
        float progress = (worldTime % 40) / 40.0f;
        return PrismKit.getCurveValue("pulse", progress);
    }

    /**
     * 安全查询一条可能不存在的曲线。
     *
     * @param curveName 曲线名称
     * @param input 输入值
     * @return 曲线值；曲线不存在时返回输入值
     */
    public static float exampleSafeUsage(String curveName, float input) {
        if (!PrismKit.hasCurve(curveName)) {
            return input;
        }
        return PrismKit.getCurveValue(curveName, input);
    }

    /**
     * 使用当前枢纽点模型创建并保存一条平滑曲线。
     * SMOOTH 模式只需要提供输出切线，输入切线会自动镜像生成。
     *
     * @throws IOException 曲线文件写入失败时抛出
     */
    public static void exampleCreateCurve() throws IOException {
        PrismCurve customCurve = new PrismCurve(
                "my_custom_curve",
                List.of(
                        CurvePivotPoint.createSmoothPivotPoint(
                                0.0f, 0.0f, new Vector2d(0.25, 0.8)
                        ),
                        CurvePivotPoint.createSmoothPivotPoint(
                                1.0f, 1.0f, new Vector2d(0.25, 0.1)
                        )
                ),
                CurveClampMode.CLAMP
        );

        PrismCurveManager.getInstance().saveCurve(customCurve);
    }
}
