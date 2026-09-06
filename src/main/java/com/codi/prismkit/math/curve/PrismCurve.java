package com.codi.prismkit.math.curve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PrismKit 的核心曲线类。
 * 使用按 X 坐标排序的枢纽点构建连续的多段三次贝塞尔曲线。
 * 
 * 设计意图：
 * - 使用至少两个枢纽点（Pivot Points）描述曲线
 * - 每两个相邻枢纽点之间使用一段三次贝塞尔曲线连接
 * - 支持多种边界处理模式，适应不同的动画需求
 * 
 * 技术细节：
 * - 枢纽点保存位置、点模式以及输入/输出切线
 * - 相邻枢纽点会自动转换为 CurveSegment
 * - 查询时先应用边界模式，再定位对应曲线段
 */
public class PrismCurve {
    // 曲线的唯一标识符
    private final String name;
    
    // 曲线段列表（多段贝塞尔曲线）
    private final List<CurveSegment> segments;

    private final List<CurvePivotPoint> pivotPoints;
    
    // 边界处理模式
    private final CurveClampMode clampMode;
    
    // ========== 构造函数 ==========
    
    /**
     * 创建一条多段贝塞尔曲线。
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
        
        this.segments = new ArrayList<>();

        // 将每两个相邻枢纽点连接成一个曲线段
        for (int i = 0; i < pivotPoints.size() - 1; i++) {
            CurvePivotPoint current = pivotPoints.get(i);
            CurvePivotPoint next = pivotPoints.get(i + 1);
            segments.add(current.linkToOther(next));
        }
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
     * 根据输入的 X 值获取对应的 Y 值。
     * 
     * @param x 输入值（通常代表时间进度，0 到 1）
     * @return 对应的曲线输出值
     * 
     * 计算流程：
     * 1. 根据 clampMode 处理超出 [0, 1] 的输入。
     * 2. 找到包含该 X 值的曲线段。
     * 3. 在曲线段内计算对应的 Y 值。
     */
    public float getValue(float x) {
        // 第一步：应用边界处理模式
        float normalized = clampMode.apply(x);
        
        // 第二步：找到包含该 X 值的曲线段
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
        return Collections.unmodifiableList(pivotPoints);
    }
}
