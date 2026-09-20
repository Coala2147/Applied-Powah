# 计划：高级充能台 / ME 充能台 / 指导与 JEI 文案

> 状态：**规划稿（不实现代码）**  
> 对应仓库：`https://github.com/Coala2147/Applied-Powah.git`  
> 下一轮在**新对话**中按本文件执行；优先级：能运行 > 文案正确 > 美观。

---

## 0. Git 协作约定（已拍板）

| 项 | 规则 |
|----|------|
| 提交标题 | **短**：`feat: 高级充能台`、`fix: 元件 tooltip`、`docs: 指南文案`；**不要**长英文小作文 |
| 分支 | 改动/新功能 → 拉分支（如 `feat/ex-orb`、`fix/tooltip-style`）→ 测试通过 → **merge 回 master** |
| 版本推进 | 合并后 `mod_version` 递增，产物进 `releases/`，再 **短标题 commit** |
| 远程 | 仅 `origin = https://github.com/Coala2147/Applied-Powah.git` |
| 不入库 | `refs/`、`logs/`、`libs/`、`.workbuddy/` |

**建议分支切分（下一轮）**

1. `fix/tooltip-ae2-style` — 文案与 JEI catalyst（小改动，可先合）  
2. `feat/me-energizing-orb` — ME 充能台  
3. `feat/advanced-energizing-orb` — 高级充能台 + 棒槽/并行  
4. `docs/guide-native-style` — 指南/物品介绍对齐 AE2/Powah  

---

## 1. 本轮反馈与目标

### 1.1 已澄清的问题

| 反馈 | 正确理解 |
|------|----------|
| JEI「充能」页 | Powah JEI 的 **Energizing** 类目：左侧 catalyst 栏是 **充能台 + 各档充能棒**（见你图6）。AP 棒/新方块也要 **`addRecipeCatalyst` 到同一类目**，不是把 AP 棒做成 orb 配方产物 |
| 元件 tooltip | AE2 原生：`已存储能源: 0/200k AE (0%)` + **`Hold [G] to open guide`**（键位 `key.ae2.guide`，文案 `gui.tooltips.ae2.OpenGuideDetail`）。我们现在只有 `0/102.4M AE`，差格式与 G 提示 |
| AP ROD 无 guide | 元件/棒都缺 AE2 式指导入口与文案 |
| 文案不像原生 | 全面对齐 AE2 `Tooltips` / Powah 本地化风格 |

### 1.2 调研结论（refs，事实）

| 主题 | 源码要点 | 对计划的含义 |
|------|----------|--------------|
| JEI catalyst | `PowahJEIPlugin.java:35-37`：`addRecipeCatalyst(ORBS, TYPE)` + `ENERGIZING_ROD.getAll()`；类目 `RecipeType.create("powah","energizing",…)` | AP 侧 JEI：用**同一 RecipeType** 把 AP 棒、ME 台、高级台注册为 catalyst |
| AE2 能量 tooltip | `Tooltips.energyStorageComponent`：`StoredEnergy + ": " + number + " " + AE + " (" + percent + ")"` | AP 元件/棒 tooltip **复制该句式**（可自实现，不必依赖 AE2 Tooltips 类） |
| AE2 GuideME | 键位「Open Guide for Items」；物品上「Hold [G]…」。完整接入需 **guideme 页**挂进 AE2 手册树 | **阶段化**：先做**本 mod 物品 tooltip 第二行**「按 G 打开指南 / 或使用 Applied Powah 指南」；真正 G 跳转本 mod 页 = 后续（见 §5） |
| ME 弹出产物 | `InterfaceBlockEntity` + `InterfaceLogic`（配置/推送 ME 存储） | ME/高级充能台「弹出到 ME」抄 **InterfaceLogic 思路**：开关 + 将输出槽 item insert 进 `IStorageService` |
| ExtendedAE 翻页 | `IPage` + `CUpdatePage` 包 + `TileExInterface implements IPage`；`MenuOpener.open(Container…)`；`GuiExInterface` + `/screens/*.json`；`InterfaceLogic(…, 36)` 扩展槽 | 高级台 **4 并行页**：BE 实现 `IPage`（或 AP 自有 `IPage`），客户端按钮发包改 page；**每页一套 6+1 槽** |
| 进度条 | AE2 压印机 GUI 使用 progress sprite；ExtendedAE 扩展压印器同款（你图5） | 新机器菜单画 **progress 矩形**，用 `progress = 0..1` 的填充宽；贴图可先用 AE2 压印机 progress 或自绘灰条 |
| 能量模型 | 棒缓存/transfer 见 Powah config；AP 输入默认 10M AE / 20M FE | 高级台：**输出/缓存 = 槽内棒之和**；**输入上限 = 64 × 槽内最高档棒的缓存**（见 §3） |

---

## 2. 新方块总览

| 方块 | 注册名（暂定） | 网络 | 一句话 |
|------|----------------|------|--------|
| **ME 充能台** | `me_energizing_orb` | AE2 + 可选 AppFlux | 原版 Powah 充能台配方 + **ME 弹出开关**；可作高级台合成原料 |
| **高级充能台**（AE/ME） | `ae_advanced_energizing_orb` / `me_advanced_energizing_orb` **或** 单方块 + 由棒类型决定 | AE2（AE 版抽 AE；ME 版抽 FE） | 直连网络抽能；**4 槽×16 棒**决定功率/缓存；**最多 4 并行**充能页 |

> 建议实现：**一个高级台方块** `advanced_energizing_orb`，BE 根据**槽内棒是 AE 还是 ME**（四槽须同类）决定抽 AE 还是 FE；配方产物可先做成两种物品 id 以区分 tooltip，或一种方块 + 配方两种入口。**下一轮开工前你拍板一种即可**；计划默认：**一种方块 + 槽内棒类型决定取能**（实现更简单）。

### 2.1 合成（暂定，可改）

| 方块 | 配方 |
|------|------|
| **ME 充能台** | 竖排：`a` 玻璃线缆（`ae2:glass_cable`） / `b` `powah:energizing_orb` / `c` AE2 **输入总线**（`ae2:import_bus`）→ 结果 `me_energizing_orb`。有向/无序待定：建议 **有序** `"a","b","c"` 一行或一列 |
| **高级充能台** | **无序**：1× ME 充能台 + 7× Powah **最高档**充能棒 `powah:energizing_rod_nitro` + 1× AE2 **能源接收器** `ae2:energy_acceptor` → 高级充能台 |

门控：无 AE2 → 全无；无 Powah → 无台；无 AppFlux → ME 台仍可合成但 **不能** 从网络抽 FE / 弹出 FE 相关能力可禁用（**建议**：无 Flux 时高级台若槽内是 ME 棒则不工作并 tooltip 说明）。

---

## 3. 高级充能台 — 玩法与数值（设计规格）

### 3.1 槽位结构

| 区域 | 数量 | 规则 |
|------|------|------|
| **棒槽（升级）** | 4 | 每槽最多 **16** 根；**仅 AP AE/ME 充能棒**；**四槽必须同一类**（全 AE 或全 ME）；**不要求同档** |
| **充能页（并行）** | 最多 **4** | 有棒才能开对应并行；**第 n 槽有棒 → 第 n 任务页可用**（你原文：1 号槽 → 第一任务） |
| **每任务页** | 输入 **6 槽** + 输出 **1 槽** | 与 Powah 充能台一致：输入最多各 1 个物品，可空；**输出槽只读**（不可玩家放入） |
| **面访问** | 7 槽均自动化 | 从**任意面**可访问 6 输入 + 1 输出（管道/总线） |

### 3.2 能量与充能规则

设槽 i 中 AP 棒档位为 \(t_i\)，数量 \(n_i\)（≤16），Powah 该档缓存 \(C(t)\)、输出 transfer \(T(t)\)（与现有 AP/Powah 表一致）。

| 量 | 公式 | 说明 |
|----|------|------|
| **缓存上限** | \(\sum_i n_i \times C(t_i)\) | 所有插入棒的缓存之和（**含**） |
| **输出上限（喂任务/对外）** | \(\sum_i n_i \times T(t_i)\) | transfer 之和（**含**） |
| **输入上限（从 AE/ME 抽）** | \(64 \times \max_i C(t_i)\) | **64 × 最高档插入棒的缓存**（不要求 n 满） |
| **输入节奏** | 沿用 AP 棒：`pullIntervalTicks` + 爆发量，但**受输入上限约束** | 禁止 FASTER 加速 |
| **AE 棒在槽内** | 网络侧抽 **AE**；内部可统一 FE 缓存（AE×2） | 网格仍保留 **5%**（config） |
| **ME 棒在槽内** | 网络侧抽 **Applied Flux FE** | 无 Flux 则不抽 |
| **充能执行** | 对**每个可用任务页**并行：若有合法 `EnergizingRecipe` 且缓存足够，按 **min(缓存余量, 页 transfer 分配)** 推进 | 简单策略：每 tick 将「输出上限」均分给**正在工作的页**，或每页独立消耗 min(配方剩余, 单页 transfer)；**实现选一种写进 config 注释** |
| **配方** | 与 Powah **完全相同** 的 `Recipes.ENERGIZING` / `EnergizingRecipe` | 不复制配方数据，直接读 Powah 注册表 |
| **产物** | 进入该页 **输出槽**；开关打开时 **推入 ME 存储**（Interface 风格） | 输出槽满则停 |

### 3.3 UI / 菜单（能运行优先）

| 元素 | 行为 |
|------|------|
| 右键 | 打开菜单（`MenuOpener` 或 Forge `NetworkHooks.openScreen`） |
| **翻页** | 左侧/角落按钮：**上一页/下一页**（ExtendedAE `IPage` 模式）；页码显示 `n/4` |
| **任务页** | 6 输入 + 1 输出 + **进度条** + 能源条（AE 或 FE 文本） |
| **设置页或角落按钮** | 「弹出产物到 ME」开关（类似图5 自动输出按钮） |
| **棒槽** | 可放同页侧栏或独立「升级」页；侧栏 4 槽更贴近图5「右侧四个槽」 |
| GUI 资源 | 占位：灰底矩形 + 系统字体；**不要求**精美贴图 |

网络包：`C2S_SetPage`、`C2S_ToggleAutoExport`（命名可仿 EPP `CUpdatePage`）。

### 3.4 ME 充能台（简版）

| 项 | 行为 |
|----|------|
| 配方执行 | 同 Powah 充能台（单任务，6+1 槽） |
| 能量 | 可**不**从 AE 网络抽（仍可像原版被棒喂）；**或**也可直连 AE 网络 — **你原文说「联网功能」主要指出产物**；默认：**不强制**从 AE 抽，仅 **自动弹出产物到 ME** |
| 弹出 | 开关打开 → 输出槽物品 `IStorageService.insert`（抄 ME 接口） |
| UI | 6+1 + 进度条 + 弹出开关；**无**棒槽、**无** 4 页 |

### 3.5 自动化与掉落

- 7 槽可从任意面访问：`IItemHandler` / AE2 `InternalInventory` 暴露 6+1（棒槽也可暴露，便于自动塞棒）。  
- 破坏：掉落方块物品；**棒槽内 AP 棒掉落且 FE 清空**（与现设计一致）；任务槽掉落物品。  
- 挖掘：与 AE2 方块类似（扳手/无加速）——**待定**是否要 AE2 wrench 兼容。

---

## 4. 指导 / 文案 / JEI（本轮也要做的「软」工作）

### 4.1 Tooltip 规范（对齐 AE2/Powah）

**能源元件（AE）**

```
超密能源元件
Hold [G] to open guide          ← 或中文「按住 [G] 打开指南」
已存储能源: 12.8M/12.8M AE (100%)
applied_powah:super_dense_energy_cell
```

- 能量行 **直接复用** AE2 句式：`已存储能源: {cur}/{max} AE ({pct}%)`（lang 键可自建 `applied_powah.tooltip.stored_energy`）。  
- 第二行：在 **未** 正式接入 GuideME 前写：`使用物品「Applied Powah 指南」查看说明`；接入后改成与 AE2 相同的 **Hold [G]…** 并真实跳转。

**AE/ME 充能棒**

```
AE 充能棒（下界）
按住 [G] 打开指南
缓存: 10M/10M AE
输出: 100k AE/t   （= Powah transfer /2，单位 AE）
取能: ME 网络 AE
applied_powah:ae_energizing_rod_nitro
```

- **禁止**物品 tooltip 写「当前 0/xx」除非该物品实例真有 NBT（棒放下后挖掉清空能量 → 物品侧显示 **满规格** 或 **0** 均可，但格式必须与 AE2 一致）。  
- 中英文 lang 都要；**键名统一** `applied_powah.tooltip.*`。

**JEI**

| 动作 | 细节 |
|------|------|
| Catalyst | `RecipeType.create("powah","energizing",…)` 注册：全部 AP 棒、ME 充能台、高级充能台 → **「充能」页左侧出现 AP 方块**（图6 效果） |
| 编译 | `libs/` 放 JEI forge jar（1.20.1），`compileOnly fg.deobf(files("libs/jei-…jar"))`；`@JeiPlugin` 类在 **无 JEI 时不会被加载** |
| 信息页 | 可选 `addIngredientInfo` 简短中英说明 |

### 4.2 指南内容（文案大纲 — 下一轮写入物品/文档）

1. 简介与前置（AE2 必须；Powah/AppFlux）  
2. 超密/极密：配方 8×上一级+运算处理器；挖掘保留 AE  
3. AE/ME 棒：放在线缆旁、取能、喂原版充能台  
4. **ME 充能台**：配方、弹出到 ME  
5. **高级充能台**：棒槽规则、4 并行、能量公式、自动弹出  
6. 配置项说明  
7. 已知限制（GuideME 集成阶段、UI 占位等）

**GuideME（长按 G）分两期**

| 期 | 内容 |
|----|------|
| **P1（本轮）** | 所有 AP 物品 tooltip 有指导提示；手持「Applied Powah 指南」可读完整文；lang 对齐 AE2 语气 |
| **P2（后续）** | 调研 AE2 `guideme` 如何 **注册附加书页/锚点**（`appeng.client.guidebook`、`PageAnchor`）；将 `docs/guidebook` 编成 guideme 资源，使 **G 对 AP 物品也能打开对应页** |

### 4.3 与旧指南物品的关系

- **保留**「Applied Powah 指南」物品（不依赖 AE2 GUI）。  
- P2 后 G 键与指南物品内容同源（同一套 markdown/lang）。

---

## 5. 技术实现地图（下一轮编码用）

### 5.1 建议包结构

```
com.coala.appliedpowah
├── AppliedPowah.java
├── chargingrod/          # 现有完整方块棒
├── energycell/           # 元件 + tooltip 对齐
├── orb/                  # 新：ME 台 + 高级台
│   ├── MeEnergizingOrbBlock/BE/Menu/Screen
│   ├── AdvancedEnergizingOrbBlock/BE/Menu/Screen
│   ├── RodUpgradeInventory   # 4×16 棒槽
│   └── EnergizingTaskPage    # 每页 6+1 + progress
├── network/              # C2S page / toggle export
├── guide/                # 指南物品 + 文案源
├── jei/                  # catalyst
├── config/
└── integration/{ae2,powah,appflux}/
```

### 5.2 关键参考文件（refs/1.20.1）

| 目标 | 参考 |
|------|------|
| 充能配方/台逻辑 | `Powah-1.20.1/.../block/energizing/EnergizingOrbTile.java`、`EnergizingRecipe`、`Recipes.ENERGIZING` |
| JEI 类目/catalyst | `Powah-1.20.1/.../compat/jei/PowahJEIPlugin.java`、`EnergizingCategory.java` |
| AE2 tooltip 句式 | `Applied-Energistics-2-forge-1.20.1/.../Tooltips.java:412`、lang `StoredEnergy` |
| ME 推送 | `appeng.blockentity.misc.InterfaceBlockEntity` + `appeng.helpers.InterfaceLogic` |
| 翻页 UI | `ExtendedAE-1.20.1-forge/.../TileExInterface.java`、`IPage.java`、`CUpdatePage.java`、`container/ContainerExInterface`、`client/gui/GuiExInterface` |
| 进度条 | AE2 压印机 Screen/菜单 progress 绘制（`appeng.client.gui…Inscriber*`） |
| 网络节点/取能 | 本仓库 `EnergizingRodBlockEntity`（保留 5%、固定 tick、FluxKey） |

### 5.3 构建与依赖

- 已有：`libs/` AE2、Powah、AppFlux（见 `libs/README.md`）。  
- **新增**：`libs/jei-1.20.1-forge-15.x.jar`（JEI 本体或 forge 变体，与 FG `fg.deobf(files(...))` 兼容）。  
- 版本示例：`0.2.0-alpha.1`（高级台第一测）或继续 `0.1.0-alpha.9`（仅 tooltip/JEI）。  
- **分支合并后再 bump 版本并短 commit**。

---

## 6. 分阶段交付（下一轮执行顺序）

### Phase A — 文案与 JEI（小，先合）

| # | 任务 | 验收 |
|---|------|------|
| A1 | 元件/棒 tooltip 改为 AE2 句式 + 指导行 | 游戏内与图1格式一致 |
| A2 | lang 中英补全 | 无 `block.applied_powah.*` 裸键 |
| A3 | JEI：catalyst 注册 AP 棒（及后续新台） | 「充能」页左侧出现 AP 棒（图6） |
| A4 | 指南物品文案更新（简版） | 右键可读新方块预告/现有说明 |
| A5 | 短 commit + merge | `fix: tooltip` / `feat: jei-catalyst` |

### Phase B — ME 充能台

| # | 任务 | 验收 |
|---|------|------|
| B1 | 方块/BE/菜单/占位 GUI | 可放置、右键打开 |
| B2 | 执行 Powah energizing 配方 | 与原版台相同产物 |
| B3 | 弹出 ME 开关 + insert | 开关开时产物进 ME |
| B4 | 合成配方（暂定竖排 a/b/c） | JEI 可见 |
| B5 | 进度条（可简陋） | 充能时可见变化 |
| B6 | 分支合并 | `feat: me-energizing-orb` |

### Phase C — 高级充能台

| # | 任务 | 验收 |
|---|------|------|
| C1 | 方块 + AE2 网络连接（facing 或六面） | 网络工具可见 |
| C2 | 4 棒槽（16/槽，同类，AP 棒） | 非法物品不可放 |
| C3 | 缓存/输出/输入按 §3.2 公式 | 日志/tooltip 可核对 |
| C4 | 最多 4 任务页 + 翻页包 | 图4/图5 式翻页 |
| C5 | 每页 6+1 + 任意面自动化 | 漏斗/总线可进出 |
| C6 | 弹出 ME + 进度条 + 能源显示 | 能完成 Powah 全配方 |
| C7 | 合成：ME 台 + 7 nitro 棒 + 能源接收器 | JEI 可见 |
| C8 | 掉落：棒清空 FE | 与现约定一致 |
| C9 | 分支合并 | `feat: advanced-energizing-orb` |

### Phase D — 指南原生化 / GuideME P2

| # | 任务 |
|---|------|
| D1 | 全物品介绍按 AE2/Powah 语气重写 |
| D2 | 调研 guideme 附加页；能则 G 键跳转；不能则文档写明限制 |
| D3 | `docs/guidebook` 与游戏内文案同步 |

---

## 7. 待你拍板（开工前）

| # | 问题 | 默认（若你不答） |
|---|------|------------------|
| 1 | 高级台：**一种方块**还是 **AE/ME 两种注册 id**？ | **一种方块**，槽内棒决定 AE/FE |
| 2 | ME 充能台是否也从 AE 网络抽能？ | **否**，仅弹出产物；仍可被原版棒喂 |
| 3 | 高级台连接线缆：仅 facing 面还是六面？ | **先 facing**（与现棒一致），测完可改 |
| 4 | 4 页并行的充能速率分配 | **均分**输出上限给正在工作的页 |
| 5 | ME 台合成是否一定要玻璃线缆+输入总线 | 按你暂定配方，JEI 可改 |
| 6 | 指南语言 | **中英** lang 都写 |
| 7 | 高级台是否要求四槽棒 **同档** | **否**（你写过不要求同档；只要求同类 AE/ME） |

---

## 8. 风险与边界

| 风险 | 缓解 |
|------|------|
| GUI 绘制/贴图 | 占位色块+文字；不阻塞功能 |
| JEI API 版本 | `libs/` 固定 jar；catalyst 失败不导致崩溃（try/catch + 日志） |
| 与 Powah 原版台并存 | 不改 Powah；新方块独立注册 id |
| 四并行 × 大缓存网络抽干 | 保留 5% + 输入上限公式；config 可调 |
| GuideME 深度集成 | P1 不承诺 G 跳转；P2 单独调研 |
| 会话过长 | **严格按本文件在新会话执行**；refs 只读 |

---

## 9. 新会话开工检查单

1. 读：本计划、`AGENTS.md`、`docs/纠正.md`、`docs/工程指导.md`  
2. Git：`feat/*` 分支，短 commit 标题  
3. Phase A → 测 → merge → Phase B → C  
4. 每阶段：`gradlew build` + `releases/AppliedPowah-<ver>.jar` + 短 commit  
5. UI 丑没关系；**配方/能量/弹出/翻页** 必须可玩  
6. 全部完成后：按 AE2 句式扫一遍 tooltip，再写 GuideME P2  

---

*本文件为完整计划方案，不含功能实现。执行时以 refs 源码为准，以本文件 §3 数值与 §6 阶段为准；与旧 ARCHITECTURE 冲突时以本文件 + `工程指导.md` 为准。*
