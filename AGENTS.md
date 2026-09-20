# AGENTS.md — Applied Powah

面向 AI/协作者的工程约定。设计事实以 `docs/工程指导.md` + `docs/纠正.md` + `refs/` 源码为准；`docs/archive/` 内旧 ARCHITECTURE **不作依据**。

## 身份

| 项 | 值 |
|----|-----|
| 显示名 | Applied Powah |
| Mod ID | `applied_powah` |
| 包名 | `com.coala.appliedpowah` |
| MC / Loader | 1.20.1 Forge 47+（先 Forge；NeoForge 日后 fork） |
| 许可证 | LGPL-3.0-only |
| 产物 | `releases/AppliedPowah-<mod_version>.jar` |
| refs | **只读**，禁止写入 |

## 硬约束

1. **不魔改 AE2**；软依赖门控：无 Powah → 无棒/台交互与棒配方；无 AppFlux → 无 ME 抽 FE；**mod 不得因缺前置崩溃**。
2. **AE2 为硬前置**（cells/rods 都依赖 AE2 API）。
3. 充能棒形态：**完整方块实体**（非 cable-bus part）。邻接 **AE2 线缆** 才能放置；`facing` 指向线缆；仅在 facing 侧接入网络。
4. 碰撞：**细杆**（与 Powah Energizing Rod 相同 VoxelShape），禁止完整立方碰撞。
5. 贴图：充能棒使用 Powah **组合模型**（`energizing_rod` 棒体 + 各档 `*_gem`）；1.0 正式版前应替换为自有贴图。
6. 不要在物品 tooltip 上伪造「当前存储」数值（物品不含 BE 能量）；能量展示以后做在 **放置后的方块/Jade**。
7. tick **禁止** `TickRateModulation.FASTER` 造成越充越快；固定节奏 + config 间隔。
8. AE 网络取能须保留网格上限 **5%**（`networkReserveRatio`，可配置）。
9. 版本号 `mod_version` 必须 **Maven 合法**（禁止 `a.1.0.0` 这类以字母开头）。
10. `logs/`、`refs/`、`.workbuddy/` **不入 git**。

## 取能设计（已拍板）

| 项 | AE 棒 | ME 棒 |
|----|-------|-------|
| 能源 | ME 网格 **AE** | ME 网络 **FE**（Applied Flux `FluxKey`） |
| 缓存 | Powah config（如 nitro 20M FE） | 同左 |
| 入能 | 统一节奏；AE 爆发默认 10M AE/次 | FE 爆发默认 20M/次 |
| 换算 | 1 AE = 2 FE（AE2 `ForgeEnergy=0.5`） | — |
| 出能（喂台） | 档位 transfer（FE/t，如 nitro 200k） | 同左 |
| 频道 | 无频道/节点不活跃则 **不抽能**；有频道 idle **1.0 AE/t** | 同左 |

## 放置规则

1. 对着线缆放置 → facing = 该线缆（最高优先）  
2. 仅一根邻接线缆 → facing = 该线缆  
3. 多根且未对准 → **南**，否则 **东**，否则任选一根  
4. 无线缆 → **不可放置**；之后线缆消失 → 方块掉落  

## 配方（摘要）

- AE：`_ a _ / a b a / _ a _`，`a`=ae2:energy_cell，`b`=同档 `powah:energizing_rod_*`  
- ME：`a _ a / _ b _ / a _ a`  
- 升级：Powah 配方，棒位换成 **上一档 AP 棒**  
- AE↔ME：无序互转（每档一对）  
- 元件：8×上一级 + ae2:calculation_processor  

## 构建与代理

- JDK：`D:\Program Files\Java\jdk-17`  
- 代理：`127.0.0.1:7890`（见 `gradle.properties` systemProp）  
- 依赖（Modrinth Maven）：AE2 `15.4.10`，Powah `5.0.11-forge`，AppFlux `1.20-1.3.7-forge`  
- 递增版本：只改 `mod_version`，产物名自动变为 `AppliedPowah-<mod_version>.jar`  

## Git 提交纪律

- **远程**：`origin = https://github.com/Coala2147/Applied-Powah.git`（仅此仓库）。  
- **标题要短**：`feat: …`、`fix: …`、`docs: …`、`chore: …`（中文或极短英文均可）；禁止长段英文标题。  
- **功能改动先拉分支**（如 `feat/ex-orb`、`fix/tooltip-style`），测试通过后再 **merge 回 master**。  
- **版本推进**（新 `mod_version` / release jar）后：合并 + 短标题 commit；`refs/`、`logs/`、`libs/`、`.workbuddy/` 不入库。  

**下一轮功能计划见：`docs/PLAN_高级充能台与指导体系.md`。**

## VoxelShape 注意

`Shapes.box` / `Block.box` 的坐标必须 **min ≤ max**；方向朝东/朝南时杆身轴向容易写反导致  
`IllegalArgumentException: The min values need to be smaller or equals to the max values`，  
进而 `ExceptionInInitializerError` 使 **整个注册阶段崩溃**（mod 无法加载）。

## 文档

| 文件 | 用途 |
|------|------|
| `README.md` | 项目总览 |
| `docs/工程指导.md` | 现行工程指导 |
| `docs/工程完整理解与架构核验.md` | 源码核验与分层事实 |
| `docs/纠正.md` | 用户意图原文（高可信） |
| `docs/guidebook/` | 游戏内指南文稿 |
| `releases/README.md` | 发行包说明 |

## 代码地图

```
com.coala.appliedpowah
├── AppliedPowah.java          # @Mod 入口、创造栏、客户端 BER
├── chargingrod/               # 完整方块棒 + BE + 模型/放置
├── energycell/                # 超密/极密 + 注册中心（含棒）
├── config/APConfig.java       # 间隔、保留比例、爆发量
├── integration/{ae2,powah,appflux}/
└── client/EnergizingRodRenderer.java  # 光柱
```
