package com.codi.prismkit.math.curve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PrismKit 的核心曲线类
 * 基于三次贝塞尔曲线实现，支持单段和多段曲线
 * 
 * 设计意图：
 * - 支持任意数量的枢纽点（Anchor Points），创建复杂的曲线形状
 * - 每两个相邻枢纽点之间使用一段三次贝塞尔曲线连接
 * - 支持多种边界处理模式，适应不同的动画需求
 * - 线程安全的不可变设计，可在多线程环境下共享
 * 
 * 技术细节：
 * - 采用三次贝塞尔曲线（4 个控制点）作为工业标准
 * - 多段曲线通过 CurveSegment 列表存储，自动找到对应的段
 * - 向后兼容：支持4个控制点的单段曲线构造函数
 */
public class PrismCurve {
    // 曲线的唯一标识符
    private final String name;
    
    // 曲线段列表（多段贝塞尔曲线）
    private List<CurveSegment> segments;

    private final List<CurvePivotPoint> pivotPoints;
    
    // 边界处理模式
    private final CurveClampMode clampMode;
    
    // ========== 构造函数：多段曲线 ==========
    
    /**
     * 主构造函数：创建多段贝塞尔曲线
     * 
     * @param name 曲线名称（用于标识和加载）
     * @param pivotPoints 曲线枢纽点列表
     * @param clampMode 边界处理模式
     */
    public PrismCurve(String name, List<CurvePivotPoint> pivotPoints, CurveClampMode clampMode) {
        if (pivotPoints == null || pivotPoints.size() < 2) {
            throw new IllegalArgumentException("多段曲线至少需要 2 个枢纽点");
        }
        this.pivotPoints = new ArrayList<>(pivotPoints);
        this.name = name;
        
        List<CurveSegment> segmentsList = new ArrayList<>();
        // 遍历枢纽点列表，将每两个相邻的枢纽点连接成一个曲线段
        for (int i = 0; i < pivotPoints.size() - 1; i++) {
            CurvePivotPoint current = pivotPoints.get(i);
            CurvePivotPoint next = pivotPoints.get(i + 1);
            segmentsList.add(current.linkToOther(next));
        }
        
        this.segments = segmentsList;
        this.clampMode = clampMode;
        
        // 验证曲线段的连续性
        validateSegments();
    }
    
    /**
     * 简化构造函数：使用默认的 CLAMP 模式
     */
    public PrismCurve(String name, List<CurvePivotPoint> pivotPoints) {
        this(name, pivotPoints, CurveClampMode.CLAMP);
    }
    
    // ========== 构造函数：向后兼容（单段曲线）==========

    /**
     * 向后兼容构造函数：创建单段贝塞尔曲线（4个控制点）
     * 这个构造函数保留了旧 API 兼容，内部转换为单段曲线
     * 
     * @param name 曲线名称
     * @param p0 起点（通常是 0, 0）
     * @param p1 第一个控制点（决定起始段的曲率）
     * @param p2 第二个控制点（决定结束段的曲率）
     * @param p3 终点（通常是 1, 1）
     * @param clampMode 边界处理模式
     */

    /*
    public PrismCurve(String name, CurveControlPoint p0, CurveControlPoint p1,
                      CurveControlPoint p2, CurveControlPoint p3, CurveClampMode clampMode) {
        this.name = name;
        this.clampMode = clampMode;
        
        // 将 4 个控制点转换为单段曲线
        CurveSegment singleSegment = new CurveSegment(p0, p1, p2, p3);
        this.segments = Collections.singletonList(singleSegment);
    }

    public PrismCurve(String name, CurveControlPoint p0, CurveControlPoint p1,
                      CurveControlPoint p2, CurveControlPoint p3) {
        this(name, p0, p1, p2, p3, CurveClampMode.CLAMP);
    }
    */



    /**
     * 验证曲线段的连续性和有效性
     * 确保相邻段的终点和起点相连接
     */
    private void validateSegments() {
        for (int i = 0; i < segments.size() - 1; i++) {
            CurveSegment current = segments.get(i);
            CurveSegment next = segments.get(i + 1);
            
            // 检查相邻段是否连接（允许微小误差）
            float currentEnd = current.getXEnd();
            float nextStart = next.getXStart();
            if (Math.abs(currentEnd - nextStart) > 0.0001f) {
                throw new IllegalArgumentException(
                    String.format("曲线段 %d 和 %d 不连续：段%d终点x=%.3f，段%d起点x=%.3f",
                        i, i+1, i, currentEnd, i+1, nextStart)
                );
            }
        }
    }

    /**
     * 核心方法：根据输入的 x 值获取对应的 y 值
     * 这是对外暴露的主要 API
     * 
     * @param x 输入值（通常代表时间进度，0 到 1）
     * @return 对应的曲线输出值
     * 
     * 实现逻辑：
     * 1. 根据 clampMode 处理超出 [0, 1] 的输入
     * 2. 找到包含该 x 值的曲线段
     * 3. 调用该段的 evaluate() 方法计算 y 值
     */
    public float getValue(float x) {
        // 第一步：应用边界处理模式
        float normalized = clampMode.apply(x);
        
        // 第二步：找到包含该 x 值的曲线段
        for (CurveSegment segment : segments) {
            if (segment.containsX(normalized)) {
                return segment.evaluate(normalized);
            }
        }
        
        // 如果未找到（理论上不应该发生），返回第一段或最后一段的值
        if (normalized <= segments.get(0).getXStart()) {
            return segments.get(0).getAnchorStart().getY();
        } else {
            return segments.get(segments.size() - 1).getAnchorEnd().getY();
        }
    }

    // ========== Getter 方法 ==========
    
    public String getName() {
        return name;
    }
    
    public List<CurveSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }
    
    public int getSegmentCount() {
        return segments.size();
    }

    public CurveClampMode getClampMode() {
        return clampMode;
    }
    
    // ========== 向后兼容的 Getter（仅对单段曲线有效）==========
    
    /**
     * 获取第一个控制点（向后兼容）
     * 仅对单段曲线有效
     */
    public CurveControlPoint getP0() {
        if (segments.size() != 1) {
            throw new UnsupportedOperationException("此方法仅适用于单段曲线");
        }
        return segments.get(0).getAnchorStart();
    }

    /**
     * 获取第二个控制点（向后兼容）
     * 仅对单段曲线有效
     */
    public CurveControlPoint getP1() {
        if (segments.size() != 1) {
            throw new UnsupportedOperationException("此方法仅适用于单段曲线");
        }
        return segments.get(0).getHandleStartOut();
    }

    /**
     * 获取第三个控制点（向后兼容）
     * 仅对单段曲线有效
     */
    public CurveControlPoint getP2() {
        if (segments.size() != 1) {
            throw new UnsupportedOperationException("此方法仅适用于单段曲线");
        }
        return segments.get(0).getHandleEndIn();
    }

    /**
     * 获取第四个控制点（向后兼容）
     * 仅对单段曲线有效
     */
    public CurveControlPoint getP3() {
        if (segments.size() != 1) {
            throw new UnsupportedOperationException("此方法仅适用于单段曲线");
        }
        return segments.get(0).getAnchorEnd();
    }

    @Override
    public String toString() {
        if (segments.size() == 1) {
            // 单段曲线：显示详细信息
            CurveSegment seg = segments.get(0);
            return String.format("PrismCurve[name=%s, clampMode=%s, points=[%s, %s, %s, %s]]",
                name, clampMode, seg.getAnchorStart(), seg.getHandleStartOut(), 
                seg.getHandleEndIn(), seg.getAnchorEnd());
        } else {
            // 多段曲线：显示段数
            return String.format("PrismCurve[name=%s, clampMode=%s, segments=%d]",
                name, clampMode, segments.size());
        }
    }

    public List<CurvePivotPoint> getPivotPoints() {
        return pivotPoints;
    }
}
