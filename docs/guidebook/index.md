# Applied Powah 指南（并入 AE2 GuideME）

与 ExtendedAE / Advanced AE 相同：页面放在 **`assets/<modid>/ae2guide/`**，
由 AE2 的 GuideME 手册合并进**同一导航树**（侧栏会出现 `Add-on: Applied Powah`）。

```
common/src/main/resources/assets/applied_powah/ae2guide/ap_intro/
├── ap_intro-index.md
├── energy-cells.md
├── energizing-rods.md
├── placement.md
├── recipes.md
└── config.md
```

- 不单独 `Guide.builder` 另开手册。
- 指南物品打开 `ae2:guide`，并锚点到 `applied_powah:ap_intro/ap_intro-index.md`。
- Hold **G**：页面 frontmatter `item_ids` 列出 AP 物品 id。
- 文风对齐 AE2 handbook（机制与步骤，不用口语）。

参考：ExtendedAE `assets/expatternprovider/ae2guide/epp_intro/epp_intro-index.md`；
AE2 `guidebook.md`（所有命名空间的 `ae2guide/` 均会并入）。
