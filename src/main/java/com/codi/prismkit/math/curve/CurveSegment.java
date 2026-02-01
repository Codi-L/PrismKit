package com.codi.prismkit.math.curve;

/**
 * 曲线段（Curve Segment）
 * 表示连接两个枢纽点的单段三次贝塞尔曲线
 * 
 * 设计意图：
 * - 每个段是独立的三次贝塞尔曲线，使用4个控制点定义
 * - anchorStart 和 anchorEnd 是曲线必须经过的枢纽点
 * - handleStartOut 和 handleEndIn 是控制手柄端点，决定曲线形状
 * 
 * 数学模型：
 * 对于段内的局部参数 t ∈ [0, 1]：
 * B(t) = (1-t)³·P0 + 3(1-t)²t·P1 + 3(1-t)t²·P2 + t³·P3
 * 其中：
 *   P0 = anchorStart（起始枢纽点）
 *   P1 = handleStartOut（起始点的右手柄）
 *   P2 = handleEndIn（结束点的左手柄）
 *   P3 = anchorEnd（结束枢纽点）
 */
public class CurveSegment {
    
    // 起始枢纽点（曲线的起点，必须经过）
    private final CurveControlPoint anchorStart;
    
    // 起始点的右手柄（Out Handle，控制曲线离开起点的方向）
    private final CurveControlPoint handleStartOut;
    
    // 结束点的左手柄（In Handle，控制曲线进入终点的方向）
    private final CurveControlPoint handleEndIn;
    
    // 结束枢纽点（曲线的终点，必须经过）
    private final CurveControlPoint anchorEnd;
    
    // 该段在全局曲线中的 x 范围（归一化坐标）
    private final float xStart;  // 段起始的 x 值（等于 anchorStart.x）
    private final float xEnd;    // 段结束的 x 值（等于 anchorEnd.x）
    
    /**
     * 构造函数
     * 
     * @param anchorStart 起始枢纽点
     * @param handleStartOut 起始点的右手柄控制点
     * @param handleEndIn 结束点的左手柄控制点
     * @param anchorEnd 结束枢纽点
     */
    public CurveSegment(CurveControlPoint anchorStart, CurveControlPoint handleStartOut,
                        CurveControlPoint handleEndIn, CurveControlPoint anchorEnd) {
        this.anchorStart = anchorStart;
        this.handleStartOut = handleStartOut;
        this.handleEndIn = handleEndIn;
        this.anchorEnd = anchorEnd;
        
        // 自动计算段的x范围
        this.xStart = anchorStart.getX();
        this.xEnd = anchorEnd.getX();
        
        // 验证：确保 xEnd > xStart（段必须向右延伸）
        if (xEnd <= xStart) {
            throw new IllegalArgumentException(
                String.format("曲线段的结束点x坐标(%.3f)必须大于起始点x坐标(%.3f)", xEnd, xStart)
            );
        }
    }
    
    /**
     * 判断全局x值是否在该段范围内
     * 
     * @param globalX 全局归一化x坐标（通常 0 到 1）
     * @return 如果 x 在 [xStart, xEnd] 范围内返回 true
     */
    public boolean containsX(float globalX) {
        return globalX >= xStart && globalX <= xEnd;
    }
    
    /**
     * 计算该段在给定全局x值处的y值
     * 
     * @param globalX 全局归一化x坐标
     * @return 对应的y值
     * @throws IllegalArgumentException 如果 globalX 不在该段范围内
     */
    public float evaluate(float globalX) {
        if (!containsX(globalX)) {
            throw new IllegalArgumentException(
                String.format("x值 %.3f 不在段范围 [%.3f, %.3f] 内", globalX, xStart, xEnd)
            );
        }
        
        // 将全局x转换为段内局部参数 t ∈ [0, 1]
        float t = (globalX - xStart) / (xEnd - xStart);
        
        // 使用三次贝塞尔公式计算y值
        return evaluateBezierY(t);
    }
    
    /**
     * 三次贝塞尔曲线求值（Y坐标）
     * 
     * 公式：B(t) = (1-t)³·P0 + 3(1-t)²t·P1 + 3(1-t)t²·P2 + t³·P3
     * 
     * 伯恩斯坦多项式权重：
     * - b0 = (1-t)³       → P0的影响权重
     * - b1 = 3(1-t)²t     → P1的影响权重
     * - b2 = 3(1-t)t²     → P2的影响权重
     * - b3 = t³           → P3的影响权重
     * 
     * @param t 局部参数，范围 [0, 1]
     * @return 对应的 y 坐标
     */
    private float evaluateBezierY(float t) {
        float u = 1.0f - t;  // u = (1 - t)
        
        // 计算权重
        float b0 = u * u * u;           // (1-t)³
        float b1 = 3 * u * u * t;       // 3(1-t)²t
        float b2 = 3 * u * t * t;       // 3(1-t)t²
        float b3 = t * t * t;           // t³
        
        // 求和得到 y 坐标
        return b0 * anchorStart.getY()
             + b1 * handleStartOut.getY()
             + b2 * handleEndIn.getY()
             + b3 * anchorEnd.getY();
    }
    
    // ========== Getter 方法 ==========
    
    public CurveControlPoint getAnchorStart() {
        return anchorStart;
    }
    
    public CurveControlPoint getHandleStartOut() {
        return handleStartOut;
    }
    
    public CurveControlPoint getHandleEndIn() {
        return handleEndIn;
    }
    
    public CurveControlPoint getAnchorEnd() {
        return anchorEnd;
    }
    
    public float getXStart() {
        return xStart;
    }
    
    public float getXEnd() {
        return xEnd;
    }
    
    /**
     * 获取段的长度（x轴跨度）
     */
    public float getLength() {
        return xEnd - xStart;
    }
    
    @Override
    public String toString() {
        return String.format("CurveSegment[x: %.3f → %.3f, anchors: (%.3f,%.3f) → (%.3f,%.3f)]",
            xStart, xEnd,
            anchorStart.getX(), anchorStart.getY(),
            anchorEnd.getX(), anchorEnd.getY());
    }
}
