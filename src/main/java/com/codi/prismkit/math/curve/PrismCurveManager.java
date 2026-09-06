package com.codi.prismkit.math.curve;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PrismCurve 的全局管理器。
 * 负责曲线的加载、缓存、保存和查询。
 *
 * 设计意图：
 * - 使用单例集中管理按名称索引的曲线
 * - 数据包曲线提供默认值，配置目录中的同名曲线具有更高优先级
 * - 曲线常驻内存，查询时不重复读取文件
 * - 支持按名称或整体显式重载
 *
 * 文件存储位置：
 * - 开发环境：<workspace>/run/config/prismkit/curves/
 * - 生产环境：<minecraft>/config/prismkit/curves/
 */
public class PrismCurveManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PrismCurveManager INSTANCE = new PrismCurveManager();

    // 曲线缓存：curveName -> PrismCurve
    private final Map<String, PrismCurve> curveCache = new ConcurrentHashMap<>();
    private final Gson gson = PrismCurveCodec.createGson();

    // 用户曲线目录，在模组公共初始化阶段设置
    private Path curvesDirectory;

    private PrismCurveManager() {
    }

    /**
     * 获取全局曲线管理器。
     *
     * @return 曲线管理器单例
     */
    public static PrismCurveManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置用户曲线目录，并加载其中已有的曲线。
     * 数据包资源管理器在此阶段可能尚未就绪，因此数据包曲线由同步事件统一加载。
     *
     * @param configDir Minecraft 配置目录
     */
    public void initialize(Path configDir) {
        curvesDirectory = configDir.resolve("prismkit").resolve("curves");

        try {
            Files.createDirectories(curvesDirectory);
            LOGGER.info("PrismCurve 用户目录: {}", curvesDirectory);
            loadCurvesFromConfigDirectory();
        } catch (IOException e) {
            LOGGER.error("创建 PrismCurve 目录失败: {}", curvesDirectory, e);
        }
    }

    /**
     * 从当前服务器获取数据包资源管理器。
     */
    private ResourceManager getResourceManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.debug("服务端资源管理器尚未就绪，跳过数据包曲线加载");
            return null;
        }
        return server.getResourceManager();
    }

    /**
     * 从 data/prismkit/curves/ 加载数据包曲线。
     * 已存在的同名缓存会被数据包版本覆盖。
     */
    public void loadCurvesFromDataDirectory() {
        ResourceManager resourceManager = getResourceManager();
        if (resourceManager == null) {
            return;
        }

        try {
            Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    "curves",
                    location -> location.getNamespace().equals("prismkit")
                            && location.getPath().endsWith(".json")
            );

            int loadedCount = 0;
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (InputStream stream = entry.getValue().open()) {
                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    PrismCurve curve = gson.fromJson(json, PrismCurve.class);
                    curveCache.put(curve.getName(), curve);
                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.error("解析曲线文件失败: {}", entry.getKey(), e);
                }
            }

            LOGGER.info("从数据包加载 {} 条曲线", loadedCount);
        } catch (Exception e) {
            LOGGER.error("扫描数据包曲线失败", e);
        }
    }

    /**
     * 从 config/prismkit/curves/ 加载用户曲线。
     * 用户曲线最后加载，因此会覆盖数据包中的同名曲线。
     */
    private void loadCurvesFromConfigDirectory() {
        if (curvesDirectory == null || !Files.exists(curvesDirectory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(curvesDirectory, "*.json")) {
            int loadedCount = 0;
            for (Path file : stream) {
                try {
                    loadCurveFromFile(file);
                    loadedCount++;
                } catch (Exception e) {
                    LOGGER.error("加载用户曲线失败: {}", file, e);
                }
            }

            if (loadedCount > 0) {
                LOGGER.info("从配置目录加载 {} 条用户曲线", loadedCount);
            }
        } catch (IOException e) {
            LOGGER.error("扫描用户曲线目录失败: {}", curvesDirectory, e);
        }
    }

    /**
     * 从指定文件加载一条曲线并更新缓存。
     *
     * @param filePath 曲线 JSON 文件
     * @throws IOException 文件读取失败时抛出
     * @throws JsonParseException JSON 结构无效时抛出
     */
    private void loadCurveFromFile(Path filePath) throws IOException, JsonParseException {
        String json = Files.readString(filePath, StandardCharsets.UTF_8);
        PrismCurve curve = gson.fromJson(json, PrismCurve.class);
        curveCache.put(curve.getName(), curve);
        LOGGER.debug("加载曲线: {} <- {}", curve.getName(), filePath.getFileName());
    }

    // ========== 公共查询 API ==========

    /**
     * 根据名称计算曲线值。
     *
     * @param curveName 曲线名称，不含 .json 后缀
     * @param x 输入值，通常是 [0, 1] 范围内的进度
     * @return 曲线输出值；曲线不存在时返回输入值作为线性回退
     */
    public float getCurveValue(String curveName, float x) {
        PrismCurve curve = curveCache.get(curveName);
        if (curve == null) {
            LOGGER.warn("未找到曲线 '{}'，使用线性回退", curveName);
            return x;
        }
        return curve.getValue(x);
    }

    /**
     * 检查曲线是否已加载。
     *
     * @param curveName 曲线名称
     * @return 曲线存在时返回 true
     */
    public boolean hasCurve(String curveName) {
        return curveCache.containsKey(curveName);
    }

    /**
     * 获取已加载的曲线对象。
     *
     * @param curveName 曲线名称
     * @return 包含曲线的 Optional；不存在时为空
     */
    public Optional<PrismCurve> getCurve(String curveName) {
        return Optional.ofNullable(curveCache.get(curveName));
    }

    // ========== 保存与重载 API ==========

    /**
     * 将曲线保存到用户曲线目录，并立即更新缓存。
     *
     * @param curve 要保存的曲线
     * @throws IOException 文件写入失败时抛出
     */
    public void saveCurve(PrismCurve curve) throws IOException {
        ensureInitialized();

        Path filePath = curvesDirectory.resolve(curve.getName() + ".json");
        Files.writeString(
                filePath,
                gson.toJson(curve),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        curveCache.put(curve.getName(), curve);
        LOGGER.info("保存曲线: {} -> {}", curve.getName(), filePath);
    }

    /**
     * 从用户曲线目录重新加载指定曲线。
     *
     * @param curveName 要重新加载的曲线名称
     */
    public void reloadCurve(String curveName) {
        ensureInitialized();

        Path filePath = curvesDirectory.resolve(curveName + ".json");
        if (!Files.exists(filePath)) {
            LOGGER.warn("曲线文件不存在，无法重新加载: {}", filePath);
            return;
        }

        try {
            loadCurveFromFile(filePath);
            LOGGER.info("重新加载曲线: {}", curveName);
        } catch (Exception e) {
            LOGGER.error("重新加载曲线失败: {}", curveName, e);
        }
    }

    /**
     * 按“数据包、用户配置”的优先级重新加载全部曲线。
     */
    public void reloadAll() {
        ensureInitialized();

        LOGGER.info("开始重新加载全部曲线");
        curveCache.clear();
        loadCurvesFromDataDirectory();
        loadCurvesFromConfigDirectory();
    }

    /**
     * 获取当前缓存的曲线数量。
     *
     * @return 已加载曲线数量
     */
    public int getCurveCount() {
        return curveCache.size();
    }

    /**
     * 清空曲线缓存。
     */
    public void clear() {
        curveCache.clear();
        LOGGER.info("PrismCurve 缓存已清空");
    }

    private void ensureInitialized() {
        if (curvesDirectory == null) {
            throw new IllegalStateException("PrismCurveManager 未初始化，请先调用 initialize()");
        }
    }
}
