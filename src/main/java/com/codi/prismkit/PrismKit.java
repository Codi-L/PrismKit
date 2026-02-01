package com.codi.prismkit;

import com.codi.prismkit.client.debug.PrismCurveDebugRenderer;
import com.codi.prismkit.math.curve.PrismCurveManager;
import com.codi.prismkit.registry.PKEntityRegister;
import com.codi.prismkit.registry.PKParticleRegister;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

@Mod(PrismKit.MOD_ID)
public class PrismKit {
    public static final String MOD_ID = "prismkit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PrismKit(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        PKEntityRegister.register(modEventBus);
        PKParticleRegister.register(modEventBus);

        // 注册公共设置事件
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 公共设置阶段：初始化 PrismCurve 管理器
     * 这个方法在服务端和客户端都会执行
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 运行单元测试（开发阶段验证功能）
            //PrismCurveTest.runAllTests();

            // 初始化曲线管理器
            PrismCurveManager.getInstance().initialize(FMLPaths.CONFIGDIR.get());
            LOGGER.info("PrismKit 初始化完成，已加载 {} 个曲线",
                    PrismCurveManager.getInstance().getCurveCount());

            // 设置默认调试曲线（可以在这里修改要显示的曲线）
            // 如果不需要显示，注释掉下面这行
            PrismCurveDebugRenderer.setDebugCurve("mountain2");
        });
    }

    // ========== 公共 API：供其他模组调用 ==========

    /**
     * 获取曲线的值（主要 API）
     *
     * @param curveName 曲线名称
     * @param x         输入值（横轴，通常是 0 到 1 的进度值）
     * @return 对应的输出值（纵轴）
     * <p>
     * 使用示例：
     * float opacity = PrismKit.getCurveValue("fade_in", progress);
     */
    public static float getCurveValue(String curveName, float x) {
        return PrismCurveManager.getInstance().getCurveValue(curveName, x);
    }

    /**
     * 检查曲线是否存在
     *
     * @param curveName 曲线名称
     * @return 如果曲线已加载返回 true
     */
    public static boolean hasCurve(String curveName) {
        return PrismCurveManager.getInstance().hasCurve(curveName);
    }

    // ========== 调试 API ==========

    /**
     * 设置要在屏幕上显示的调试曲线
     *
     * @param curveName 曲线名称（如 "fade_in_smooth"），传入 null 则关闭显示
     *                  <p>
     *                  使用示例：
     *                  PrismKit.setDebugCurve("fade_in_smooth");  // 显示曲线
     *                  PrismKit.setDebugCurve(null);              // 关闭显示
     */
    public static void setDebugCurve(String curveName) {
        PrismCurveDebugRenderer.setDebugCurve(curveName);
    }

    /**
     * 获取当前正在调试显示的曲线名称
     *
     * @return 曲线名称，如果未设置则返回空字符串
     */
    public static String getDebugCurveName() {
        return PrismCurveDebugRenderer.getDebugCurveName();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(PKParticleRegister.TEST_PARTICLE.get(),
                    com.codi.prismkit.client.particle.TestParticle.Provider::new);
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(PKEntityRegister.LASER.get(),
                    com.codi.prismkit.client.renderer.LaserRenderer::new);
        }
    }

    /**
     * 客户端 Forge 事件监听器（游戏运行时事件）
     * 用于监听渲染事件并绘制调试曲线
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        /**
         * GUI 渲染事件：在所有 GUI 元素绘制完成后绘制调试曲线
         * 这样曲线会覆盖在游戏界面最上层
         */
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            // 先绘制坐标轴（可选，帮助理解坐标系统）
            PrismCurveDebugRenderer.renderAxes(event.getGuiGraphics());

            // 绘制曲线
            PrismCurveDebugRenderer.renderCurve(event.getGuiGraphics());

            // 绘制调试信息文本
            PrismCurveDebugRenderer.renderDebugText(event.getGuiGraphics());
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            // 资源包同步完成后重新加载曲线
            PrismCurveManager.getInstance().loadCurvesFromDataDirectory();
        }
    }
}
