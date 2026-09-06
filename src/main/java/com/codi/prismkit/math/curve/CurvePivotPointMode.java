package com.codi.prismkit.math.curve;

/**
 * 枢纽点两侧曲线的连接模式。
 */
public enum CurvePivotPointMode {
    /** 左右切线对齐且互为镜像。 */
    SMOOTH,

    /** 与该点相连的曲线段按直线生成。 */
    LINEAR,

    /** 左右切线彼此独立，可形成尖角。 */
    SPLIT
}
