---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Chinese / 中文
  position: 90
categories:
- applied powah
---

# Applied Powah（中文）

本页为中文摘要。完整英文页见本目录其它页面（GuideME 语言包可在 `ae2guide/_zh_cn/` 扩展全文翻译）。

## 概述

- **超密 / 极密能源元件**：12.8M / 102.4M AE；8×上一级 + 运算处理器。
- **AE / ME 充能棒**：完整方块；AE 棒抽网格 AE，ME 棒抽 Applied Flux FE；供给附近 Powah 充能台。
- **双端安装**：改变玩法并新增物品，服务端与客户端都需要。

## 配方示意

**AE 棒（十字）** — 中心为同档 Powah 棒，上下左右为 AE2 能源元件：

```
_ a _
a b a
_ a _
```

**ME 棒（四角）**：

```
a _ a
_ b _
a _ a
```

**AE ↔ ME**：同档无序互转。

## 掉落

默认 `rodsKeepEnergyOnBreak=true`：挖掘后物品保留棒内 FE 缓存（对齐 Powah）。

## 颜色

Tooltip 标签为浅灰，数值（如 `0/20M FE`）为更深灰色（对齐 Powah）。

## 充能台（规划，0.2.x）

- 仅**底部**接入 ME 网络（默认）；`orbMultiFacing=true` 时可测试多面连接。
- 配置文件：`config/applied_powah.toml`（不再使用 `-common` 后缀）。
