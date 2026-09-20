# Applied Powah

AE2 × Powah 缝合 mod（Minecraft **1.20.1 Forge**）。

- **许可证**：LGPL-3.0-only（与 AE2 同级）
- **Mod ID**：`applied_powah`
- **Java 包**：`com.coala.appliedpowah`
- **GitHub**：`https://github.com/Coala2147/Applied-Powah.git`
- **文档**：本 README、`docs/工程指导.md`、`AGENTS.md`、`docs/guidebook/`
- **发行 jar**：`releases/AppliedPowah-<mod_version>.jar`（旧版本保留不删）

## 功能（当前 alpha）

| 内容 | 说明 |
|------|------|
| 超密 / 极密能源元件 | 12.8M / 102.4M AE；合成 8×上一级 + 运算处理器 |
| AE Energizing Rod ×7 档 | **完整方块**，从 ME 网络抽 **AE**，喂附近 Powah 充能台 |
| ME Energizing Rod ×7 档 | **完整方块**，从 ME 网络抽 **Applied Flux FE**，喂充能台 |
| 配方 | AE 十字 / ME 四角 + 中心同档 Powah 棒；升级仿 Powah；AE↔ME 无序；缺前置自动关配方 |
| 放置 | **邻接 AE2 线缆才能放**；facing 朝向线缆（对准优先 → 单缆 → 南/东） |
| 碰撞 | 细杆状（同 Powah），**不是**完整立方体 |
| 光柱 | 有能量/正在推能时，棒与充能台之间显示光柱（贴图借自 Powah，1.0 前可换） |

## 前置

| 模组 | 必须？ |
|------|--------|
| Minecraft 1.20.1 + Forge 47+ | 是 |
| **Applied Energistics 2 15+** | **是** |
| Powah 5+ | 强烈建议（充能台与配方中心） |
| Applied Flux 1+ | ME 棒抽 FE 需要 |

## 构建

```
JAVA_HOME=D:\Program Files\Java\jdk-17
HTTP(S)_PROXY=http://127.0.0.1:7890   # gradle.properties 已写 systemProp
./gradlew build
# → releases/AppliedPowah-<mod_version>.jar
```

版本号改 `gradle.properties` 的 `mod_version`（须为 Maven 合法版本，如 `0.1.0-alpha.6`）。

## 源码布局

```
common/src/main/java/com/coala/appliedpowah/   # 逻辑
forge/src/main/java/.../AppliedPowah.java      # @Mod 入口
common/src/main/resources/assets/applied_powah # 模型/贴图/lang
common/src/main/resources/data/applied_powah   # 配方/战利品
docs/                                           # 工程指导、核验、指南
releases/                                       # 发行 jar
refs/                                           # 只读参考（AE2/Powah/AppFlux），不入仓
```

## 路线（摘录）

- **进行中**：完整方块充能棒、取能、喂充能台、光柱、本地化
- **后续**：Jade 能量显示、专用贴图（1.0 前）、无线充能棒（展望）、kubejs 路线（展望）
- 详见 `docs/工程指导.md`
