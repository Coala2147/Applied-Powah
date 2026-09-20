# Applied Powah — Releases

| 文件 | 版本 | 说明 |
|------|------|------|
| **`AppliedPowah-0.1.0-alpha.8.jar`** | `0.1.0-alpha.8` | **当前** |
| alpha.1–7 | — | 保留 |

Git：`https://github.com/Coala2147/Applied-Powah.git`

## alpha.8

1. **元件配方**  
   - 超密 = **8× AE2 致密** + 运算处理器  
   - 极密 = **8× 超密** + 运算处理器  
   - AE2 原版致密仍为 8×能源元件 + 运算处理器（本 mod 不改）

2. **能源元件状态（对齐 AE2）**  
   - 使用 `APEnergyCellBlockItem`（同 AE2 `internalCurrentPower`）  
   - 挖掘走 AE2 `exportSettings` 路径保留电量  
   - 悬停 tooltip：`12.8M / 12.8M AE` 一类

3. **JEI**  
   - AP 棒是 **工作台配方**，**不会**出现在 Powah「充能」台页面（那是 orb 配方）。  
   - 对 AP 棒按 **R** 应显示合成配方（配方 JSON 已去 BOM 并核对）。  
   - 完整 JEI 信息插件因 API 依赖暂未打进包，后续再加。

4. **指南**  
   - 创造栏 / 物品 **「Applied Powah 指南」**，右键打开简易指南界面。  
   - **长按 G 打开的是 AE2 GuideME**，目前 **尚未** 接入本 mod 页面；图文见 `docs/guidebook/` 与指南物品。

5. **构建**  
   - 依赖改为 `libs/` 本地 jar（见 `libs/README.md`）。

## 前置

1.20.1 Forge 47+ + **AE2 15+**；Powah；AppFlux（ME 棒）。
