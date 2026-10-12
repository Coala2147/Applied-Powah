---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: 能源元件
  position: 10
  icon: applied_powah:super_dense_energy_cell
categories:
  - Applied Powah
item_ids:
  - applied_powah:super_dense_energy_cell
  - applied_powah:extreme_dense_energy_cell
---

# 能源元件

两块大容量 AE 能源元件，供用满 <ItemLink id="dense_energy_cell" /> 之后仍嫌不够的网络使用。
行为与 AE2 原版元件完全一致——同样的存储语义、同样的充能行为、同样的
`internalCurrentPower` NBT——只是更大。

<Row>
  <BlockImage id="applied_powah:super_dense_energy_cell" scale="4" p:fullness="4" />
  <BlockImage id="applied_powah:extreme_dense_energy_cell" scale="4" p:fullness="4" />
</Row>

| 元件 | 容量 | 合成 |
|------|----------|--------------|
| <ItemLink id="applied_powah:super_dense_energy_cell" /> | 12.8M AE | 8× <ItemLink id="energy_cell" /> + 运算处理器 |
| <ItemLink id="applied_powah:extreme_dense_energy_cell" /> | 102.4M AE | 8× 超密元件 + 运算处理器 |

## 使用方法

1. 紧贴 ME 线缆或任何可联网设备放置。
2. 与普通 AE2 元件一样，为网络能量池增加容量。
3. 悬停物品——或用 Jade / WTHIT / TOP 查看方块——即可看到存储能量；
   显示直接走 AE2 自己的覆盖层集成，与原版元件格式一致。

生存模式下挖下元件会保留已存储的 AE。

<RecipeFor id="applied_powah:super_dense_energy_cell" fallbackText="8× 能源元件 + 运算处理器。" />

<RecipeFor id="applied_powah:extreme_dense_energy_cell" fallbackText="8× 超密能源元件 + 运算处理器。" />
