---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: 配置
  position: 50
categories:
  - Applied Powah
---

# 配置

`config/applied_powah-common.toml`（首次运行后名称以实际生成为准）：

| 键 | 默认 | 含义 |
|----|------|------|
| `pullIntervalTicks` | 1 | 从网络抽取的间隔（tick） |
| `networkReserveRatio` | 0.05 | ME 网格必须保留的能量比例（5%） |
| `aeBurstAe` | 10000000 | AE 棒每次抽取的 AE 量 |
| `meBurstFe` | 20000000 | ME 棒每次抽取的 FE 量 |

具体数值可改；平衡请在整合包中调整。
