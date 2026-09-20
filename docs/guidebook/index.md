# Applied Powah 指南（Guidebook 文稿）

> 供游戏内手册 / 网页使用。源码位置：`docs/guidebook/`。

## index

1. [简介](intro.md)
2. [高密度能源元件](energy_cells.md)
3. [AE / ME 充能棒](energizing_rods.md)
4. [放置与连接](placement.md)
5. [配方](recipes.md)
6. [配置](config.md)

---

## 简介（intro.md）

**Applied Powah** 把 AE2 的网络能力接到 Powah 的充能玩法上：

- 更大容量的 **AE 能源元件**（超密 / 极密）。
- **AE / ME 充能棒**：从 ME 网络取能，喂给附近的 **Powah 充能台（Energizing Orb）**。

前置：Minecraft 1.20.1 + Forge、**Applied Energistics 2**；建议安装 Powah；ME 棒还需要 Applied Flux。

---

## 高密度能源元件（energy_cells.md）

| 方块 | 容量 | 合成 |
|------|------|------|
| 超密能源元件 | 12.8M AE | 8×AE2 能源元件 + 1×运算处理器 |
| 极密能源元件 | 102.4M AE | 8×超密 + 1×运算处理器 |

面向不想研究 Powah 的玩家；安装本 mod 后仍可只使用元件。

---

## AE / ME 充能棒（energizing_rods.md）

七档：初级 → 基础 → 硬化 → 烈焰 → 钻石 → 富生 → 下界。

| 类型 | 取能来源 | 内部概念 |
|------|----------|----------|
| **AE 充能棒** | ME 网络中的 **AE** | 按 AE 计（内部按 FE 缓存，显示时 AE≈FE/2） |
| **ME 充能棒** | ME 网络中的 **FE**（需 Applied Flux） | 按 FE 计 |

- **输出**（喂充能台）：按档位 transfer（例如下界约 200k FE/t，与 Powah 默认一致）。
- **需要频道**才能从网络取能；无频道时不工作。
- 放置后：靠近 **Powah 充能台** 且台内有配方时，会把能量推入充能台；有光柱表示正在工作。
- 挖下后 **不保留** 内部能量。

---

## 放置与连接（placement.md）

充能棒是 **完整方块**（不是线缆上的小零件）：

1. 必须放在 **AE2 线缆**（玻璃/包层/智能等，cable bus）旁边，否则无法放置。
2. **对着某根线缆** 放置 → 棒会 **朝向（face）** 那根线缆。
3. 只有一根邻接线缆时自动朝向它。
4. 多根且未对准时：默认朝 **南**，其次朝 **东**。
5. 线缆被破坏后，棒可能掉落或重新对准其它线缆。

碰撞体积与原版 Powah 棒类似：细杆，不是整格实心。

---

## 配方（recipes.md）

- **AE 棒（某档）**：十字四格 AE2 能源元件 + 中心同档 Powah 充能棒。  
- **ME 棒（某档）**：四角 AE2 能源元件 + 中心同档 Powah 充能棒。  
- **升级**：与 Powah 棒升级相同材料，把「上一档棒」换成对应 AP 棒。  
- **AE ↔ ME**：同档无序互转。  
- 缺 Powah / Applied Flux 时，相关配方会自动关闭。

---

## 配置（config.md）

`config/applied_powah-common.toml`（名称以实际生成为准）：

| 键 | 默认 | 含义 |
|----|------|------|
| `pullIntervalTicks` | 1 | 从网络抽取的间隔（tick） |
| `networkReserveRatio` | 0.05 | AE 网络必须保留的能量比例（5%） |
| `aeBurstAe` | 10000000 | AE 棒每次抽取的 AE 量 |
| `meBurstFe` | 20000000 | ME 棒每次抽取的 FE 量 |

具体数值可改；平衡请在整合包中调整。
