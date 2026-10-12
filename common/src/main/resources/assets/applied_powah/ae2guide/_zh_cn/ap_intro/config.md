---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: 配置
  position: 50
categories:
  - Applied Powah
---

# 配置

`config/applied_powah-common.toml`（首次运行生成）。分两个小节：

### `[charging_rod]`

| 键 | 默认 | 含义 |
|-----|---------|---------|
| `pullIntervalTicks` | 1 | 网络抽能充入棒缓存的间隔（tick） |
| `networkReserveRatio` | 0.05 | 抽取后 ME 网格必须保留的能量比例（5%） |
| `aeBurstAe` | 10000000 | AE 棒每次抽取的 AE 量；FE 当量约为 AE × 2 |
| `meBurstFe` | 20000000 | ME 棒每次抽取的 FE 量（Applied Flux） |
| `rodsKeepEnergyOnBreak` | true | 挖下/放置的棒在物品 NBT 上保留 FE 缓存（Powah 式掉落） |

### `[energizing_orb]`

| 键 | 默认 | 含义 |
|-----|---------|---------|
| `orbMultiFacing` | false | 充能台可使用非朝上的视觉朝向；ME 连接仍仅限底面 |
| `orbAutoExport` | false | 自动推出产物（相邻容器 / ME），每 tick 限速 |
| `orbExportItemsPerTick` | 8 | 每个充能台每 tick 自动输出的最大物品数 |
| `orbPullFromNetwork` | true | 高级充能台从 ME 网络抽能入棒缓存；ME 充能台从不联网抽取（仅靠棒供能） |

平衡请按整合包调整；除 Forge 另行说明外，所有改动即时生效。
