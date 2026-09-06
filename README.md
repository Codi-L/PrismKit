# PrismKit（棱镜）

一个面向 Minecraft 特效开发的通用工具库模组。

## 当前开发内容

### 贝塞尔曲线工具

- [x] 基于枢纽点与切线的多段贝塞尔曲线算法
- [x] 曲线 JSON 加载、保存与重载
- [x] 贝塞尔曲线调试显示器
- [ ] 实时编辑贝塞尔曲线

曲线使用 `pivot_points` 数组描述。每个枢纽点支持 `SMOOTH`、`LINEAR` 和 `SPLIT` 三种模式；输入超出标准范围时可选择 `CLAMP`、`REPEAT` 或 `MIRROR` 处理方式。内置示例曲线位于 `src/main/resources/data/prismkit/curves/`。

### PrismSystem 特效系统

正在开发中。

## 版本与运行环境

- Minecraft 1.21.1
- NeoForge 21.1.249
- Java 21

本分支使用 NeoForge 和 ModDevGradle 构建。运行 `./gradlew build`（Windows 使用 `gradlew.bat build`）后，模组 JAR 位于 `build/libs/`。

运行 `./gradlew test`（Windows 使用 `gradlew.bat test`）可验证曲线计算、JSON 往返和内置曲线资源。

## 关于作者

- E-mail: codi.l@qq.com
- QQ: 2542949224
- BiliBili: [传送门](https://space.bilibili.com/402284936?spm_id_from=333.788.0.0)
