package com.codi.prismkit.math.curve;

public class CurveControlPoint {
    private float x;
    private float y;

    public CurveControlPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }
    public float getY() {
        return y;
    }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }


    /**
     * 验证控制点是否在标准范围内（x 和 y 都在 [0, 1] 之间）
     * 注意：这不是强制要求，某些情况下允许超出范围
     * 
     * @return 如果 x 和 y 都在 [0, 1] 范围内返回 true
     */
    public boolean isNormalized() {

        return x >= 0.0f && x <= 1.0f && y >= 0.0f && y <= 1.0f;
    }

    @Override
    public String toString() {
        return String.format("ControlPoint(%.3f, %.3f)", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CurveControlPoint)) return false;
        CurveControlPoint other = (CurveControlPoint) obj;
        return Float.compare(x, other.x) == 0 && Float.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) * 31 + Float.hashCode(y);
    }
}
