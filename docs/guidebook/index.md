# Applied Powah 指南（GuideME）

游戏内指南使用 **GuideME**（AE2 15.4.10 的硬依赖），不为本模组另做自定义 GUI 书。

## 资源位置

```
common/src/main/resources/assets/applied_powah/applied_powah_guide/
├── index.md
├── energy-cells.md
├── energizing-rods.md
├── placement.md
├── recipes.md
└── config.md
```

- Guide id：`applied_powah:guide`
- 资源文件夹：`applied_powah_guide`（API：`Guide.builder(...).folder("applied_powah_guide")`）
- 官方文档：https://guideme.appliedenergistics.org/

## Hold G

页面 frontmatter 的 `item_ids` 列出物品 id 后，GuideME 的 **G** 键可从物品 tooltip 跳到对应页。

## 文风

对齐 AE2 guidebook：陈述机制与步骤，使用标准术语，避免口语与营销用语。

## AE2 手册合并（可选，未启用）

AE2 会加载所有命名空间下 `ae2guide/` 目录中的页面并入其导航树（见 AE2 `guidebook.md`）。若希望内容同时出现在 AE2 手册中，可将页面放到 `assets/applied_powah/ae2guide/`。当前版本使用独立 GuideME 手册。
