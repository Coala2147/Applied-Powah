# Applied Powah 指南（Guidebook 文稿）

> 供游戏内指南与文档站使用。语气对齐 AE2 / ExtendedAE 等工程文档：陈述事实与步骤，避免口语化表述。
> 源码位置：`docs/guidebook/`。

## index

1. [简介](intro.md)
2. [高密度能源元件](energy_cells.md)
3. [AE / ME 充能棒](energizing_rods.md)
4. [放置与连接](placement.md)
5. [配方](recipes.md)
6. [配置](config.md)

---

## 简介（intro.md）

**Applied Powah** 是面向 Minecraft 1.20.1 Forge 的 AE2 扩展，提供：

- 高容量 **AE 能源元件**（超密 / 极密）。
- **AE / ME 充能棒**：自 ME 网络取能，并向邻近的 **Powah Energizing Orb** 输出能量。

前置：Minecraft 1.20.1 + Forge、**Applied Energistics 2**。建议安装 Powah；ME 充能棒需要 Applied Flux。

---

## 高密度能源元件（energy_cells.md）

| 方块 | 容量 | 合成 |
|------|------|------|
| 超密能源元件 | 12.8M AE | 8× AE2 能源元件 + 1× 运算处理器 |
| 极密能源元件 | 102.4M AE | 8× 超密能源元件 + 1× 运算处理器 |

物品 tooltip 采用 AE2 句式：`已存储能源: {cur}/{max} AE ({pct})`，正文为浅灰色。

---

## AE / ME 充能棒（energizing_rods.md）

七档：初级 → 基础 → 硬化 → 烈焰 → 钻石 → 富生 → 下界。

| 类型 | 取能来源 | 计量单位 |
|------|----------|----------|
| **AE 充能棒** | ME 网格中的 **AE** | AE（内部按 FE 缓存，显示 AE ≈ FE/2） |
| **ME 充能棒** | ME 网络中的 **FE**（Applied Flux） | FE |

- 输出（喂充能台）：按档位 transfer（与 Powah 默认一致；例如下界约 200k FE/t）。
- 节点需持有频道方可从网络取能；无频道时不工作。
- 网格默认保留上限的 5%（`networkReserveRatio`，可配置）。
- 邻近存在 **Powah Energizing Orb** 且其中有合法配方时，棒会向其输出能量。
- 挖下后不保留内部能量。

物品 tooltip 仅展示规格（缓存 / 输出 / 取能来源），不显示放置后的实时能量。

---

## 放置与连接（placement.md）

充能棒为完整方块：

1. 必须邻接 **AE2 线缆**（玻璃 / 包层 / 智能等）放置，否则无法放置。
2. 对准某根线缆放置时，`facing` 指向该线缆。
3. 仅一根邻接线缆时自动朝向该线缆。
4. 多根且未对准时：默认朝南，其次朝东。
5. 线缆被破坏后，棒可能掉落或重新对准其它线缆。

碰撞体积为细杆（与 Powah 充能棒一致），非整格实心。

---

## 配方（recipes.md）

- **AE 棒（某档）**：十字形 4× AE2 能源元件 + 中心同档 Powah 充能棒。
- **ME 棒（某档）**：四角 4× AE2 能源元件 + 中心同档 Powah 充能棒。
- **升级**：材料与 Powah 对应档位配方相同，将棒位替换为上一档 Applied Powah 棒。
- **AE ↔ ME**：同档无序互转。
- 缺少 Powah 或 Applied Flux 时，对应配方自动禁用。

安装 JEI 后，棒会作为 catalyst 出现在 Powah **Energizing** 类目中（表示可向充能台供能），工作台配方仍按 R 查看。

---

## 配置（config.md）

`config/applied_powah-common.toml`（以实际生成文件为准）：

| 键 | 默认 | 含义 |
|----|------|------|
| `pullIntervalTicks` | 1 | 从网络抽取的间隔（tick） |
| `networkReserveRatio` | 0.05 | AE 网格须保留的能量比例 |
| `aeBurstAe` | 10000000 | AE 棒每次抽取量（AE） |
| `meBurstFe` | 20000000 | ME 棒每次抽取量（FE） |

数值可按整合包需求调整。
