# Applied Powah — 工程架构与文件夹组织（v2，含充能机制核实）

> 起草/更新：2026-09-20（v1）→ 2026-09-20（v2：补充能机制、全称、配方、双档元件、单 Forge 决策、GitHub 可裸连）
> 依据：`AppliedPowah_设计_取能机制与高密度能源元件.md`（设计文档 v4）、`纠正.md`、本地 `refs/` 源码（Powah / AE2 / Applied Flux）
> 状态：**架构与文件夹组织起草阶段**，尚未建 git 仓库、未建可运行构建（构建需联网，见 §4）
> 关联设计文档：`../AppliedPowah_设计_取能机制与高密度能源元件.md`、`../纠正.md`

---

## 1. 工程目标（本工程到底要做什么）

**一句话**：做一个把 **AE2 × Powah** 能力缝合的 Minecraft mod（1.20.1，先只做 **Forge**），在不魔改 AE2 源码的前提下，补齐"高密度能源元件"与"AE/ME 充能棒"两块缺口；可选接 Applied Flux 让 ME 充能棒抽 FE。

### 1.1 两个核心功能

| 功能 | 形态 | 机制 | 阶段 |
|------|------|------|------|
| **高密度能源元件**（超密 / 极密，两个新增档） | 方块（镜像 AE2 能源元件） | 复用 `EnergyCellBlock(容量, 充率, 优先级)` + 复用 `EnergyCellBlockEntity`；8×上一级 + 1×运算处理器 合成 | Phase 1（首选先做，风险最低） |
| **AE 充能棒 / ME 充能棒**（= AE/ME Energizing Rod，7 个等级） | **双重形态**：①放到 AE2 线缆上 = AE2 网络部件（part）抽取能量入自身缓冲；②拿在手里 = 便携充能器，给背包物品充电 | AE 棒从网格 `IEnergyService` 抽 AE；ME 棒从 Applied Flux 的 FE 缓冲抽 FE；**手持时把缓冲能量推给背包里其他物品（"充能"）** | Phase 2–3 |

### 1.2 目标版本与 loader（v2 更新）

- **Minecraft 1.20.1，先只做 Forge**（覆盖 ATM9 与绝大多数启动器）。
- **Architectury 双 loader（Forge+NeoForge）判定为不必要**：单 Forge 已满足绝大多数需求。若日后确需 NeoForge（如 ATM10），边际成本见 §3.1。
- 1.21.1 的 `refs/` 已就绪，仅作后续版本参考，**本期只做 1.20.1**。

### 1.3 硬约束（来自用户纠正）

- **软依赖**：Powah、Applied Flux 缺任一个，mod 都**不能崩**；缺 Powah 仅剩高密度能源元件（+ 无 Powah 贴图/分级的棒），缺 Applied Flux 仅剩 AE 棒。
- **充能棒形态**：放到线缆 = AE2 part（仿终端/总线）；手持 = 便携充能器。**扳手潜行右键快速挖掘、常规模具/空手正常速度且必掉落物品**。
- **"充能"是必须项（v2 最重要更正）**：手持充能棒开启充能时，必须把自身缓冲的 FE **推给背包内其他物品**（见 §3.7），不是"只抽能入自身缓冲就完事"。
- **贴图**：能源元件借用 AE2 致密/创造贴图；充能棒暂时借用 **Powah `battery_*` 七档**贴图（已核实 `refs/.../textures/item/battery_*.png`）。
- **频道**：缺频道仅 3–10 AE/t 基础供电；有频道才按配置速率抽能。
- **风险封顶**：充能棒对网络/对物品的吞吐要受"最大输入/输出"与爆发间隔约束，避免把网格抽停机（设计文档 §4）。

---

## 2. 工程现状盘点（已核实事实，非臆测）

### 2.1 `refs/` 真实内容（本地）

| 路径 | 内容 | 用途 |
|------|------|------|
| `refs/1.20.1/Applied-Energistics-2-forge-1.20.1/` | AE2 全源码（`appeng.api`、`appeng.parts`、`EnergyCellBlock.java` 等） | 取能 API、part 范式、能源元件构造 |
| `refs/1.20.1/Powah-1.20.1/` | Powah 全源码（多 loader）；`battery_*` 贴图；`BatteryItem`/`ForgeEnvHandler` | **"充能"机制范本、贴图、分级命名 |
| `refs/1.20.1/ExtendedAE-appflux-1.20.1-forge/` | Applied Flux 完整源码（包 `com.glodblock.github.appflux`） | 读网络 FE 的 API、`Induction Card` 爆发机制 |
| `refs/1.20.1/configs/` | `ae2/`、`appflux-common.toml`、`powah.json5` | 开发环境运行配置 |
| `refs/1.21.1/` | AE2 / Powah / AppliedGenerators / ExtendedAE-appflux(neoforge) | 后续版本参考 |

> 设计文档 v4 第 6/9 节称 Applied Flux "仅 gradle 骨架、FE API 待联网"——**已过时**：本地 `refs/1.20.1/ExtendedAE-appflux-1.20.1-forge/` 是**完整源码**，FE 读取（§3.5）与 Induction Card 爆发（§3.7）均已本地可查。

### 2.2 已本地核实的关键 API（直接支撑实现）

- **取网格 AE**：`appeng/api/networking/energy/IEnergyService.extractAEPower(amt, Actionable.MODULATE, PowerMultiplier.ONE)`。
- **读网络 FE（Applied Flux，本地源码）**：`IStorageService` + `FluxKey.of(EnergyType.FE)` 作为 AEKey 抽取（范式见 `common/caps/NetworkFEPower.java`、`ItemPortableFECell.java`）。FE 与 AE 功率缓冲天然隔离。
- **能源元件构造**：`EnergyCellBlock(maxPower, chargeRate, priority)`；方块实体 `EnergyCellBlockEntity` 可复用；致密配方在 `CraftingRecipes.java:333`（8 环 + 1 心）。
- **part 范式**：`appeng/parts/AEBasePart.java` 基类；`PartTerminal`/`PartExportBus` 挂 cable bus；`PartPlacement.java`、`AEWrench.java` 管放置/扳手拆除。
- **"充能"机制（Powah 范本，v2 核心）**：`owmii.powah.item.BatteryItem.inventoryTick` → `owmii.powah.forge.ForgeEnvHandler.chargeItemsInPlayerInv`：遍历玩家背包物品，对每个 `IEnergyStorage` 目标调用 `receiveEnergy`，每槽上限 = 源的 `getMaxExtract()`（= 输出速率），总量受源自身存储约束；源随后 `consume(charged)`。即**电池是源，其它物品是接收方**。
- **Induction Card 爆发机制（Applied Flux 范本）**：`ItemInductionCard` 装到便携 FE 元件/接口上，使装备"间隔一段时间给一次巨量的电"并"充能玩家的库存"（tooltip 实证）。即**周期爆发**而非平滑逐 tick。

### 2.3 仍待确认（需你拍板，见 §6）

1. 充能棒 7 档的具体中文名↔Powah Tier 枚举映射（暂按 初级=starter … 下界=spirited 推断，见 §3.9）。
2. 无 Powah 时 ME 充能棒（基础）的中心材料用什么（§3.8 已给 cross/corners 备选，但 with-Powah 版的 ME 棒中心待定）。
3. mod id / 包名（暂定 `appliedpowah` / `com.lufh.appliedpowah`）。
4. 是否授权我开始 Phase 0 脚手架（联网后执行）。

---

## 3. 工程架构

### 3.1 技术栈与 loader 决策（v2）

- **Minecraft 1.20.1 + Forge**，Java 17。
- **构建工具**：保留 Architectury Loom，但**只配 Forge 一个 loader 目标**（不建 neoforge 模块）。理由：单 Forge 已覆盖绝大部分需求；Architectury 仍提供 `common`/`forge` 的共享/平台拆分，使日后加 NeoForge 成本最低。
- **若日后确需 NeoForge（如 ATM10），边际工作量评估（你问的"增加多少"）**：
  - 代码几乎零重写：能源元件、充能棒的功能逻辑放在 `common`，只依赖跨 loader 的 AE2/Powah/AppliedFlux API（这些 mod 在 1.20.1 都同时发 Forge 与 NeoForge 构建，API 一致）。
  - 新增：`neoforge/` 模块 + NeoForge 版 `mods.toml` + 把依赖坐标换成 `-neoforge` 版 + 一轮 NeoForge 构建/运行测试。
  - 估计：**低~中**（构建管线 + 一轮联调，约 0.5–1 天），不是架构重做。
  - 结论：现在先 Forge-only，把 `neoforge/` 留空/暂不建；需要时再补模块即可。

### 3.2 模块划分（Architectury：common + forge，仅 Forge 目标）

```
common/     ← 与 loader 无关的共享源码（功能主体）
forge/      ← Forge loader 入口（@Mod、mods.toml、平台实现；充电调 ForgeEnvHandler 同类逻辑）
```
（`neoforge/` 暂不建；如 §3.1 评估，日后一键补。）

### 3.3 依赖与条件加载矩阵

| 前置存在 | 可用模块 | 实现要点 |
|----------|----------|----------|
| 仅 AE2 | 高密度能源元件（超密+极密） | 不崩，仅此 |
| AE2 + Powah | + AE/ME 充能棒（Powah 分级/贴图/配方） | Powah 类运行时探测 |
| AE2 + Powah + Applied Flux | + ME 棒抽 FE | Applied Flux 类运行时探测 |
| AE2 + Applied Flux（无 Powah） | 能源元件 + ME 棒（无 Powah 贴图/分级，用 cross/corners 配方） | 见 §3.8 |

所有 Powah / Applied Flux 注册在 `integration/` 包内做运行时探测，缺省跳过。

### 3.4 功能包划分（对应 Phase）

```
com.lufh.appliedpowah
├── energycell/       Phase 1：超密/极密能源元件（Block/BlockEntity/Item）
├── chargingrod/      Phase 2–3：AE/ME Energizing Rod（part + 手持充能器）
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
- **充能棒（part + 手持）**：继承 `AEBasePart`，经 `PartPlacement` 挂 cable bus 获得 `IGridNode` 与频道（放置形态）；物品形态 `inventoryTick` 实现手持充能（见 §3.7）。扳手拆除走 AE2 part 体系；非扳手途径在 `getDrops`/loot 兜底必掉。
- **取 AE**：`grid.getService(IEnergyService.class).extractAEPower(..., PowerMultiplier.ONE)`。
- **取 FE**：`grid.getService(IStorageService.class)` + `FluxKey.of(EnergyType.FE)` 抽取。

### 3.6 风险与缓解（摘要，细节见设计文档 §4）

- 面向 1（抽 AE 抽干网络）：高密度元件抬高缓冲 + 抽量/吞吐封顶。
- 面向 2（FE 大吞吐耗 AE）：缓冲式取能 + 封顶（建议 ≤25% 网络容量）+ 爆发间隔约束。

### 3.7 充能棒"充能"机制（v2 最重要，基于 Powah 源码）

**含义**：充能棒手持且在背包、开启充能时，是**能量源**，把自身缓冲的 FE **推给背包里其他 `IEnergyStorage` 物品**（如电动工具、其它电池、AE2 便携元件等）。这正是用户强调的"充能棒一定要能充能"。

**实现范本（Powah `BatteryItem` + `ForgeEnvHandler.chargeItemsInPlayerInv`）**：
1. 充能棒 `Item` 实现 `inventoryTick`：若持有者为玩家且开启充能（shift 右键切换，仿 `BatteryItem.use`），遍历玩家背包物品。
2. 对每个 `IEnergyStorage` 目标调用 `receiveEnergy(maxPerSlot, false)`，`maxPerSlot = 本棒该档"最大输入/输出"`，总量 ≤ 本棒当前缓冲；本棒随后 `consume(已转出量)`。
3. 排除自身（或其它充能棒）避免互充，仿 `s -> !(s.getItem() instanceof OurRodItem)`。
4. 充能棒自身需实现 `IEnergyStorage`（`ForgeCapabilities.ENERGY`，仿 Powah `registerTransfer` 里对 `IEnergyContainingItem` 的 attach），使其既能从网络/其它源充入、又能向物品充出。

**爆发式取能（Induction Card 机制，用于"从网络抽能填充缓冲"那一段）**：
- 用户要求：x tick 值采用 Applied Flux **Induction Card** 式"间隔一段时间给一次巨量的电"。即充能棒**不平滑逐 tick 抽**，而是每 x tick 一次性抽取一笔（金额=该档内部缓存，或按配置的爆发量），填入自身缓冲；再由 §3.7 的平滑充能输出给物品。
- 落地：在 part 形态（挂在线缆时）的 `tick` 里，按 `config` 的 `burstIntervalTicks` 触发 `extractAEPower` / `IStorageService.extract(FluxKey.FE)` 的爆发抽取；手持形态不参与从网络抽（仅输出）。
- 注意：爆发抽取同样受 §3.6 的"≤25% 网络容量"封顶与频道约束保护，避免网络停机。

### 3.8 配方（v2 敲定方向）

**全称**：AE 充能棒 = `AE Energizing Rod`；ME 充能棒 = `ME Energizing Rod`。各 7 档（初级…下界）。

**(a) 有 Powah 时 —— AE Energizing Rod（基础）显式配方（用户提供）**：
```
能源元件  空      能源元件
空      Powah:energizing_rod_basic   空      ← 中心 = Powah 的 Energizing Rod（基础），是"方块"
能源元件  空      能源元件
```
> 注：Powah `energizing_rod_basic` 经核实是**方块**（`data/powah/recipes/crafting/energizing_rod_basic.json` 结果项）；作配方中心材料合法。ME 棒（基础）的 with-Powah 中心材料待你定（§2.3-2）。

**(b) 无 Powah 也能用（保证 mod 不崩、ME 棒也有配方）—— cross / corners 备选**：
- **四个初级能源元件上下左右（十字）** → **AE Energizing Rod（基础）**（不依赖 Powah）。
- **四个初级能源元件四角** → **ME Energizing Rod（基础）**（不依赖 Powah）。
> 这两条是 Powah 无关的保底配方；与 (a) 是否并存、以及更高级棒的升级链，待你确认后落到 `data/appliedpowah/recipes/`。

**(c) 升级链（沿用用户早期设想）**：基础→各级 一级一级升（仿 Powah 分级）；AE 棒 ↔ ME 棒 互转走存储台/样板台（无合成）；贴图暂用 Powah `battery_*` 七档。

### 3.9 充能棒 7 档参数表（用户提供，v2）

| 充能棒等级 | 内部缓存 | 最大输入/输出 | 推测对应 Powah Tier |
|------------|----------|--------------|---------------------|
| 初级 | 10 kFE | 10 FE/t | starter |
| 基础 | 100 kFE | 50 FE/t | basic |
| 硬化 | 250 kFE | 120 FE/t | hardened |
| 烈焰 | 800 kFE | 300 FE/t | blazing |
| 钻石 | 1.5 MFE | 700 FE/t | niotic |
| 富生 | 4 MFE | 1.2 kFE/t | nitro |
| 下界 | 10 MFE | 3 kFE/t | spirited |

- **内部缓存** = 充能棒自身缓冲容量；**最大输入/输出** = 从网络抽能（输入）与给物品充能（输出）的每 tick 速率上限。
- **每 x tick 抽取量暂定 = 对应的内部缓存**（爆发式，Induction Card 机制，见 §3.7），x 为该档爆发间隔。

### 3.10 我们额外增加的能源元件（v2 确认）

在原 AE2 两档（能源元件 200k / 致密 1.6M）之上，新增 **两档**：
- **超密能源元件**：`12.8 M` AE（`8 × 致密`）
- **极密能源元件**：`102.4 M` AE（`8 × 超密`）——与 AE2 自身"×8"惯例一致。

---

## 4. 我的职责边界（你到底应该干啥）

### 4.1 现在（离线）就能做 ✅
- 架构梳理、本文档、文件夹骨架、构建脚本草稿。
- 基于本地 `refs/` 源码核实 API 与"充能"机制（已完成 §2.2，含 Applied Flux FE 读取与 Induction Card 爆发）。
- 列待确认项与待联网项清单（§6）。

### 4.2 联网才能做（v2：GitHub 可裸连；其余按"失败 3 次就等"）
- **GitHub 可直接连接**：可 `git init` + 关联 `https://github.com/LuFH5746/Applied-Powah.git` + 首次提交/推送（本 workspace 当前非 git 仓库）。
- **Maven / Modrinth / CurseForge 等**：可能仍需科学上网；若某地址**连续测试 ≥3 次**出现 404 / 超时 / 429，则**停止硬试、暂等**，不再钻牛角尖。
- Gradle 依赖解析（`ae2`/`powah`/`appflux` 坐标）、`gradle wrapper` 生成、`build` / `runClient` 调通，均受上条约束。

### 4.3 需要你拍板 ⚠️
- 充能棒 7 档中文名↔Powah Tier 映射（§2.3-1 / §3.9）。
- 无 Powah 时 ME 棒（基础）中心材料；with-Powah 与 cross/corners 配方是否并存（§2.3-2 / §3.8）。
- mod id / 包名（§2.3-3）。
- 是否授权 Phase 0 脚手架（§2.3-4）。

---

## 5. 初步文件夹组织（已落盘，v2 适配单 Forge）

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
└── refs/                              ← ⚠️ 本地 ONLY（依赖 mod 源码参考），gitignore，不入仓
    ├── 1.20.1/  (AE2 / Powah / Applied Flux / configs)
    └── 1.21.1/
```
> 注：`neoforge/` 暂不建（见 §3.1）；若日后需要，按 §3.1 评估补一个模块即可。

---

## 6. 待确认 / 下一步清单

| # | 事项 | 谁 | 状态 |
|---|------|----|------|
| 1 | 充能棒 7 档中文名 ↔ Powah Tier 映射 | 你确认 | 🟡 暂按推断 |
| 2 | 无 Powah 时 ME 棒中心材料；with-Powah 与 cross/corners 是否并存 | 你确认 | 🟡 |
| 3 | mod id / 包名（`appliedpowah` / `com.lufh.appliedpowah`） | 你确认 | 🟡 |
| 4 | Phase 0 脚手架授权 | 你授权 | 🟡 |
| 5 | Applied Flux FE 读取 API | 已本地解决（§2.2） | ✅ |
| 6 | 设计文档 v4 "Applied Flux 仅骨架" 误判更正 | 你确认后我改 | 🟡 建议 |
| 7 | git 仓库初始化 + 关联 `LuFH5746/Applied-Powah` | GitHub 可裸连，可执行 | 🟢 待你点头 |
| 8 | 单 Forge 决策确认（NeoForge 留作日后可选） | 你确认 | 🟢 建议 |

---

*本架构全部结论基于本地 `refs/` 1.20.1 源码核实；构建相关版本号均为 DRAFT 占位，联网前不可运行 `gradle`。"充能"机制与 Induction Card 爆发均来自 Powah / Applied Flux 本地源码，非臆测。*
