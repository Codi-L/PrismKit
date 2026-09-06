package com.codi.prismkit.math.curve;

import org.joml.Vector2d;

/**
 * 贝塞尔曲线的枢纽点。
 * 除位置外，还保存进入和离开该点的切线以及点模式。
 */
public class CurvePivotPoint extends CurveControlPoint {

    /**
     * 枢纽点处的切线向量示例。中间为枢纽点，左右控制点分别由 tangentIn 和 tangentOut 控制：
     * o-------(tangentIn)-------O-------(tangentOut)-------o
     */
    private Vector2d tangentIn;  // 输入切线（指向左侧，x < 0）
    private Vector2d tangentOut; // 输出切线（指向右侧，x > 0）
    private CurvePivotPointMode pointMode;

    private CurvePivotPoint(float x, float y, Vector2d tangentIn, Vector2d tangentOut, CurvePivotPointMode pointMode) {
        super(x, y);

        // 构造时统一校验切线方向，避免生成无法按 X 轴求值的曲线段
        if (!isValidTangent(tangentIn, false)) {
            throw new IllegalArgumentException("不合法的输入切线：必须指向左侧 (x < 0) 且不能垂直。");
        }
        if (!isValidTangent(tangentOut, true)) {
            throw new IllegalArgumentException("不合法的输出切线：必须指向右侧 (x > 0) 且不能垂直。");
        }

        this.tangentIn = tangentIn;
        this.tangentOut = tangentOut;
        this.pointMode = pointMode;
    }

    /**
     * 内部校验方法：检查切线向量是否合法
     * 
     * @param tangent 待检查的向量
     * @param isOut 是否为输出切线（右侧）
     * @return 是否通过检查
     */
    private static boolean isValidTangent(Vector2d tangent, boolean isOut) {
        if (tangent == null) return false;
        
        double length = tangent.length();
        if (length <= 1e-6) return false; // 防止零向量导致除以零

        // X 轴约束：输入(In)指向左 (x < 0)，输出(Out)指向右 (x > 0)
        if (isOut) {
            if (tangent.x <= 0) return false;
        } else {
            if (tangent.x >= 0) return false;
        }

        // 垂直约束：防止斜率无限大导致的计算异常
        double unitY = Math.abs(tangent.y / length);
        return unitY < 0.99;
    }

    /**
     * 创建“平滑（SMOOTH）”模式的枢纽点。
     * 该模式下左右切线对齐且互为镜像。
     *
     * @param x 枢纽点的 X 坐标
     * @param y 枢纽点的 Y 坐标
     * @param tangent 枢纽点的切线向量（会自动生成镜像切线）
     * @return 构造好的枢纽点对象
     */
    public static CurvePivotPoint createSmoothPivotPoint(float x, float y, Vector2d tangent) {
        // tangent 默认为输出切线（指向右侧），其取反后即为输入切线（指向左侧）
        return new CurvePivotPoint(x, y, new Vector2d(tangent).negate(), tangent, CurvePivotPointMode.SMOOTH);
    }

    /**
     * 创建“线性（LINEAR）”模式的枢纽点。
     * 与该点相连的曲线段会使用直线控制点。
     *
     * @param x 枢纽点的 X 坐标
     * @param y 枢纽点的 Y 坐标
     * @return 构造好的枢纽点对象
     */
    public static CurvePivotPoint createLinearPivotPoint(float x, float y) {
        // 线性段由 linkToOther() 计算控制点；这里保留合法的占位切线
        return new CurvePivotPoint(x, y, new Vector2d(-1, 0), new Vector2d(1, 0), CurvePivotPointMode.LINEAR);
    }

    /**
     * 创建“拆分（SPLIT）”模式的枢纽点。
     * 该模式下输入切线和输出切线彼此独立，可以形成尖角。
     *
     * @param x 枢纽点的 X 坐标
     * @param y 枢纽点的 Y 坐标
     * @param tangentIn 输入切线向量（需指向左侧，x < 0）
     * @param tangentOut 输出切线向量（需指向右侧，x > 0）
     * @return 构造好的枢纽点对象
     */
    public static CurvePivotPoint createSplitPivotPoint(float x, float y, Vector2d tangentIn, Vector2d tangentOut) {
        return new CurvePivotPoint(x, y, tangentIn, tangentOut, CurvePivotPointMode.SPLIT);
    }

    public Vector2d getTangentOut() {
        return tangentOut;
    }

    public void setTangentOut(Vector2d tangentOut) {
        if (isValidTangent(tangentOut, true)) {
            this.tangentOut = new Vector2d(tangentOut);
        }
    }

    public Vector2d getTangentIn() {
        return tangentIn;
    }

    public void setTangentIn(Vector2d tangentIn) {
        if (isValidTangent(tangentIn, false)) {
            this.tangentIn = new Vector2d(tangentIn);
        }
    }

    public CurvePivotPointMode getPointMode() {
        return pointMode;
    }

    public void setPointMode(CurvePivotPointMode pointMode) {
        this.pointMode = pointMode;
    }

    /**
     * 获取输入控制点（左侧）在曲线坐标系下的位置
     */
    public CurveControlPoint getTangentInPoint() {
        return new CurveControlPoint(
                getX() + (float) tangentIn.x,
                getY() + (float) tangentIn.y
        );
    }

    /**
     * 获取输出控制点（右侧）在曲线坐标系下的位置
     */
    public CurveControlPoint getTangentOutPoint() {
        return new CurveControlPoint(
                getX() + (float) tangentOut.x,
                getY() + (float) tangentOut.y
        );
    }

    /**
     * 将当前枢纽点与下一个枢纽点连接为曲线段。
     * 任一端为 LINEAR 模式时，使用位于连线三等分点上的控制点生成直线段。
     *
     * @param other 下一个枢纽点
     * @return 连接两个枢纽点的曲线段
     */
    public CurveSegment linkToOther(CurvePivotPoint other) {
        if (pointMode == CurvePivotPointMode.LINEAR || other.pointMode == CurvePivotPointMode.LINEAR) {
            float deltaX = (other.getX() - getX()) / 3.0f;
            float deltaY = (other.getY() - getY()) / 3.0f;
            return new CurveSegment(
                    this,
                    new CurveControlPoint(getX() + deltaX, getY() + deltaY),
                    new CurveControlPoint(getX() + 2.0f * deltaX, getY() + 2.0f * deltaY),
                    other
            );
        }

        return new CurveSegment(
                this,
                this.getTangentOutPoint(),
                other.getTangentInPoint(),
                other
        );
    }
}
