package com.codi.prismkit.math.curve;

/**
 * 曲线超出 [0, 1] 范围时的处理模式
 * 
 * 设计意图：
 * - 提供多种边界处理策略，适应不同的使用场景
 * - CLAMP 适合单次动画（如淡入淡出）
 * - REPEAT 适合循环动画（如呼吸灯效果）
 * - MIRROR 适合来回循环（如摆动效果）
 */
public enum CurveClampMode {
    /**
     * 钳位模式：超出范围时使用边界值
     * 例如：t < 0 时返回 curve(0)，t > 1 时返回 curve(1)
     */
    CLAMP,

    /**
     * 重复模式：将输入值映射回 [0, 1] 范围
     * 例如：t = 1.3 等同于 t = 0.3
     */
    REPEAT,

    /**
     * 镜像模式：在边界处反向重复
     * 例如：t = 1.3 等同于 t = 0.7（相当于 1 - 0.3）
     */
    MIRROR;

    /**
     * 根据模式对输入值进行处理
     * 
     * @param t 原始输入值
     * @return 处理后的值（保证在 [0, 1] 范围内）
     */
    public float apply(float t) {
        switch (this) {
            case CLAMP:
                // 直接钳位到 [0, 1]
                return Math.max(0.0f, Math.min(1.0f, t));
            
            case REPEAT:
                // 取小数部分，实现循环
                if (t >= 0.0f) {
                    return t - (float) Math.floor(t);
                } else {
                    return 1.0f - (Math.abs(t) - (float) Math.floor(Math.abs(t)));
                }
            
            case MIRROR:
                // 镜像循环
                float repeated = apply_repeat(t);
                int cycle = (int) Math.floor(t);
                // 偶数周期正向，奇数周期反向
                return (cycle % 2 == 0) ? repeated : (1.0f - repeated);
            
            default:
                return Math.max(0.0f, Math.min(1.0f, t));
        }
    }

    /**
     * 内部辅助方法：实现 REPEAT 逻辑
     */
    private float apply_repeat(float t) {
        if (t >= 0.0f) {
            return t - (float) Math.floor(t);
        } else {
            return 1.0f - (Math.abs(t) - (float) Math.floor(Math.abs(t)));
        }
    }
}
