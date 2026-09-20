# Applied Powah — Releases

| 文件 | 版本 | 状态 |
|------|------|------|
| **`AppliedPowah-0.1.0-alpha.6.jar`** | `0.1.0-alpha.6` | **当前** |
| alpha.1–5 等 | — | 保留不删 |

许可证：LGPL-3.0-only

## alpha.6 相对 alpha.5 的修复

| 反馈 | 处理 |
|------|------|
| 名称显示为 `block.applied_powah...` | 补全 **`block.*` 与 `item.*`** 中英 lang |
| 无法充能 | 修 BE：放置时扫描充能台、`orbPos` 持久化、facing 侧接入网络；并清理失效 part 代码 |
| 尺寸太小 / 碰撞完整方块 | 碰撞改为 **Powah 同款细杆 VoxelShape**（按 facing）；模型仍用 rod+gem 组合 |
| 无光柱 | 增加 **`EnergizingRodRenderer`**（借 Powah beam 贴图），推能时棒→充能台光柱 |
| 误导性 tooltip | **物品不再写「存储:0/…」**（物品无 BE 能量；数值仅作设计参考时勿写进物品） |

## 前置

MC 1.20.1 + Forge 47+ + **AE2 15+**；Powah；AppFlux（ME 棒）。

## 文档

- 根目录 `README.md`、`AGENTS.md`
- `docs/guidebook/index.md`（及同目录各章文稿）
- `docs/工程指导.md`

## 构建

改 `gradle.properties` → `mod_version`，产物 `releases/AppliedPowah-<mod_version>.jar`。
