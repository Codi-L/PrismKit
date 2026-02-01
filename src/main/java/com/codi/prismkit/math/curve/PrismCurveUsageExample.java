package com.codi.prismkit.math.curve;

/**
 * PrismCurve 使用示例
 * 展示如何在其他模组中使用 PrismKit 的曲线系统
 * 
 * 这个类包含了常见的使用场景和最佳实践
 */
public class PrismCurveUsageExample {

    /**
     * 示例 1：基础使用 - 淡入动画
     * 
     * 场景：制作一个物品使用时的淡入效果
     */
    public static void exampleFadeIn() {
        // 假设这是物品的使用进度（0 到 1）
        float progress = 0.5f; // 50% 进度
        
        // 使用 PrismKit 的曲线获取不透明度
        float opacity = com.codi.prismkit.PrismKit.getCurveValue("fade_in_smooth", progress);
        
        // opacity 现在是一个平滑的渐变值，可以直接用于渲染
        // 例如：renderItemWithOpacity(itemStack, opacity);
        
        System.out.println("进度: " + progress + " -> 不透明度: " + opacity);
    }

    /**
     * 示例 2：高级使用 - 粒子发射速度曲线
     * 
     * 场景：制作一个爆炸效果，粒子速度先快后慢
     */
    public static void exampleParticleSpeed(int ticksElapsed, int totalTicks) {
        // 计算归一化的时间进度
        float timeProgress = (float) ticksElapsed / totalTicks;
        
        // 使用弹跳曲线获取速度倍数
        float speedMultiplier = com.codi.prismkit.PrismKit.getCurveValue("bounce", timeProgress);
        
        // 基础速度 * 曲线倍数
        float finalSpeed = 1.0f * speedMultiplier;
        
        // spawnParticle(position, velocity * finalSpeed);
    }

    /**
     * 示例 3：循环动画 - 呼吸灯效果
     * 
     * 场景：制作一个循环的脉冲光效
     */
    public static void examplePulsingLight(long worldTime) {
        // 使用世界时间创建循环动画（每 40 tick 一个周期）
        float cycleProgress = (worldTime % 40) / 40.0f;
        
        // 使用 REPEAT 模式的曲线
        float brightness = com.codi.prismkit.PrismKit.getCurveValue("pulse", cycleProgress);
        
        // setLightLevel(brightness);
    }

    /**
     * 示例 4：检查曲线是否存在
     * 
     * 场景：在使用曲线前检查其是否已加载
     */
    public static void exampleSafeUsage(String curveName, float input) {
        if (com.codi.prismkit.PrismKit.hasCurve(curveName)) {
            // 曲线存在，安全使用
            float value = com.codi.prismkit.PrismKit.getCurveValue(curveName, input);
            System.out.println("曲线值: " + value);
        } else {
            // 曲线不存在，使用回退值
            System.out.println("曲线 '" + curveName + "' 未找到，使用线性回退");
            float value = input; // 线性回退
        }
    }

    /**
     * 示例 5：创建自定义曲线（在代码中）
     * 
     * 场景：在运行时动态创建曲线
     */
    /*
    public static void exampleCreateCurve() throws Exception {
        // 创建一个新的曲线
        PrismCurve customCurve = new PrismCurve(
            "my_custom_curve",
            new CurveControlPoint(0.0f, 0.0f),   // 起点
            new CurveControlPoint(0.25f, 0.8f),  // 快速上升
            new CurveControlPoint(0.75f, 0.9f),  // 保持高位
            new CurveControlPoint(1.0f, 1.0f),   // 终点
            CurveClampMode.CLAMP
        );
        
        // 保存到文件（会自动存到 config/prismkit/curves/ 目录）
        PrismCurveManager.getInstance().saveCurve(customCurve);
        
        // 现在可以在任何地方使用这个曲线
        float value = com.codi.prismkit.PrismKit.getCurveValue("my_custom_curve", 0.5f);
        System.out.println("自定义曲线值: " + value);
    }

     */

    /**
     * 最佳实践总结
     * 
     * 1. 性能考虑：
     *    - getCurveValue() 直接计算贝塞尔公式，性能开销极低
     *    - 可以每帧调用数千次而不影响游戏帧率
     *    - 避免在循环中重复检查 hasCurve()，在初始化时检查一次即可
     * 
     * 2. 曲线命名规范：
     *    - 使用小写字母和下划线：fade_in_smooth ✓
     *    - 避免空格和特殊字符：fade in smooth ✗
     *    - 使用描述性名称：curve1 ✗ -> fade_in_smooth ✓
     * 
     * 3. JSON 文件位置：
     *    - 开发环境：run/config/prismkit/curves/
     *    - 生产环境：<minecraft>/config/prismkit/curves/
     *    - 可以通过资源包覆盖曲线定义
     * 
     * 4. 错误处理：
     *    - PrismKit 在曲线不存在时会自动回退到线性（y=x）
     *    - 检查日志以发现缺失的曲线定义
     *    - 在生产环境中使用 hasCurve() 进行防御性检查
     * 
     * 5. 精度说明：
     *    - 目前版本使用实时计算，精度完全精确（无离散化误差）
     *    - 不需要为了“高精度”而使用特殊方法，默认即最佳
     */
}
