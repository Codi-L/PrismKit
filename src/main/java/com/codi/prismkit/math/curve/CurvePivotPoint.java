package com.codi.prismkit.math.curve;

import org.joml.Vector2d;

public class CurvePivotPoint extends CurveControlPoint{

    /**
     * 以下为在枢纽点处的切线向量示例，中间为枢纽点，左侧控制点由tangentIn控制，右侧控制点由tangentOut控制
     * o-------(tangentIn)-------O-------(tangentOut)-------o
     */
    private Vector2d tangentIn;  // 输入切线（指向左侧，x < 0）
    private Vector2d tangentOut; // 输出切线（指向右侧，x > 0）
    private CurvePivotPointMode pointMode;

    private CurvePivotPoint(float x, float y, Vector2d tangentIn, Vector2d tangentOut, CurvePivotPointMode pointMode) {
        super(x, y);

        // 使用提取出的 check 方法进行合法性校验
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
     * 创建一个“平滑 (Smooth)”模式的枢纽点。
     * 在此模式下，左右控制点（切线）是对齐且镜像的。
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
     * 创建一个“线性 (Linear)”模式的枢纽点。
     * 在此模式下，曲线在到达此点前后将表现为直线。
     *
     * @param x 枢纽点的 X 坐标
     * @param y 枢纽点的 Y 坐标
     * @return 构造好的枢纽点对象
     */
    public static CurvePivotPoint createLinearPivotPoint(float x, float y) {
        // 线性模式默认使用水平切线：In 为 (-1, 0), Out 为 (1, 0)
        return new CurvePivotPoint(x, y, new Vector2d(-1, 0), new Vector2d(1, 0), CurvePivotPointMode.LINEAR);
    }

    /**
     * 创建一个“拆分 (Split)”模式的枢纽点。
     * 在此模式下，输入切线和输出切线是独立的，可以形成尖角。
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

    public Vector2d getTangentOut() { return tangentOut; }
    public void setTangentOut(Vector2d tangentOut) {
        if (isValidTangent(tangentOut, true)) {
            this.tangentOut = new Vector2d(tangentOut);
        }
    }

    public Vector2d getTangentIn() { return tangentIn; }
    public void setTangentIn(Vector2d tangentIn) {
        if (isValidTangent(tangentIn, false)) {
            this.tangentIn = new Vector2d(tangentIn);
        }
    }

    public CurvePivotPointMode getPointMode() { return pointMode; }
    public void setPointMode(CurvePivotPointMode pointMode) { this.pointMode = pointMode; }

    /**
     * 获取输入控制点（左侧）在曲线坐标系下的位置
     */
    public CurveControlPoint getTangentInPoint () {
        return new CurveControlPoint(
                getX() + (float) tangentIn.x,
                getY() + (float) tangentIn.y
        );
    }

    /**
     * 获取输出控制点（右侧）在曲线坐标系下的位置
     */
    public CurveControlPoint getTangentOutPoint () {
        if (this.pointMode == CurvePivotPointMode.LINEAR) {
            throw new UnsupportedOperationException("线性模式下 PivotPoint 不存在输出切线");
        } 
        return new CurveControlPoint(
                getX() + (float) tangentOut.x,
                getY() + (float) tangentOut.y
        );
    }

    public CurvePivotPointMode getPivotPointMode() {
        return pointMode;
    }

    //将该枢纽点与另一个枢纽点连接，返回曲线段
    public CurveSegment linkToOther(CurvePivotPoint other) {
        return new CurveSegment(
                this,
                this.getTangentOutPoint(),
                other.getTangentInPoint(),
                other
        );
    }





}
