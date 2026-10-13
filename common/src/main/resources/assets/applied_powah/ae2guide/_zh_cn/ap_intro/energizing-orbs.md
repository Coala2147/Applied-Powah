---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: 充能台
  position: 30
  icon: applied_powah:fluix_energizing_orb
categories:
  - Applied Powah
item_ids:
  - applied_powah:fluix_energizing_orb
  - applied_powah:advanced_energizing_orb
  - applied_powah:me_energizing_orb
---

# 充能台

Applied Powah 新增三个接入 ME 网络、运行 Powah **充能**配方（`powah:energizing`）的机器。
需要安装 **Powah**——未安装时三者及其配方都不存在，但模组本身照常加载。

<Row>
  <BlockImage id="applied_powah:fluix_energizing_orb" scale="4" />
  <BlockImage id="applied_powah:advanced_energizing_orb" scale="4" />
  <BlockImage id="applied_powah:me_energizing_orb" scale="4" />
</Row>

三者都只在**底面**连接 ME 网络（方块下方放置线缆），电网状态与存储能量通过 AE2 的
覆盖层集成（Jade / WTHIT / TOP）显示，并额外显示反应进度。

## 福鲁伊克斯充能台

最基础的一款：住进 ME 网络的 Powah 充能台。

| 属性 | 行为 |
|----------|----------|
| 配方页 | **1** 页（6 输入 + 1 输出） |
| 能量缓存 | **无**——不存储网络能量 |
| 网络抽取 | **从不**从 ME 抽 AE/FE |
| 充能棒供能 | **是**——Powah 棒与 AP AE/ME 棒为当前配方充能 |
| 产物输出 | 三态切换（见下） |
| 棒槽 | 无 |

必须在载入配方期间由充能棒（或任何 Powah 兼容供能装置）供能；无配方时不接受能量。

### 产物输出

GUI 左侧工具栏按钮，循环三种状态：

- **关**——产物留在本页输出槽（ME 合成 CPU 订单按设计挂起，切回即续）
- **指定面**——只推入勾选的方块面；配妥后再按第二个按钮打开面配置
- **ME 网络**——产物插入 ME 网络（CPU 余料也经网络返还）

## 高级充能台

带棒槽、真实能量缓存和 **4 个独立配方页** 的进阶款。

| 属性 | 行为 |
|----------|----------|
| 配方页 | **4** 页，各自独立输入、输出与进度 |
| 棒槽 | 4 槽 × 每槽最多 **16** 根 AP 棒 |
| 棒系列 | 必须全 **AE** 或全 **ME**（混用被阻止） |
| 棒档位 | 允许混档 |
| 能量缓存 | 已插棒容量之和（+ 能量卡），GUI 与覆盖层均显示 |
| 网络抽取 | 从 ME 充入棒缓存（AE 电网，或经 Applied Flux 抽 FE） |
| 输出槽 | 同种产物最多堆叠 **64** |

缓存工作方式：

1. 插入同一系列的 AP 棒，每根棒按档位增加缓存上限。
2. 充能台从 ME 网络抽能充入缓存。
3. 配方从缓存消耗能量，剩余留存。
4. 无棒 → 无缓存 → 不联网抽取。

AE 棒：网络侧抽 **AE**；内部缓存为 FE（显示时 AE = FE/2）。ME 棒：网络侧经
Applied Flux 抽 **FE**。

## ME 充能台

自动化款：样板供应器式主机，直接接收 ME 网络的合成任务。

| 属性 | 行为 |
|----------|----------|
| 配方页 | **4** 页，ME 订单可跨页投料 |
| 样板槽 | **36** 个已编码处理样板（4×9） |
| 网络任务 | 接收合成 CPU 的 `pushPattern` 订单（爆发式，类似 Induction Card） |
| 充能棒供能 | 同高级充能台（棒槽 + 缓存） |
| 返还 Inventory | 产物与余料经推送方样板供应器返还 |
| GUI | 充能台页 + 样板页（工具栏按钮），与样板供应器一致 |

用工具栏按钮打开样板页编码样板；合成 CPU 自动供料并取走产物。

## GUI

充能台 GUI 为 **176×199**，包含：

- 左侧 **6 个输入槽**（2×3）
- 右侧 **1 个输出槽**（只读）
- 输出槽旁的**竖向进度条**（悬停显示数值）
- 能量不足时进度条上方的**警告图标**
- **左侧工具栏**（主面板外）：
  1. **?**——打开本 GuideME 页面
  2. **<** / **>**——切换配方页（高级 / ME 充能台）
  3. **产物输出**——循环三态；处于「指定面」时第二个按钮打开面配置
  4. **样板**（仅 ME 充能台）——打开样板页
- **右侧面板**——5 个 AE2 升级槽（各档均有加速卡；高级 / ME 另有能量卡）

标题下方显示页码，高级充能台还显示棒缓存。

### 扳手交互

手持 **Powah 扳手**（链接模式）右键充能台**不会**打开 GUI，而是完成
棒→台链接（见[充能棒](energizing-rods.md)）。

## 配方

<RecipeFor id="applied_powah:fluix_energizing_orb" fallbackText="Powah 充能台 + AE2 输入总线 + AE2 接口。需要 Powah。" />

<RecipeFor id="applied_powah:advanced_energizing_orb" fallbackText="四条边中点各 1× 福鲁伊克斯充能台 + 四个角各 1× 下界棒 + 正中 1× 工程处理器。需要 Powah。" />

<RecipeFor id="applied_powah:me_energizing_orb" fallbackText="高级充能台 + AE2 样板供应器 + 2× 容量卡 + 工程处理器。需要 Powah。" />
