package com.codi.prismkit.math.curve;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * PrismCurve 的全局管理器
 * 负责曲线的加载、缓存、保存和查询
 * 
 * 设计意图：
 * - 单例模式，确保全局只有一个管理器实例
 * - 内存缓存所有已加载的曲线，避免重复读取文件
 * - 支持热重载（监听文件变化自动更新）
 * - 线程安全的 API 设计
 * 
 * 文件存储位置：
 * - 开发环境：<workspace>/run/config/prismkit/curves/
 * - 生产环境：<minecraft>/config/prismkit/curves/
 */
public class PrismCurveManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 单例实例
    private static PrismCurveManager instance;
    
    // 曲线缓存：name -> PrismCurve
    private final Map<String, PrismCurve> curveCache;
    
    // JSON 解析器
    private final Gson gson;
    
    // 曲线文件存储目录
    private Path curvesDirectory;

    /**
     * 私有构造函数（单例模式）
     */
    private PrismCurveManager() {
        this.curveCache = new HashMap<>();
        this.gson = PrismCurveCodec.createGson();
    }

    /**
     * 获取管理器的单例实例
     */
    public static PrismCurveManager getInstance() {
        if (instance == null) {
            instance = new PrismCurveManager();
        }
        return instance;
    }

    /**
     * 初始化管理器：设置存储目录并加载所有曲线
     * 这个方法应该在 Mod 启动时调用一次
     * 
     * @param configDir Minecraft 的 config 目录（用于用户自定义曲线）
     */
    public void initialize(Path configDir) {
        // 设置用户自定义曲线存储路径：config/prismkit/curves/
        this.curvesDirectory = configDir.resolve("prismkit").resolve("curves");
        
        try {
            // 如果目录不存在则创建
            Files.createDirectories(curvesDirectory);
            LOGGER.info("PrismCurve 用户目录: {}", curvesDirectory);
            
            // 先加载内置曲线（从代码创建）
            //loadBuiltInCurves();
            
            // 导出内置曲线为 JSON 文件到 data 目录
            //exportBuiltInCurvesToDataDirectory();
            
            // 加载 data 目录中的曲线（资源文件）
            //loadCurvesFromDataDirectory();
            
            // 最后加载 config 目录中的曲线（用户自定义曲线会覆盖内置曲线）
            loadCurvesFromConfigDirectory();
            
        } catch (IOException e) {
            LOGGER.error("创建 PrismCurve 目录失败: {}", curvesDirectory, e);
        }
    }

    /**
     * 加载内置曲线（代码中预定义的基础曲线）
     * 这些曲线可以被 config 目录中的 JSON 文件覆盖
     */
    /*
    private void loadBuiltInCurves() {
        try {
            // 淡入曲线：平滑的 S 型曲线
            PrismCurve fadeInSmooth = new PrismCurve(
                "fade_in_smooth",
                new CurveControlPoint(0.0f, 0.0f),
                new CurveControlPoint(0.3f, 0.1f),
                new CurveControlPoint(0.7f, 0.9f),
                new CurveControlPoint(1.0f, 1.0f),
                CurveClampMode.CLAMP
            );
            curveCache.put(fadeInSmooth.getName(), fadeInSmooth);
            
            // 弹跳曲线：超过 1.0 后回落
            PrismCurve bounce = new PrismCurve(
                "bounce",
                new CurveControlPoint(0.0f, 0.0f),
                new CurveControlPoint(0.5f, 1.2f),
                new CurveControlPoint(0.8f, 0.9f),
                new CurveControlPoint(1.0f, 1.0f),
                CurveClampMode.CLAMP
            );
            curveCache.put(bounce.getName(), bounce);
            
            // 脉冲曲线：循环上升下降
            PrismCurve pulse = new PrismCurve(
                "pulse",
                new CurveControlPoint(0.0f, 0.0f),
                new CurveControlPoint(0.33f, 1.0f),
                new CurveControlPoint(0.67f, 1.0f),
                new CurveControlPoint(1.0f, 0.0f),
                CurveClampMode.REPEAT
            );
            curveCache.put(pulse.getName(), pulse);
            
            // 山峰曲线：多段贝塞尔曲线示例（3个枢纽点，2个段）
            List<CurveSegment> mountainSegments = new ArrayList<>();
            
            // 段1：上升段 (0.0 → 0.5)
            // 从底部快速上升到峰顶
            mountainSegments.add(new CurveSegment(
                new CurveControlPoint(0.0f, 0.0f),   // 枢纽点1：起点
                new CurveControlPoint(0.2f, 0.2f),   // 枢纽点1的右手柄
                new CurveControlPoint(0.3f, 0.9f),   // 枢纽点2的左手柄
                new CurveControlPoint(0.5f, 1.0f)    // 枢纽点2：峰顶
            ));
            
            // 段2：下降段 (0.5 → 1.0)
            // 从峰顶缓慢下降到底部
            mountainSegments.add(new CurveSegment(
                new CurveControlPoint(0.5f, 1.0f),   // 枢纽点2：峰顶（与段1结束点相同）
                new CurveControlPoint(0.7f, 0.9f),   // 枢纽点2的右手柄
                new CurveControlPoint(0.8f, 0.2f),   // 枢纽点3的左手柄
                new CurveControlPoint(1.0f, 0.0f)    // 枢纽点3：终点
            ));
            
            PrismCurve mountain = new PrismCurve("mountain", mountainSegments, CurveClampMode.CLAMP);
            curveCache.put(mountain.getName(), mountain);
            
            LOGGER.info("已加载 {} 个内置曲线（包含 1 个多段曲线）", 4);
            
        } catch (Exception e) {
            LOGGER.error("加载内置曲线失败", e);
        }
    }

     */

    /**
     * 导出内置曲线为 JSON 文件到 data 目录
     * 这样开发者可以查看和修改内置曲线的定义
     */
    private void exportBuiltInCurvesToDataDirectory() {
        // 获取项目的 data 目录路径
        // 开发环境: <workspace>/run/config -> <workspace> -> PrismKit/src/main/resources/data/prismkit/curves/
        // 生产环境: 曲线已打包在 JAR 中，无需导出
        
        try {
            // curvesDirectory = <workspace>/run/config/prismkit/curves/
            // 往上回溯：curves -> prismkit -> config -> run -> <workspace>
            Path runDir = curvesDirectory.getParent().getParent().getParent();
            
            LOGGER.info("[Debug] curvesDirectory = {}", curvesDirectory);
            LOGGER.info("[Debug] runDir = {}", runDir);
            LOGGER.info("[Debug] runDir.getFileName() = {}", runDir.getFileName());
            
            // 检查是否是开发环境（存在 run 目录）
            if (!runDir.getFileName().toString().equals("run")) {
                LOGGER.info("非开发环境（run 目录名称为: {}），跳过导出内置曲线", runDir.getFileName());
                return;
            }
            
            // 工作区根目录
            Path workspaceRoot = runDir.getParent();
            LOGGER.info("[Debug] workspaceRoot = {}", workspaceRoot);
            
            // 尝试定位 PrismKit 模组目录
            Path prismkitModule = workspaceRoot.resolve("PrismKit");
            LOGGER.info("[Debug] prismkitModule = {}, exists = {}", prismkitModule, Files.exists(prismkitModule));
            
            if (!Files.exists(prismkitModule)) {
                LOGGER.info("找不到 PrismKit 模组目录，跳过导出");
                return;
            }
            
            // data 目录路径：PrismKit/src/main/resources/data/prismkit/curves/
            Path dataDir = prismkitModule.resolve("src").resolve("main").resolve("resources")
                                        .resolve("data").resolve("prismkit").resolve("curves");
            
            Files.createDirectories(dataDir);
            LOGGER.info("导出内置曲线到: {}", dataDir);
            
            // 导出所有内置曲线
            int exportedCount = 0;
            for (PrismCurve curve : curveCache.values()) {
                Path filePath = dataDir.resolve(curve.getName() + ".json");
                String json = gson.toJson(curve);
                Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                LOGGER.debug("导出曲线: {} -> {}", curve.getName(), filePath.getFileName());
                exportedCount++;
            }
            
            LOGGER.info("已导出 {} 个内置曲线到 data 目录", exportedCount);
            
        } catch (Exception e) {
            LOGGER.warn("导出内置曲线失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private ResourceManager getResourceManager() {
        // 关键修复：不管客户端还是服务端，都尝试获取服务端 ResourceManager
        // 因为 data/ 目录由服务端 ResourceManager 管理
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getResourceManager();
        }
        
        // 如果服务端未启动（比如初始化阶段），返回 null
        LOGGER.warn("[Debug] 服务端 ResourceManager 未就绪，可能是初始化阶段");
        return null;
    }

    /**
     * 从 data 目录加载曲线（资源文件）
     * 优先级：低于 config 目录，高于内置曲线
     * 
     * 使用 ClassLoader 读取资源，服务端和客户端都能工作
     */
    public void loadCurvesFromDataDirectory() {

        try {
            ResourceManager resourceManager = getResourceManager();

            if (resourceManager == null) {
                return;
            }

            Map<ResourceLocation, Resource> resourcesMap = resourceManager.listResources(
                    "curves",  // 修复：完整路径，不含 data/ 前缀
                    location -> {
                        return location.getNamespace().equals("prismkit") && location.getPath().endsWith(".json");
                    }
            );


            int loadedCount = 0;

            for (Map.Entry<ResourceLocation, Resource> entry : resourcesMap.entrySet()) {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();

                try (InputStream stream = resource.open()) {
                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    PrismCurve curve = gson.fromJson(json,PrismCurve.class);

                    curveCache.put(curve.getName(), curve);

                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.error("解析曲线文件失败: {}", location, e);
                }
            }


            if (loadedCount > 0) {
                LOGGER.info("从 data 目录加载 {} 个曲线（覆盖内置版本）", loadedCount);
            } else {
                LOGGER.info("未从 data 目录加载任何曲线，使用内置版本");
            }

        } catch (Exception e) {
            LOGGER.error("扫描 data 目录曲线失败", e);
        }
    }

    /**
     * 从 config 目录加载用户自定义曲线
     * 优先级最高，会覆盖 data 目录和内置曲线
     */
    private void loadCurvesFromConfigDirectory() {
        if (!Files.exists(curvesDirectory)) {
            LOGGER.info("配置目录不存在，跳过加载用户曲线: {}", curvesDirectory);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curvesDirectory, "*.json")) {
            int loadedCount = 0;
            for (Path file : stream) {
                try {
                    loadCurveFromFile(file);
                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.error("加载曲线文件失败: {}", file, e);
                }
            }
            if (loadedCount > 0) {
                LOGGER.info("从配置目录加载 {} 个用户自定义曲线", loadedCount);
            }
            
        } catch (IOException e) {
            LOGGER.error("扫描配置目录失败: {}", curvesDirectory, e);
        }
    }

    /**
     * 从文件加载单个曲线
     * 
     * @param filePath 曲线的 JSON 文件路径
     * @throws IOException 文件读取失败
     * @throws JsonParseException JSON 解析失败
     */
    private void loadCurveFromFile(Path filePath) throws IOException, JsonParseException {
        String json = Files.readString(filePath);
        PrismCurve curve = gson.fromJson(json, PrismCurve.class);
        
        // 存入缓存
        curveCache.put(curve.getName(), curve);
        LOGGER.debug("加载曲线: {} <- {}", curve.getName(), filePath.getFileName());
    }

    /**
     * 核心 API：根据名称获取曲线的值
     * 这是对外暴露的主要接口，供其他模组调用
     * 
     * @param curveName 曲线名称（不含 .json 后缀）
     * @param x 输入值（横轴，通常是 0 到 1）
     * @return 对应的输出值（纵轴），如果曲线不存在则返回 x 本身（线性回退）
     * 
     * 使用示例：
     * float opacity = PrismCurveManager.getInstance().getCurveValue("fade_in", progress);
     */
    public float getCurveValue(String curveName, float x) {

        PrismCurve curve = curveCache.get(curveName);
        
        if (curve == null) {
            LOGGER.warn("未找到曲线 '{}', 使用线性回退 (返回输入值)", curveName);
            return x; // 回退策略：返回原值（相当于 y=x 直线）
        }
        
        return curve.getValue(x);
    }

    /**
     * 检查曲线是否已加载
     * 
     * @param curveName 曲线名称
     * @return 如果曲线存在返回 true
     */
    public boolean hasCurve(String curveName) {
        return curveCache.containsKey(curveName);
    }

    /**
     * 获取曲线对象（Optional 包装）
     * 
     * @param curveName 曲线名称
     * @return Optional<PrismCurve>，如果不存在则为空
     */
    public Optional<PrismCurve> getCurve(String curveName) {
        return Optional.ofNullable(curveCache.get(curveName));
    }

    /**
     * 保存曲线到文件
     * 这个方法会在可视化编辑器中使用
     * 
     * @param curve 要保存的曲线对象
     * @throws IOException 文件写入失败
     */
    public void saveCurve(PrismCurve curve) throws IOException {
        if (curvesDirectory == null) {
            throw new IllegalStateException("PrismCurveManager 未初始化，请先调用 initialize()");
        }

        // 生成文件名：<curve_name>.json
        String fileName = curve.getName() + ".json";
        Path filePath = curvesDirectory.resolve(fileName);
        
        // 序列化为 JSON
        String json = gson.toJson(curve);
        
        // 写入文件
        Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // 更新缓存
        curveCache.put(curve.getName(), curve);
        
        LOGGER.info("保存曲线: {} -> {}", curve.getName(), filePath);
    }

    /**
     * 重新加载指定曲线（热重载功能）
     * 
     * @param curveName 要重载的曲线名称
     */
    public void reloadCurve(String curveName) {
        Path filePath = curvesDirectory.resolve(curveName + ".json");
        
        if (!Files.exists(filePath)) {
            LOGGER.warn("曲线文件不存在，无法重载: {}", filePath);
            return;
        }

        try {
            loadCurveFromFile(filePath);
            LOGGER.info("重新加载曲线: {}", curveName);
        } catch (Exception e) {
            LOGGER.error("重载曲线失败: {}", curveName, e);
        }
    }

    /**
     * 重新加载所有曲线
     */
    public void reloadAll() {
        LOGGER.info("开始重新加载所有曲线...");
        curveCache.clear();
        //loadBuiltInCurves();
        loadCurvesFromDataDirectory();
        loadCurvesFromConfigDirectory();
    }

    /**
     * 获取已加载曲线的数量
     */
    public int getCurveCount() {
        return curveCache.size();
    }

    /**
     * 清空所有缓存（通常在 Mod 卸载时调用）
     */
    public void clear() {
        curveCache.clear();
        LOGGER.info("PrismCurve 缓存已清空");
    }

    /**
     * 将两个控制点连接成一个曲线段
     *
     * @param p1 第一个PivotPoint
     * @param p2 第二个PivotPoint
     * @return 连接而成的CurveSegment
     */
    public CurveSegment linkPivotPoint(CurvePivotPoint p1, CurvePivotPoint p2) {
        return new CurveSegment(
                p1,
                p1.getTangentOutPoint(),
                p2.getTangentInPoint(),
                p2
        );
    }
}
