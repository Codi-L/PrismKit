package com.codi.prismkit.client.debug;

import com.codi.prismkit.PrismKit;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

/**
 * PrismCurve 调试渲染器
 * 在游戏界面上可视化显示指定的贝塞尔曲线
 * 
 * 设计意图：
 * - 让开发者实时预览曲线效果，无需反复进出游戏
 * - 使用红色线条绘制曲线，背景透明不遮挡游戏画面
 * - 坐标系统：屏幕左下角 (0,0)，右上角 (1,1)
 * 
 * 使用方法：
 * 在 Config.java 中设置 debugCurveName = "fade_in_smooth"
 * 进入游戏后曲线会自动显示在屏幕上
 */
@OnlyIn(Dist.CLIENT)
public class PrismCurveDebugRenderer {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 调试配置：要显示的曲线名称（为空则不显示）
    private static String debugCurveName = "";
    
    // 曲线采样点数量（越多越平滑，但性能开销越大）
    private static final int SAMPLE_POINTS = 200;
    
    // 曲线颜色（RGBA，红色）
    private static final float RED = 1.0f;
    private static final float GREEN = 0.0f;
    private static final float BLUE = 0.0f;
    private static final float ALPHA = 1.0f;
    
    // 线条粗细（像素）
    private static final float LINE_WIDTH = 2.0f;

    /**
     * 设置要调试显示的曲线
     * 
     * @param curveName 曲线名称（如 "fade_in_smooth"），传入 null 或空字符串则关闭显示
     */
    public static void setDebugCurve(String curveName) {
        debugCurveName = (curveName == null) ? "" : curveName;
        if (!debugCurveName.isEmpty()) {
            LOGGER.info("[PrismCurve Debug] 设置调试曲线: {}", debugCurveName);
        } else {
            LOGGER.info("[PrismCurve Debug] 关闭曲线显示");
        }
    }

    /**
     * 获取当前正在调试的曲线名称
     */
    public static String getDebugCurveName() {
        return debugCurveName;
    }

    /**
     * 渲染曲线到屏幕
     * 使用 GuiGraphics 内置方法绘制，确保与 Minecraft GUI 渲染系统兼容
     * 
     * @param guiGraphics Minecraft 的 GUI 渲染上下文
     */
    public static void renderCurve(GuiGraphics guiGraphics) {
        // 如果未设置调试曲线，直接返回
        if (debugCurveName.isEmpty()) {
            return;
        }
        
        // 检查曲线是否存在
        if (!PrismKit.hasCurve(debugCurveName)) {
            LOGGER.warn("[PrismCurve Debug] 曲线 '{}' 不存在", debugCurveName);
            return;
        }
        
        LOGGER.debug("[PrismCurve Debug] 开始渲染曲线: {}", debugCurveName);
        
        // 使用 GUI 缩放后的尺寸（与 GuiGraphics 一致）
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        LOGGER.debug("[PrismCurve Debug] GUI 屏幕尺寸: {}x{}", screenWidth, screenHeight);
        
        // 采样曲线并用直线连接点
        int prevX = -1;
        int prevY = -1;
        
        for (int i = 0; i < SAMPLE_POINTS; i++) {
            // 计算归一化的 x 坐标（0 到 1）
            float normalizedX = i / (float) (SAMPLE_POINTS - 1);
            
            // 通过 PrismKit API 获取对应的 y 值
            float normalizedY = PrismKit.getCurveValue(debugCurveName, normalizedX);
            
            // 转换为屏幕坐标（GUI 缩放后的逻辑坐标）
            int screenX = (int) (normalizedX * screenWidth);
            int screenY = (int) (screenHeight - (normalizedY * screenHeight));  // 反转 y 轴
            
            // 从第二个点开始，用直线连接前一个点和当前点
            if (prevX >= 0) {
                // 使用 GuiGraphics.fill 绘制粗线（通过多个平行线模拟粗线）
                int thickness = (int) LINE_WIDTH;
                // 颜色格式：ARGB，RED = 0xFFFF0000
                int red = 0xFFFF0000;  // 不透明红色
                for (int t = -thickness/2; t <= thickness/2; t++) {
                    drawLine(guiGraphics, prevX, prevY + t, screenX, screenY + t, red);
                }
            }
            
            prevX = screenX;
            prevY = screenY;
        }
        
        LOGGER.debug("[PrismCurve Debug] 渲染完成");
    }
    
    /**
     * 使用 Bresenham 算法绘制直线
     * 通过 GuiGraphics.fill 绘制单个像素
     */
    private static void drawLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1
                ? 1
                : -1;
        int sy = y0 < y1
                ? 1
                : -1;
        int err = dy - dx;
        
        while (true) {
            // 绘制单个像素（使用 fill 填充 1x1 的矩形）
            guiGraphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            
            if (x0 == x1 && y0 == y1) {
                break;
            }
            
            int e2 = 2*err;
            if (e2 < dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 > -dx) {
                err -= dx;
                y0 += sy;
            }
        }
    }

    /**
     * 渲染坐标轴（辅助功能，可选）
     * 绘制白色的 X 轴和 Y 轴，帮助理解坐标系统
     *
     * @param guiGraphics Minecraft 的 GUI 渲染上下文
     */
    public static void renderAxes(GuiGraphics guiGraphics) {
        if (debugCurveName.isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // 白色半透明（ARGB: 0x4DFFFFFF 约 30% 透明度）
        int whiteAlpha = 0x4DFFFFFF;
        
        // 绘制 X 轴（底部，水平线）
        drawLine(guiGraphics, 0, screenHeight - 1, screenWidth - 1, screenHeight - 1, whiteAlpha);
        
        // 绘制 Y 轴（左侧，垂直线）
        drawLine(guiGraphics, 0, 0, 0, screenHeight - 1, whiteAlpha);
    }

    /**
     * 渲染调试信息文本（显示当前曲线名称）
     * 
     * @param guiGraphics Minecraft 的 GUI 渲染上下文
     */
    public static void renderDebugText(GuiGraphics guiGraphics) {
        if (debugCurveName.isEmpty()) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        String text = "Debug Curve: " + debugCurveName;
        
        // 在屏幕右上角显示文本
        int textWidth = mc.font.width(text);
        int x = mc.getWindow().getGuiScaledWidth() - textWidth - 10;
        int y = 10;
        
        // 绘制半透明黑色背景
        guiGraphics.fill(x - 2, y - 2, x + textWidth + 2, y + mc.font.lineHeight + 2, 0x80000000);
        
        // 绘制文本（红色）
        guiGraphics.drawString(mc.font, text, x, y, 0xFFFF0000);
    }
}
