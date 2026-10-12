---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: 充能棒
  position: 20
  icon: applied_powah:ae_energizing_rod_starter
categories:
  - Applied Powah
item_ids:
  - applied_powah:ae_energizing_rod_starter
  - applied_powah:ae_energizing_rod_basic
  - applied_powah:ae_energizing_rod_hardened
  - applied_powah:ae_energizing_rod_blazing
  - applied_powah:ae_energizing_rod_niotic
  - applied_powah:ae_energizing_rod_spirited
  - applied_powah:ae_energizing_rod_nitro
  - applied_powah:me_energizing_rod_starter
  - applied_powah:me_energizing_rod_basic
  - applied_powah:me_energizing_rod_hardened
  - applied_powah:me_energizing_rod_blazing
  - applied_powah:me_energizing_rod_niotic
  - applied_powah:me_energizing_rod_spirited
  - applied_powah:me_energizing_rod_nitro
---

# 充能棒

全方块充能棒：从 **ME 网络**取能，供给附近的 **Powah 充能台**（或本模组的
[充能台](energizing-orbs.md)）。共七档，与 Powah 对齐：初级 → 基础 → 硬化 → 烈焰 →
钻石 → 富生 → 下界。

<Row>
  <BlockImage id="applied_powah:ae_energizing_rod_starter" scale="4" />
  <BlockImage id="applied_powah:me_energizing_rod_starter" scale="4" />
</Row>

| 系列 | 能量来源 | 备注 |
|--------|---------------|-------|
| <ItemLink id="applied_powah:ae_energizing_rod_starter" /> … 下界 | ME 电网的 **AE** | 无需额外模组 |
| <ItemLink id="applied_powah:me_energizing_rod_starter" /> … 下界 | ME 网络的 **FE** | 需要 **Applied Flux**；未安装时配方禁用 |

## 各档数值

与 Powah 档位一致。物品提示栏显示同样数值；挖下棒时缓存会保留在物品上（可配置）。

| 档位 | 缓存（AE 棒） | 输出（AE 棒） | 缓存（ME 棒） | 输出（ME 棒） |
|------|-----------------|-------------------|-----------------|-------------------|
| 初级 | 5k | 50/t | 10k | 100/t |
| 基础 | 20k | 200/t | 40k | 400/t |
| 硬化 | 50k | 500/t | 100k | 1k/t |
| 烈焰 | 200k | 2k/t | 400k | 4k/t |
| 钻石 | 500k | 5k/t | 1M | 10k/t |
| 富生 | 2M | 20k/t | 4M | 40k/t |
| 下界 | 10M | 100k/t | 20M | 200k/t |

## 使用方法

1. 紧贴 **AE2 线缆**放置并朝向它——详见[放置](placement.md)。
2. 在附近放一个 **Powah 充能台**（或本模组充能台）并载入有效配方。
3. 棒从网络抽能入缓存，再推入充能台；供能时有光束显示。

### 扳手链接（Powah 扳手）

把棒绑定到指定充能台（Powah 同款流程）：

1. 扳手切到 **Link** 模式（空中 shift+右键）。
2. 右键**棒**——聊天栏提示"开始链接"。
3. 右键**充能台**——聊天栏提示"链接完成"。

之后即使附近有其它充能台，该棒也只供能这台。对已链接扳手右键 AP 充能台
完成的是链接，不会打开 GUI。

## 配方

AE 棒用能量元件摆**十字**；ME 棒摆**四角**；中间放同档 Powah 棒。升级沿用
Powah 图案（中间放上一档 AP 棒），AE ↔ ME 为无序转换。

<RecipeFor id="applied_powah:ae_rod_starter" fallbackText="4× AE2 能源元件摆十字 + Powah 初级棒（中间）。" />

<RecipeFor id="applied_powah:me_rod_starter" fallbackText="4× AE2 能源元件摆四角 + Powah 初级棒（中间）。需要 Applied Flux。" />
