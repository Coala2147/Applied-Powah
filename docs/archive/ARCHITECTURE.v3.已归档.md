# Applied Powah — 工程架构与文件夹组织（v3，整体整理）

> 起草/更新：2026-09-20（v1→v2→v3 整体整理：充能棒定为 AE2 part 形态、删除历史误述、高密度元件补未来 TODO、明确 `refs/` 只读）
> 依据：`AppliedPowah_设计_取能机制与高密度能源元件.md`（设计文档 v4）、`纠正.md`、本地 `refs/` 源码（**只读参考**）
> 状态：**架构与文件夹组织起草阶段**，未建 git 仓库、未建可运行构建（构建需联网，见 §4）
> 关联设计文档：`../AppliedPowah_设计_取能机制与高密度能源元件.md`、`../纠正.md`

---

## 1. 工程目标（本工程到底要做什么）

**一句话**：做把 **AE2 × Powah** 能力缝合的 Minecraft mod（1.20.1，先只做 **Forge**），不魔改 AE2 源码，补齐两块缺口：①高密度能源元件（超密/极密，已定）；②AE/ME 充能棒（**AE2 part 形态**，喂 Powah 充能台）。可选接 Applied Flux 让 ME 棒抽 FE。

### 1.1 两个核心功能

| 功能 | 形态 | 机制 | 阶段 |
|------|------|------|------|
| **高密度能源元件**（超密 / 极密） | 方块（镜像 AE2 能源元件） | 复用 `EnergyCellBlock(容量, 充率, 优先级)` + 复用 `EnergyCellBlockEntity`；8×上一级 + 1×运算处理器 合成 | Phase 1（首选先做，风险最低） |
| **AE 充能棒 / ME 充能棒**（= AE/ME Energizing Rod，7 档） | **AE2 part（挂 cable bus）**：接入 AE 网络，从网络取 FE 入自身缓冲（"取能"），并作为馈电器把能量喂给附近的 Powah 充能台（Energizing Orb）以驱动其配方 | AE 棒经 `IEnergyService` 取 AE→转 FE；ME 棒经 `IStorageService`+`FluxKey.FE` 取 FE；**放电端 = 向充能台 `fillEnergy` 推能（"充能"），见 §3.7** | Phase 2–3 |

### 1.2 目标版本与 loader

- **Minecraft 1.20.1，先只做 Forge**（覆盖 ATM9 与绝大多数启动器）。
- **Architectury 双 loader（Forge+NeoForge）判定为不必要**：单 Forge 已满足需求；Architectury 仍提供 `common`/`forge` 的共享/平台拆分，使日后加 NeoForge 成本最低（§3.1）。
- 1.21.1 的 `refs/` 已就绪，仅作后续版本参考，**本期只做 1.20.1**。

### 1.3 硬约束

- **软依赖 + 配方门控**：Powah、Applied Flux 缺任一个，mod 都**不能崩**；**缺 Powah → 禁用全部充能棒（AE/ME）的合成配方**（充能棒唯一用途是喂充能台，无台则无意义，仅剩超密/极密能源元件）；**缺 Applied Flux → 仅禁用 ME 棒配方**（AE 棒从 AE 网格取能不需 Flux，仍可合成），仅剩 AE 棒 + 能源元件。
- **充能棒形态（确定）**：**AE2 part**，挂在 cable bus 上（接入 AE 网络），不是独立方块、不是手持便携充能器。支持 **四种 AE 线缆（glass / covered / smart / dense）及其颜色变体** 作为宿主。
- **"充能"是必须项**：充能棒必须**能充能**——①自身能被充入 FE（从 AE 网络"取能"），②放置后能把存储的 FE **喂给附近的 Powah 充能台**（经 `EnergizingOrb.fillEnergy`），驱动充能台产出（见 §3.7）。
- **贴图**：能源元件借用 AE2 致密/创造贴图；充能棒借用 **Powah 充能棒 `energizing_rod_*` 七档**美术（作为 part 模型素材，注册时按 AE2 part 渲染处理）。
- **频道**：缺频道仅 3–10 AE/t 基础供电；有频道才按配置速率抽能。
- **风险封顶**：充能棒对网络/对物品的吞吐要受"最大输入/输出"与爆发间隔约束，避免把网格抽停机（设计文档 §4）。
- **`refs/` 只读约束**：`refs/` 是本地只读源码参考，**AI 不修改、不写入**，仅作核实依据；已在 `.gitignore` 忽略、不入仓。

---

## 2. 工程现状盘点（已核实事实，非臆测）

### 2.1 `refs/` 真实内容（本地，只读）

| 路径 | 内容 | 用途 |
|------|------|------|
| `refs/1.20.1/Applied-Energistics-2-forge-1.20.1/` | AE2 全源码（`appeng.api`、`appeng.parts`、`EnergyCellBlock.java` 等） | 取能 API、part 范式、能源元件构造 |
| `refs/1.20.1/Powah-1.20.1/` | Powah 全源码（多 loader）；`block/energizing`（EnergizingOrb/Rod）；`energizing_rod_*` 七档贴图；分级命名（Tier） | 充能机制范本 = `block/energizing`（充能台↔充能棒）；贴图与分级 |
| `refs/1.20.1/ExtendedAE-appflux-1.20.1-forge/` | Applied Flux 完整源码（包 `com.glodblock.github.appflux`） | 读网络 FE 的 API、`Induction Card` 爆发机制 |
| `refs/1.20.1/configs/` | `ae2/`、`appflux-common.toml`、`powah.json5` | 开发环境运行配置 |
| `refs/1.21.1/` | AE2 / Powah / AppliedGenerators / ExtendedAE-appflux(neoforge) | 后续版本参考 |

> 设计文档 v4 第 6/9 节称 Applied Flux "仅 gradle 骨架、FE API 待联网"——**已过时**：本地 `refs/1.20.1/ExtendedAE-appflux-1.20.1-forge/` 是**完整源码**，FE 读取（§3.5）与 Induction Card 爆发（§3.7）均已本地可查。

### 2.2 已本地核实的关键 API（直接支撑实现）

- **取网格 AE**：`appeng/api/networking/energy/IEnergyService.extractAEPower(amt, Actionable.MODULATE, PowerMultiplier.ONE)`。
- **读网络 FE（Applied Flux，本地源码）**：`IStorageService` + `FluxKey.of(EnergyType.FE)` 作为 AEKey 抽取（范式见 `common/caps/NetworkFEPower.java`、`ItemPortableFECell.java`）。FE 与 AE 功率缓冲天然隔离。
- **能源元件构造**：`EnergyCellBlock(maxPower, chargeRate, priority)`；方块实体 `EnergyCellBlockEntity` 可复用；致密配方在 `CraftingRecipes.java:333`（8 环 + 1 心）。
- **part 范式**：`appeng/parts/AEBasePart.java` 基类；`PartTerminal`/`PartExportBus` 挂 cable bus；`PartPlacement.java`、`AEWrench.java` 管放置/扳手拆除。充能棒即此范式的 part 子类。
- **充能机制（Powah 范本）**：`EnergizingOrbTile`（充能台）无 FE 输入口、能量只经 `fillEnergy` 注入，填满 `recipe.getEnergy()` 即产出；`EnergizingRodTile`（充能棒，`extends AbstractEnergyStorage`）是 FE 储能方块，其 `postTick` 在附近有含有效配方的充能台时把 `min(存储, transfer)` 经 `orb.fillEnergy` 推入（详见 §3.7）。
- **Induction Card 爆发机制（Applied Flux 范本）**：`ItemInductionCard` 装到便携 FE 元件/接口上，使装备"间隔一段时间给一次巨量的电"（tooltip 实证）。即**周期爆发**而非平滑逐 tick。

### 2.3 仍待确认（需你拍板，见 §6）

1. ME 充能棒（基础）中心材料（with-Powah 版；AE 棒基础中心 = Powah `energizing_rod_basic` 已定）。
2. mod id / 包名（暂定 `appliedpowah` / `com.lufh.appliedpowah`）。
3. 是否授权我开始 Phase 0 脚手架（联网后执行）。

---

## 3. 工程架构

### 3.1 技术栈与 loader 决策

- **Minecraft 1.20.1 + Forge**，Java 17。
- **构建工具**：保留 Architectury Loom，但**只配 Forge 一个 loader 目标**（不建 neoforge 模块）。理由：单 Forge 已覆盖绝大部分需求；Architectury 仍提供 `common`/`forge` 的共享/平台拆分，使日后加 NeoForge 成本最低。
- **若日后确需 NeoForge（如 ATM10），边际工作量评估**：
  - 代码几乎零重写：能源元件、充能棒的功能逻辑放在 `common`，只依赖跨 loader 的 AE2/Powah/AppliedFlux API（这些 mod 在 1.20.1 都同时发 Forge 与 NeoForge 构建，API 一致）。
  - 新增：`neoforge/` 模块 + NeoForge 版 `mods.toml` + 把依赖坐标换成 `-neoforge` 版 + 一轮 NeoForge 构建/运行测试。
  - 估计：**低~中**（构建管线 + 一轮联调，约 0.5–1 天），不是架构重做。
  - 结论：现在先 Forge-only，把 `neoforge/` 留空/暂不建；需要时再补模块即可。

### 3.2 模块划分（Architectury：common + forge，仅 Forge 目标）

```
common/     ← 与 loader 无关的共享源码（功能主体）
forge/      ← Forge loader 入口（@Mod、mods.toml、平台实现）
```
（`neoforge/` 暂不建；如 §3.1 评估，日后一键补。）

### 3.3 依赖与条件加载矩阵

| 前置存在 | 可用模块 | 实现要点 |
|----------|----------|----------|
| 仅 AE2 | 高密度能源元件（超密+极密） | 不崩，仅此；充能棒配方全禁用（见 §3.8(b)） |
| AE2 + Powah | + AE 充能棒（7 档；配方随 Powah 存在才注册） | AE 棒从 AE 网格取能，不需 Flux |
| AE2 + Powah + Applied Flux | + ME 充能棒（7 档，抽 FE） | 三者皆在才注册 ME 棒配方 |
| AE2 + Applied Flux（无 Powah） | 仅能源元件 | 无充能台→充能棒无意义→棒配方全禁用 |

所有 Powah / Applied Flux 注册在 `integration/` 包内做运行时探测，缺省跳过。

### 3.4 功能包划分（对应 Phase）

```
com.lufh.appliedpowah
├── energycell/       Phase 1：超密/极密能源元件（Block/BlockEntity/Item）
├── chargingrod/      Phase 2–3：AE/ME Energizing Rod（AE2 part，挂 cable bus，喂充能台；配方受 Powah/Flux 门控）
├── definitions/      方块/物品注册（镜像 AEBlocks/AEBlockEntities）
├── integration/
│   ├── ae2/          AE2 API（取 AE、part 基类扩展）
│   ├── powah/        Powah 分级/贴图/配方探测
│   └── appflux/      Applied Flux FE 读取（FluxKey/EnergyType）
├── config/           每档"内部缓存/最大输入/输出"、爆发间隔 x 等参数
└── api/              对外/内部抽象
```

### 3.5 关键实现路径（源码已核实，非空想）

- **高密度元件**：`new EnergyCellBlock(12_800_000, 12_800, 1600)`（超密）、`102_400_000`（极密）+ 复用 `EnergyCellBlockEntity` + 注册 `BlockEntityType`；配方镜像 `CraftingRecipes.java:333`（8 环 + 1 心）。零改 AE2 源码。
- **充能棒（AE2 part）**：实现为 `AEBasePart` 子类，挂在 cable bus（接入 AE 网络），支持四种 AE 线缆（glass/covered/smart/dense）及其颜色变体。每档有自身 FE 缓冲；从 AE2/Applied Flux 网络爆发式取 FE 填入缓冲（Induction Card 机制），并在附近有 Powah 充能台时把存储经 `EnergizingOrb.fillEnergy` 推入（见 §3.7）。
- **取 AE**：`grid.getService(IEnergyService.class).extractAEPower(..., PowerMultiplier.ONE)`。
- **取 FE**：`grid.getService(IStorageService.class)` + `FluxKey.of(EnergyType.FE)` 抽取。

### 3.6 风险与缓解（摘要，细节见设计文档 §4）

- 面向 1（抽 AE 抽干网络）：高密度元件抬高缓冲 + 抽量/吞吐封顶。
- 面向 2（FE 大吞吐耗 AE）：缓冲式取能 + 封顶（建议 ≤25% 网络容量）+ 爆发间隔约束。

### 3.7 充能棒"充能"机制（与充能台 Energizing Orb 配合使用）

**Powah 真实机制（源码核实，`owmii.powah.block.energizing`）**：
- `EnergizingOrbTile`（充能台）`extends AbstractTickableTile`，**本身不是普通 FE 输入设备**：只有一个内部 `Energy buffer`（累计能量），能量只能经 `fillEnergy(amount)` 注入；当 buffer 填满（`recipe.getEnergy()` 总量），清输入并把配方产物放入 slot 0。即：**充能台没有电缆接口，只能被"充能棒"喂电**。
- `EnergizingRodTile`（充能棒）`extends AbstractEnergyStorage`：**是一个 FE 储能方块**。其 `postTick()` 在①自身存有能量、②附近（`Powah.config().general.energizing_range`，默认 **4** 的立方范围）有③含有效 `energizing` 配方的充能台 时，每约 **20 tick**（coolDown）把 `fill = min(本棒存储量, 该档 transfer)` 的能量通过 `orb.fillEnergy(fill)` 推入充能台缓冲，并从自身扣除。
- 绑定方式：棒 `onPlace` 时扫描范围自动找 orb 并 `setOrbPos`；也可用 Powah 扳手在范围内手动 link。

**我们 Applied Powah 充能棒的设计落点**（对齐 Powah，并体现"取能"特色）：
- 形态：**AE2 part**（挂 cable bus，接入 AE 网络），区别于 Powah 原版"独立方块储能块"。每档容量/transfer 见 §3.9。
- **充电（入能）**：通过本 mod 的"取能"机制从 **AE2 / Applied Flux FE 网络**抽取 FE 填入 rod 自身缓冲（Induction Card 式爆发抽取，见下）。这是它区别于 Powah 原版棒的"Applied Powah"特征。
- **放电（喂充能台）**：当 Powah 存在（`integration/powah` 运行时探测）且 rod 附近有充能台时，rod 自动链接并把存储的 FE 经 `EnergizingOrb.fillEnergy` 推入充能台缓冲——完全镜像 `EnergizingRodTile.postTick`。**这一步是"和充能台配合使用"的落地，也是"能充能"的必须项。**
- 缺 Powah 时：rod 仍可正常存取能 / 从 AE 网络取能（不崩），只是没有"喂充能台"这一交互；日后可扩展为喂其它兼容 FE 机器的缓冲。

**爆发式取能（Induction Card 机制，用于"从网络抽能填充 rod 缓冲"）**：
- 每 x tick 一次性抽取一笔（金额=该档内部缓存，或按配置的爆发量）填入自身缓冲；再由"喂充能台"逻辑定期释放。非平滑逐 tick。
- 落地：rod 的 tick 里按 `config.burstIntervalTicks` 触发 `extractAEPower` / `IStorageService.extract(FluxKey.FE)` 的爆发抽取。
- 受 §3.6 "≤25% 网络容量"封顶与频道约束保护，避免网络停机。

### 3.8 配方

**全称**：AE 充能棒 = `AE Energizing Rod`；ME 充能棒 = `ME Energizing Rod`。各 7 档（初级…下界）。

**(a) 有 Powah 时 —— AE Energizing Rod（基础）显式配方（用户提供）**：
```
能源元件  空      能源元件
空      Powah:energizing_rod_basic   空      ← 中心 = Powah 的 Energizing Rod（基础），是"方块"
能源元件  空      能源元件
```
> 注：Powah `energizing_rod_basic` 经核实是**方块**（`data/powah/recipes/crafting/energizing_rod_basic.json` 结果项）；作配方中心材料合法。ME 棒（基础）的 with-Powah 中心材料待你定（§2.3-1）。

**(b) 配方门控**：充能棒唯一用途是喂 Powah 充能台，故**无 Powah 时棒毫无意义，直接禁用 AE/ME 棒全部合成配方**；**无 Applied Flux 时仅禁用 ME 棒配方**（ME 棒需从 FE 网络取能），AE 棒仍可合成（AE 棒从 AE 网格取能，不需 Flux）。即：
- Powah 在 → 注册 AE 棒（全 7 档）配方；
- Powah + Flux 都在 → 额外注册 ME 棒（全 7 档）配方；
- Powah 不在 → 不注册任何棒配方，仅剩超密/极密能源元件。

**(c) 升级链（沿用用户早期设想）**：基础→各级 一级一级升（仿 Powah 分级）；AE 棒 ↔ ME 棒 互转走存储台/样板台（无合成）；贴图用 Powah 充能棒 `energizing_rod_*` 七档（见 §1.3）。

### 3.9 充能棒 7 档参数表（取自 Powah 源码+config，可改）

> 数值来源：Powah `config/v2/DefaultEnergies.java` —— `energizingTransfer() = baseScaling()×100`、`energizingCapacity() = energizingTransfer()×100`；`baseScaling() = (1,4,10,40,100,400,2000)` 对应 [starter,basic,hardened,blazing,niotic,spirited,nitro]（Forge 平台 ×100/×100）。即**真实默认值如下**，均可经本 mod 的 `EnergyConfig`（镜像 Powah，按 Tier 分级）在配置文件调整。

| 充能棒等级 | 对应 Powah Tier | 内部缓存(容量) | 最大输入/输出(transfer) |
|------------|-----------------|---------------|------------------------|
| 初级 | starter | 10 kFE | 100 FE/t |
| 基础 | basic | 40 kFE | 400 FE/t |
| 硬化 | hardened | 100 kFE | 1 kFE/t |
| 烈焰 | blazing | 400 kFE | 4 kFE/t |
| 钻石 | niotic | 1 MFE | 10 kFE/t |
| 富生 | spirited | 4 MFE | 40 kFE/t |
| 下界 | nitro | 20 MFE | 200 kFE/t |

- **内部缓存** = 充能棒自身 FE 缓冲容量；**最大输入/输出** = ①从 AE/FE 网络取能（输入）的每 tick 速率上限，也是②喂充能台时每次 `fillEnergy` 的推能上限（= Powah `EnergizingRodTile.postTick` 里的 `getTransfer(variant)`，见 §3.7）。
- **每 x tick 抽取量暂定 = 对应的内部缓存**（爆发式，Induction Card 机制，见 §3.7），x 为该档爆发间隔（`config.burstIntervalTicks`）。
- 命名/映射（已按你指定订正）：初级=starter、基础=basic、硬化=hardened、烈焰=blazing、钻石=niotic、富生=spirited、下界=nitro。
- 这些值**均可配置**：本 mod 用与 Powah 同构的 `EnergyConfig`（TieredEnergyValues），玩家/你可在 `appliedpowah` 配置文件里改容量与 transfer，不必硬编码。

### 3.10 现有高密度能源元件（已定，保留）

在原 AE2 两档（能源元件 200k / 致密 1.6M）之上，新增 **两档**，面向**不想研究 Powah 的玩家**也照常可用：
- **超密能源元件**：`12.8 M` AE（`8 × 致密`）
- **极密能源元件**：`102.4 M` AE（`8 × 超密`）——与 AE2 自身"×8"惯例一致。

### 3.11 高密度能源元件未来路线（TODO，当前不实现）

面向**想研究 Powah 的玩家**，计划未来扩展：以 Powah `能量单元 (Energy Cell)` 经某种合成变为 AP 新版能源组件，形成一系列元件，并支持 **kubejs 自定义**。要点：
- 与 §3.10 的超密/极密**并存，不互斥**（一条给不研究 Powah 的玩家，一条给研究 Powah 的玩家）。
- 当前版本**不实现**；预留 TODO，待你后续拍板具体合成方式与 kubejs 接入点。

---

## 4. 我的职责边界（你到底应该干啥）

### 4.1 现在（离线）就能做 ✅
- 架构梳理、本文档、文件夹骨架、构建脚本草稿。
- 基于本地 `refs/` 源码核实 API 与"充能"机制（已完成 §2.2，含 Applied Flux FE 读取与 Induction Card 爆发）。
- 列待确认项与待联网项清单（§6）。

### 4.2 联网才能做（GitHub 可裸连；其余按"失败 3 次就等"）
- **GitHub 可直接连接**：可 `git init` + 关联 `https://github.com/LuFH5746/Applied-Powah.git` + 首次提交/推送（本 workspace 当前非 git 仓库）。
- **Maven / Modrinth / CurseForge 等**：可能仍需科学上网；若某地址**连续测试 ≥3 次**出现 404 / 超时 / 429，则**停止硬试、暂等**，不再钻牛角尖。
- Gradle 依赖解析（`ae2`/`powah`/`appflux` 坐标）、`gradle wrapper` 生成、`build` / `runClient` 调通，均受上条约束。

### 4.3 需要你拍板 ⚠️
- ME 充能棒（基础）中心材料（with-Powah 版；无 Powah 时棒配方已全禁用，见 §3.8(b)）。
- mod id / 包名（§2.3-2）。
- 是否授权 Phase 0 脚手架（§2.3-3）。

---

## 5. 初步文件夹组织（已落盘，适配单 Forge）

```
Applied-Powah/                         ← 仓库根 = 本工作区
├── docs/
│   ├── ARCHITECTURE.md                ← 本文
│   ├── AppliedPowah_设计_取能机制与高密度能源元件.md   ← 设计文档（暂留根目录，建议移入 docs/）
│   └── 纠正.md
├── common/                            ← 共享模块（功能主体；跨 loader）
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/lufh/appliedpowah/
│       │   ├── api/  energycell/  chargingrod/  definitions/
│       │   ├── integration/{ae2,powah,appflux}/  config/
│       └── resources/
│           ├── assets/appliedpowah/{blockstates,models/block,models/item,lang,textures/item}
│           └── data/appliedpowah/{recipes,loot_tables,tags/{items,blocks}}
├── forge/                             ← Forge loader 入口（仅此一个 loader 目标）
│   ├── build.gradle
│   └── src/main/{java/com/lufh/appliedpowah/forge/, resources/META-INF/mods.toml}
├── gradle/wrapper/                    ← gradle-wrapper.properties（jar 联网后由 `gradle wrapper` 生成）
├── build.gradle / settings.gradle / gradle.properties   ← DRAFT，版本占位
├── .gitignore                         ← 忽略 build/ .gradle/ run/ 以及 refs/（本地参考，不入仓）
├── gradlew / gradlew.bat              ← 联网后由 `gradle wrapper` 生成
└── refs/                              ← ⚠️ 本地 ONLY（依赖 mod 源码参考，只读、gitignore、不入仓）
    ├── 1.20.1/  (AE2 / Powah / Applied Flux / configs)
    └── 1.21.1/
```
> 注：`neoforge/` 暂不建（见 §3.1）；若日后需要，按 §3.1 评估补一个模块即可。

---

## 6. 待确认 / 下一步清单

| # | 事项 | 谁 | 状态 |
|---|------|----|------|
| 1 | ME 充能棒（基础）中心材料（with-Powah 版） | 你确认 | 🟡 |
| 2 | mod id / 包名（`appliedpowah` / `com.lufh.appliedpowah`） | 你确认 | 🟡 |
| 3 | Phase 0 脚手架授权 | 你授权 | 🟡 |
| 4 | Applied Flux FE 读取 API | 已本地解决（§2.2） | ✅ |
| 5 | 设计文档 v4 "Applied Flux 仅骨架" 误判更正 | 你确认后我改 | 🟡 建议 |
| 6 | git 仓库初始化 + 关联 `LuFH5746/Applied-Powah` | GitHub 可裸连，待凭据 | 🟢 待你点头 |
| 7 | 单 Forge 决策确认（NeoForge 留作日后可选） | 你确认 | 🟢 建议 |
| 8 | 充能棒 = AE2 part 形态（挂 cable bus，支持四种 AE 线缆+颜色变体） | 已确定（你指定） | ✅ |
| 9 | 高密度元件未来路线（Powah Energy Cell 合成 → AP 组件 + kubejs 自定义） | 当前 TODO，待拍板合成方式 | 🟡 |

---

*本架构全部结论基于本地 `refs/` 1.20.1 源码核实（`refs/` 只读、不入仓）；构建相关版本号均为 DRAFT 占位，联网前不可运行 `gradle`。"充能"机制（充能棒 ↔ 充能台 Energizing Orb）与 Induction Card 爆发均来自 Powah / Applied Flux 本地源码，非臆测。充能棒定为 AE2 part 形态（挂 cable bus，支持四种 AE 线缆及颜色变体），从 AE/ME 网络取能、喂附近充能台。*
