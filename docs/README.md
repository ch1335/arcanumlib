# ArcanumLib 文档

本目录同时提供**中文**与**英文**两套文档。英文版用于发布成 GitHub Wiki，中文版随仓库提供。

| 主题 | 中文 | English（发布到 wiki） |
|---|---|---|
| 战利品表注入器 | [zh_cn/Loot-Injector.md](zh_cn/Loot-Injector.md) | [en_us/Loot-Injector.md](en_us/Loot-Injector.md) |
| 战利品条件 | [zh_cn/Loot-Conditions.md](zh_cn/Loot-Conditions.md) | [en_us/Loot-Conditions.md](en_us/Loot-Conditions.md) |
| 属性 | [zh_cn/Attributes.md](zh_cn/Attributes.md) | [en_us/Attributes.md](en_us/Attributes.md) |
| 注入器格式速览 | [loot_injector_format.json](loot_injector_format.json)（仓库内共用，不是 wiki 页面） | 同左 |

英文版另有 `en_us/Home.md`（wiki 首页）与 `en_us/_Sidebar.md`（wiki 侧边栏）。

## 约定

- **两套文档的文件名与章节结构一一对应**（行数、各级标题数、代码块数都对齐），方便对照，也方便写"语言对等"校验；
- 文件名用**连字符**（`Loot-Injector.md`）而不是空格：GitHub Wiki 的标题由文件名决定，连字符文件名的标题照样显示为 "Loot Injector"，但链接不需要 `%20` 转义；
- 改动时**请同时更新两种语言**；只改一种时在 PR 里说明原因，别让它长期失衡。

## 发布到 GitHub Wiki

GitHub Wiki **没有目录层级**，页面 URL 是 `/wiki/<页面名>`（不带 `.md`），所以内部的跨页链接必须是
**扁平页面名**（`[Loot Conditions](Loot-Conditions)`）。而仓库里的文档要用相对路径
（`[Loot Conditions](Loot-Conditions.md)`）才能在 GitHub 的文件页里点开——两种写法不能同时成立。

因此：**仓库里统一用 `.md` 相对链接，发布时用脚本转换**（与
[github-wiki-flat](https://claudskills.com/skills/github-wiki-flat/SKILL.md) 的建议一致）。

```shell
git clone https://github.com/<owner>/<repo>.wiki.git wiki

# Windows PowerShell 默认禁止运行 .ps1，所以要带 -ExecutionPolicy Bypass
powershell -ExecutionPolicy Bypass -File 1.21.1/docs/publish-wiki.ps1 -Target wiki -WhatIf   # 先预览
powershell -ExecutionPolicy Bypass -File 1.21.1/docs/publish-wiki.ps1 -Target wiki

cd wiki && git add -A && git commit -m "docs: sync" && git push
```

（装了 PowerShell 7 的话把 `powershell -ExecutionPolicy Bypass -File` 换成 `pwsh -File` 即可。）

脚本做的事：把 `en_us/*.md`（默认源目录就是脚本同级的 `en_us/`）写到 wiki 检出根目录，
并把 `](Name.md)` 重写成 `](Name)`。它还会：

- 拒绝写入**不像 wiki 检出**的目录（remote 里没有 `.wiki` 且目录非空就报错），防止误覆盖；
- 列出"不是 `.md`、在 wiki 上必然 404"的链接目标（目前是空的，因为英文文档之间只互相链接）。

> **两个实测遇到的问题，改这个脚本时注意**：
> 1. 脚本必须保持**纯 ASCII**：Windows PowerShell 5.1 会把**无 BOM 的 UTF-8 文件按 ANSI/GBK 读**，
>    中文注释会让它直接语法报错（已实测）。中文说明放这份 README 里就好。
> 2. 默认源目录是 `en_us/`，不是脚本所在的 `docs/`——否则会把这份 README 当成待发布的页面拷过去。

> 注意：wiki 上的内容以**默认分支**对外可见，且**不要**同时在网页上编辑——下一次发布会覆盖。

## 目标读者

| 读者 | 建议入口 |
|---|---|
| 模组作者（用它当依赖库 / 扩展它） | `Loot-Injector.md` 的「给模组的扩展点」与「用数据生成写注入器」 |
| 整合包作者（只想改掉落） | `Loot-Injector.md` 的「常见任务配方」与「常见任务配方」 |
| 想给自己的模组挂概率 / 属性 | `Loot-Conditions.md`、`Attributes.md` |
