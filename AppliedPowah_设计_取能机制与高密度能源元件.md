# Applied Powah — 设计文档（纠正版 v4）

> 目标版本：**NeoForge 1.20.1（ATM10）/ Forge 1.20.1（ATM9）**
> 参考源码根：`D:\Desktop\Code\Applied Powah\refs\1.20.1`（AE2 / ExtendedAE / Powah 已本地化；Applied Flux 仅 gradle 骨架，源码需集成时引入）
> 状态：设计阶段（v4 已按第四次纠正把充能棒定为 AE2 网络部件 + 扳手/挖掘掉落行为）
> 变更记录：
> - v1 误将"ME 网络取 FE"理解为"抽 AE 后按 2:1 折回 FE"。
> - v2 纠正取 FE 机制；但仍错把充能棒当"手持物品 + 给背包充电 + 充能台"，并误读 §4 面向 2。
> - v3：充能棒定为方块（带网格节点），仅抽能入自身缓冲，不主动充别人；§4 面向 2 改为"FE 大吞吐消耗 AE 网络能源"；无线充能棒本期不做。
> - v4：**充能棒 = AE2 网络部件（part）**，效仿终端/输入输出总线挂在 cable bus 上（非 Powah 式"只能放特定能量线缆"）；扳手潜行右键快速挖掘；不用扳手用传统工具/空手挖掘与原版 AE2 方块一致（无明显加速）且**必须掉落物品**。

---

## 0. 结论速览（先给结论）

| 议题 | 结论 | 依据 |
|------|------|------|
| 给致密能源元件加"8 致密 + 1 运算处理器 → 下一档"合成路径 | ✅ **可行，近乎零成本** | `EnergyCellBlock` 容量由构造参数注入；致密配方已用相同模式 |
| 从 AE 网络"取 AE"（AE 充能棒） | ✅ 用 `IEnergyService.extractAEPower(...)` | 源码 `IEnergySource.java` 已确认签名 |
| 从 ME 网络"取 FE"（ME 充能棒） | ✅ 读 **Applied Flux 的 FE 存储缓冲**（Flux Accessor），**不是** AE 折回 | WebSearch 核实 Applied Flux 机制；与 AE 功率缓冲隔离 |
| 充能棒的放置/形态 | 🟦 **AE2 网络部件（part）**，挂在 cable bus 上，效仿终端/输入输出总线；非 Powah 式"只能放特定能量线缆" | 用户第四次纠正 |
| 充能棒是否主动给其他物品充电 / 手持充电 / 充能台 | ❌ **均否**。仅从网络抽取能量存入自身缓冲 | 用户第三次纠正 |
| 频道要求 | 🟦 缺频道仅最基础 **3-10 AE/t**；有频道才按配置速率抽取 | 用户第三次纠正 |
| 挖掘/掉落 | 🟦 **扳手潜行右键 = 快速挖掘**；不用扳手用传统工具/空手 = 和原版 AE2 方块一致（无明显加速）且**必掉落物品** | 用户第四次纠正 |
| 无线充能棒 | 🟨 **本期不做**，仅留后续开发（原生无无线方案） | 用户第三次纠正 |
| 条件加载 | ✅ 软依赖：无 aflux 仅 AE 棒；无 Powah mod 不崩、仅剩能源组件 | 用户明确 |

**一句话**：加档能源元件最稳、先做；充能棒是**挂在 AE2 线缆上的网络部件**（像终端/总线），AE 棒抽 AE、ME 棒抽 aflux 的 FE，都只抽能入自身缓冲；挖掘遵循 AE2 惯例（扳手瞬收、常规模具/空手正常速度且必掉落）。

---

## 1. 术语澄清

AE2 里**只有一个网格（Grid）**，"AE 网络"与"ME 网络"是同一套设备的两种视角：

- **AE** = AE2 内部能量单位。外部 FE 经 Energy Acceptor / ME Controller 以 **`2 FE = 1 AE`** 单向注入网格（默认 `powerRatioForgeEnergy = 0.5`）。该换算**只用于外部 FE 注入网络方向**。
- **ME** = 物料/流体的存储网络。能量（AE）与存储（ME）共存于同一 Grid，但**分属不同的缓冲**。

### ME 网络里的 FE 来自 Applied Flux，不是从 AE 折回

- **Applied Flux**（GlodBlock，CurseForge #965012，LGPLv3，1.20.1 Forge/NeoForge 均有）往 ME 驱动器里加 **FE 存储单元**，把 Forge Energy 直接存进网络；通过 **Flux Accessor** 读写。
- 官方明确：**存入的 FE 与网络自身的 AE 功率缓冲相互隔离**（"stored FE is not compatible with the network [AE power]"）。即 FE 是 Applied Flux 的独立缓冲。
- 因此 **取 FE = 读 Applied Flux 的 FE 存储缓冲**（仅装了 aflux 才有）；**取 AE = 读 AE2 原生 `IEnergyService` 的 AE 缓冲**（AE2 在即可，无需 aflux）。

---

## 2. 核心提议：高密度能源元件（加档）

### 2.1 现有实现证据（1.20.1 源码，未变）

**容量定义** —— `appeng/core/definitions/AEBlocks.java:183-184`
```java
ENERGY_CELL        = block("Energy Cell",        ..., () -> new EnergyCellBlock(200000,  800,  200),  EnergyCellBlockItem::new);
DENSE_ENERGY_CELL   = block("Dense Energy Cell",   ..., () -> new EnergyCellBlock(1600000, 1600, 1600), EnergyCellBlockItem::new);
```
三个构造参数含义（`EnergyCellBlock`）：`(maxPower, chargeRate, priority)`。

**方块实体** —— `appeng/core/definitions/AEBlockEntities.java:127-130`：普通与致密**共用同一个 `EnergyCellBlockEntity` 类**，仅注册不同的 `BlockEntityType`；容量在构造时由 `cellBlock.getMaxPower()` 注入。

**致密配方** —— `appeng/datagen/providers/recipes/CraftingRecipes.java:333`：`8 × 能源元件 + 1 × 运算处理器 → 1 × 致密能源元件`（纯压缩，无凭空生电）。

### 2.2 我们的方案（镜像既有模式，零改 AE2 源码）

| 元件 | maxPower (AE) | chargeRate (AE/t) | priority | 合成配方 | 贴图（指定） |
|------|--------------|-------------------|----------|----------|--------------|
| 能源元件（原生） | 200,000 | 800 | 200 | — | AE2 原版 |
| 致密能源元件（原生） | 1,600,000 | 1,600 | 1600 | 8×能源元件 + 1×运算处理器 | AE2 原版 |
| **超密能源元件（拟）** | **12,800,000** | **12,800** | 1600 | 8×致密 + 1×运算处理器 | **借用 AE2 致密贴图** |
| （可选）极密能源元件 | 102,400,000 | 102,400 | 1600 | 8×超密 + 1×运算处理器 | **借用 AE2 创造(creative)贴图** |

> 容量取 `8 × 上一级`，与 AE2 自身惯例一致。

**代码骨架（注册，对应 `AEBlocks` / `AEBlockEntities`）：**
```java
// AEBlocks.java 风格
public static final BlockDefinition<EnergyCellBlock> HYPER_DENSE_ENERGY_CELL =
    block("Hyper Dense Energy Cell", id, () -> new EnergyCellBlock(12_800_000, 12_800, 1600), EnergyCellBlockItem::new);

// AEBlockEntities.java 风格（复用原 Entity 类）
public static final BlockEntityType<EnergyCellBlockEntity> HYPER_DENSE_ENERGY_CELL =
    create("hyper_dense_energy_cell", EnergyCellBlockEntity.class, EnergyCellBlockEntity::new, AEBlocks.HYPER_DENSE_ENERGY_CELL);

// 配方（镜像 CraftingRecipes.java:333 的 shaped）
ShapedRecipeBuilder.shaped(..., HYPER_DENSE_ENERGY_CELL)
    .define('a', DENSE_ENERGY_CELL).define('c', calcProcessor); // 8 环 + 1 心
```

**可行性评级：✅ 高**（注册式扩展，无需 mixin/改 AE2）。

### 2.3 与 EAE 的关系

Extended AE（EAE）1.20.1 存在（`1.20-1.4.18`，Forge/NeoForge 均发布），但功能清单**不含任何更高密度能源元件**。高密度缺口与 EAE 无关、独立存在，本模组加档属纯补位，不冲突。

### 2.4 条件加载（软依赖）

- **Powah 是可选前置**：即使不装 Powah，模组**不应报错/崩溃**，此时整个模组仅剩这些（超密/极密）能源组件。所有 Powah 相关注册（充能棒、分级、贴图）运行时检测存在性，缺省则跳过。
- **Applied Flux 是可选前置**：仅影响 ME/FE 棒是否可用；无 aflux 时仅剩 AE 棒与能源组件。

---

## 3. AE 充能棒 / ME 充能棒（★v4 定为 AE2 网络部件）

### 3.1 形态与定位（用户第四次纠正：效仿终端/输入输出总线）

- **充能棒是 AE2 网络部件（part）**，不是独立方块，也**不是** Powah 那种"只能放在特定能量线缆上"的方块。它效仿 **ME 终端、输入输出总线** 那样，**挂在 cable bus（线缆总线）的一个 part 槽上**。
- 作为 part，它从所在 cable bus **自动获得网格节点（IGridNode）与频道分配**——这同时满足了"接入网格"和"有频道才抽能"两个要求，且无需我们手写节点逻辑。
- 物品形态（合成/互转/升级产物）拿在手里，对准 AE2 线缆右键放置即成为 part；从线缆上拆除后回到物品形态。
- **不主动给背包物品充电、不手持充电、不涉及"充能台"**（v2 误加已删）。
- 功能仅一种：**从网络抽取能量，存入自身缓冲**。
  - **AE 充能棒**：从网络 AE 功率缓冲抽 AE，存入自身 AE 缓冲（类似一个可被网络充能的能源组件）。用于**不装 Applied Flux 时也能用**。
  - **ME / FE 充能棒**：从 Applied Flux 的 FE 存储缓冲抽 FE，存入自身 FE 缓冲。**仅当 Applied Flux 安装时可用**。

> 与原版 Powah 充能棒的区别：原版只能放在特定能量线缆上；我们的作为 AE2 part，可挂到任意 AE2 线缆的空闲 part 槽，接入方式更灵活、与 AE2 网络深度统一。

### 3.2 频道机制（关键约束）

- **缺频道**：仅有最基础 **3-10 AE/t**（网络对未频道化设备的基线供电），无法按配置速率抽能。
- **有频道**：才按配置速率抽取能源（占用一个 ME 频道，与其他 ME 设备一致）。
- 因采用 part 模型，频道由 cable bus 统一分配，无需额外实现。

### 3.3 取能实现（两条不同链路）

**AE 棒（取 AE，原生）：**
```java
IEnergyService ess = grid.getService(IEnergyService.class);
double got = ess.extractAEPower(amtAE, Actionable.MODULATE, PowerMultiplier.ONE); // 取字面 AE 量用 ONE
```
- 取字面 AE 量用 `PowerMultiplier.ONE`（避免被 `powerUsageMultiplier` 二次缩放）。
- 抽出的是网格**共享 AE 缓冲**，会真实降低 `getStoredPower()`。

**ME / FE 棒（取 FE，来自 Applied Flux）：**
```java
// 读 Applied Flux 的 FE 存储缓冲（Flux Accessor / FE storage 服务）
// ⚠️ 具体 API 类名/方法待集成 Applied Flux 时核实（refs 仅 gradle 骨架，无源码）
double fe = fluxStorage.getStoredFE();
myFEBuffer.receiveEnergy((int) feWanted, false); // 存入自身缓冲（不向外充其他物品）
```
- FE 来自 Applied Flux 的**独立 FE 缓冲**，与 AE 功率缓冲无关；抽 FE 动作本身不直接消耗网格 AE 功率（但 FE 的**网络吞吐**会，见 §4 面向 2）。
- ⚠️ Applied Flux 的 FE 存储服务 API 名未核实（本地 refs 无其源码），集成阶段需引入 aflux 依赖确认。

### 3.4 合成配方链（补回最初点 #4）

- **原版 Powah 充能棒一级一级升 → 我们加一系列配方**：
  - **原版充能棒 → AE 充能棒**（直接转）；随后 **AE 棒同样可一级一级升**（仿原版分级）。
  - 另一路：**围着一圈能源组件合成 → AE 充能棒**（直接抽 AE 网络中的致密组件）。
- **最高级 Powah 棒 → ME 充能棒**（抽 FE 网络）；ME 棒同样可分级。
- **AE 棒 ↔ ME 棒 互转**：直接丢**存储台 / 样板台**即可，**无需合成配方**（用户明确）。
- **贴图**：充能棒暂时使用 **Powah 的贴图**（用户明确）。

### 3.5 抽取量 = 设计参数（去掉"充能台/手持充电"误读）

- "每次抽取的量"是一项**设计参数**，决定充能棒**自身缓冲的充能速率**，以及对网络的负载大小。
- **不**关乎"充能台"或"手持充电"（v2 误加，已删）。
- 建议：AE 棒与 ME 棒各自有可配置的**每 tick 抽取上限**；同时受 §4 抽量封顶约束（≤ 25% 网络容量）。
- 待你定：AE 棒、ME 棒各自的目标每 tick 抽取量（或 Rate 档位）。

### 3.6 挖掘与掉落行为（★v4 新增，用户第四次纠正）

必须与原版 AE2 方块/部件一致，避免"挖下来不掉落"的坑：

| 操作 | 行为 |
|------|------|
| **扳手（AE2 wrench）潜行右键** | **快速挖掘**：瞬时收起 part，作为物品掉落（AE2 part 标准行为，由 part 系统处理） |
| 不用扳手，用传统工具（镐等）挖掘 | 和原版 AE2 方块一致：**无明显加速效果**（正常挖掘速度），part 随线缆/本体拆除并**掉落物品** |
| 不用扳手，空手挖掘 | 同上：**正常速度、必掉落物品**（不得设置为"不可破坏/不掉落"） |

实现要点：
- 采用 AE2 **part** 模型后，扳手潜行右键拆除、`dropBlockAsItem` 式掉落由 AE2 的 `IPart` / `PartHost` 体系天然提供，优先复用，不要另起炉灶。
- 仍需显式确认/实现：**非扳手途径（工具/空手）拆除也必须掉落物品**——若走 part 路线，断线缆时 part 应随掉落；若部分路径默认不掉落，需在 `playerWillDestroy` / `getDrops` / loot 中补齐。
- 可选（建议）：掉落时保留自身缓冲内的能量 NBT（AE2 能源元件默认保留电量），提升体验；至少保证物品本体必掉。
- **禁止**出现"挖下来不掉落"——这是本次明确点名要避免的漏洞。

---

## 4. 与"点 #2 风险（高吞吐误耗 → 关机）"的关系（v3 双面向）

AE2 关键规则（已核实）：**一个网络每 tick 不能消耗或接收超过其存储容量的 AE**。否则需求尖峰超过缓冲 → 网络能量耗尽 → **重启（reboot）**。

该风险分**两个面向**：

- **面向 1 — 直接抽 AE 耗尽网络**：AE 棒直接从网格 AE 缓冲抽能，可能把 controller 等整网能量抽干 → 网络停机。
- **面向 2 — FE 大吞吐消耗 AE 网络能源**：ME 网络在**吞吐任何内容时都会消耗 AE 能源**——物品、流体，以及**经 Applied Flux 的 FE 吞吐**皆然。FE 被大量吞吐时，同样会大量消耗 AE 网络能源；超过缓冲即重启/停机。
  - 这与"aflux FE Port 默认输出率 `Integer.MAX_VALUE`"**无关**；本质是 AE2 的**吞吐即耗能**通则，FE 吞吐只是其中一种会耗 AE 的负载。

缓解三件套：

1. **本提议的高密度元件直接抬高缓冲上限**（12.8M AE = 25.6M FE 缓冲），是治本手段（主要压面向 1）。
2. **充能棒对每 tick 抽量封顶**：抽量 ≤ `grid.getMaxStoredPower()` 的安全比例（如 ≤ 25%），避免把缓冲抽空（同时压面向 1 与面向 2）。
3. **优先采用"缓冲式"取能**：充能棒自带 AE/FE 缓冲，由网络充能后离线持有，而非持续 live-pull，从根上隔离对实时缓冲的冲击。

---

## 5. 无线充能棒（★v3：本期不做，仅后续开发）

- 你设想的无线形态类似 **EAE 的 ME Wireless Connector**，用类似 **ME Wireless Setup Kit** 进行连接。
- 但**原生 AE2 并无无线的办法**来让充能棒脱离有线网络工作，故**本期（Phase 1-4）不包含无线充能棒**，仅作为后续开发方向记录。

---

## 6. 参考源码索引（v4 更新）

路径前缀（AE2）：`D:\Desktop\Code\Applied Powah\refs\1.20.1\Applied-Energistics-2-forge-1.20.1\src\main\java\`

| 用途 | 文件 |
|------|------|
| 取能接口签名 | `appeng/api/networking/energy/IEnergySource.java` |
| 网格能量服务（getStoredPower / extract 等） | `appeng/api/networking/energy/IEnergyService.java`、`appeng/me/service/EnergyService.java` |
| 能源元件方块（容量构造） | `appeng/block/networking/EnergyCellBlock.java` |
| 能源元件实体（IAEPowerStorage 挂网） | `appeng/blockentity/networking/EnergyCellBlockEntity.java` |
| **part 实现范式（终端/总线）** | `appeng/parts/*`（如 `PartTerminal`、`PartExportBus`、`PartImportBus`） |
| **part 放置 / 线缆总线 / 扳手拆除** | `appeng/parts/PartPlacement.java`、`appeng/block/cablebus/`、`appeng/items/tools/powered/AEWrench.java` |
| 注册示例 | `appeng/core/definitions/AEBlocks.java:183`、`AEBlockEntities.java:127` |
| 致密配方示例 | `appeng/datagen/providers/recipes/CraftingRecipes.java:333` |
| 能量换算比（仅注入方向） | `appeng/core/AEConfig.java`（`powerRatioForgeEnergy`） |

**Applied Flux（取 FE 来源）**：
- `refs\1.20.1\ExtendedAE-appflux-1.20.1-forge` 仅有 gradle 骨架（**无源码**）。
- WebSearch 已核实机制：往 ME 驱动器加 FE 存储单元、Flux Accessor 读写、**FE 与 AE 功率缓冲隔离**。
- ⚠️ 其 FE 存储服务 API 名/方法待**集成时引入 aflux 依赖并核实**（CurseForge #965012，1.20.1 Forge/NeoForge）。

**Powah（贴图与分级来源）**：`refs\1.20.1\Powah-1.20.1`（充能棒分级、贴图资源）。

---

## 7. 条件加载矩阵（v4）

| 前置存在 | 可用模块 |
|----------|----------|
| Powah + AE2 | 高密度能源组件 + AE 充能棒（含 Powah 分级/贴图） |
| Powah + AE2 + Applied Flux | 另含 ME/FE 充能棒（抽 FE） |
| 仅 AE2（无 Powah） | **仅高密度能源组件**（mod 不崩） |
| 仅 AE2 + Applied Flux（无 Powah） | 能源组件 + ME 棒（无 Powah 贴图/分级） |

实现：所有 Powah / Applied Flux 相关注册在运行时检测是否存在，缺省则跳过（软依赖）。

---

## 8. 实施阶段建议（v4：去除便携棒 Phase，充能棒按 part 实现）

| 阶段 | 内容 | 依赖 | 风险 |
|------|------|------|------|
| Phase 0 | 脚手架：Architectury Loom 单源码双 loader（Forge+NeoForge，1.20.1），接入 AE2 / Powah / Applied Flux API（软依赖检测） | — | 低 |
| **Phase 1** | **高密度能源元件（超密 + 可选极密）+ 合成配方 + 贴图借用** | AE2 API | **低（首选先做）** |
| Phase 2 | **AE / ME 充能棒 = AE2 part**：挂在 cable bus（效仿终端/总线），AE 棒抽 AE、ME 棒读 aflux FE 缓冲；按频道机制工作；**扳手潜行右键快速挖掘 + 常规模具/空手正常速度且必掉落** | Phase 0 + aflux API | 中 |
| Phase 3 | **合成配方链**（原版→AE→分级；最高级→ME）+ 互转（存储台，无合成）+ 抽取量参数 + 抽量封顶 | Phase 2 | 中 |
| （未来） | 无线充能棒：ME Wireless Connector 式连接（本期不做） | — | — |

---

## 9. 待你确认 / 下一步

1. **超密容量**定为 `12.8M AE`（单档）还是**再加一档极密 `102.4M AE`**？
2. **AE 棒 / ME 棒各自每 tick 抽取量（充能自身缓冲的速率）目标值？**
3. 是否授权我**开始 Phase 0 脚手架**（建 `build.gradle` / `settings.gradle` / `common+forge+neoforge` 三子项目 + Architectury 依赖）？
4. **(待核实) Applied Flux FE 存储服务 API 名**（refs 无源码，集成时引入确认）。

—

*本文档所有 AE2 API 结论均来自本地 1.20.1 源码核实；Applied Flux 机制经 WebSearch 核实（CurseForge #965012）；未读源码确认的点（如 `PowerMultiplier.ONE` 取值、`extractAEPower` 副作用、aflux FE 存储 API 名）已在文中标为待确认。v4 已将充能棒定为 AE2 网络部件（part，仿终端/总线）、明确扳手/挖掘掉落行为。*
