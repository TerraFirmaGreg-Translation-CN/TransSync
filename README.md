# Paratranz 翻译同步工具

## 功能

- 批量上传源文件。
- 批量上传译文。
- 批量下载译文。
- 可配置扫描文件及生成译文路径映射。
- 可配置忽略部分文件。
- 提供GUI桌面应用，可视化操作。
- 提供命令行模式，支持从环境变量中读取配置，可接入CI/CD。

## 配置

### 环境变量

| 名称                   | 描述                                         |
|:---------------------|:-------------------------------------------|
| PARATRANZ_API_TOKEN  | Paratranz API Token                        |
| PARATRANZ_PROJECT_ID | Paratranz 项目 ID                            |
| PARATRANZ_WORKSPACE  | 工作目录，用于扫描待翻译的源文件，以及存储译文。                   |
| HTTP_LOG_LEVEL | HTTP 请求日志级别，可选值：NONE, BASIC, HEADERS, BODY |

### 参考配置文件

```json
{
    "token" : "Go https://paratranz.cn/users/my get your token",
    "projectId" : 15950,
    "httpLogLevel" : "NONE",
    "workspace" : "..",
    "rules" : [ {
        "sourcePattern" : "Tools-Modern/LanguageMerger/LanguageFiles/**/en_us/**.json",
        "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
        "srcLang" : "en_us",
        "destLang" : "zh_cn",
        "ignores" : [ ]
    }, {
        "sourcePattern" : "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/**.json",
        "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
        "srcLang" : "en_us",
        "destLang" : "zh_cn",
        "ignores" : [ "**/tfg_ores/*_index.json" ]
    } ]
}
```

## 文件扫描映射

参数说明:

| 参数                   | 描述                                                                               |
|:---------------------|:---------------------------------------------------------------------------------|
| workspace             | 工作目录，用于扫描待翻译的源文件，以及存储译文。程序只会扫描位于工作目录(workspace)下的文件和目录。                          |
| sourcePattern         | 源文件匹配规则，用于识别要翻译的文件。支持[Glob](https://en.wikipedia.org/wiki/Glob_(programming))语法。 |
| translationPattern   | 映射译文规则，根据源文件路径生成译文路径。详见下文。                                                       |
| srcLang               | 源语言，用于识别源文目录结构。                                                                  |
| destLang              | 目标语言，用于生成译文文件。                                                                   |
| ignores               | 忽略列表，用于忽略某些文件。                                                                   |

```json
{
  "workspace": "/data/tfgcn",
  "rules": [
    {
      "sourcePattern" : "Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/**.json",
      "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
      "srcLang" : "en_us",
      "destLang" : "zh_cn",
      "ignores" : [ "**/tfg_ores/*_index.json" ]
    }
  ]
}
```

### sourcePattern

首先，程序执行时需要确认翻译文件范围。根据 `sourcePattern` 确定原文，随后才能上传到 `paratrans` 项目中。

**1、单文件扫描**

若扫描目录没有复杂结构，只需要识别指定语言文件，例如：

```
test/ae2/en_us.json
test/create/en_us.json
```

扫描语法如下：

| Pattern            | 说明                                                               | 结果                                              |
|:-------------------|:-----------------------------------------------------------------|-------------------------------------------------|
| `test/**/en_us.json` | 识别 test/**/ 目录下的所有en_us.json文件                       | test/ae2/en_us.json<br/> test/create/en_us.json |

**2、原文分散在多文件、多目录中**

以下面的目录结构为例：

```
test/en_us/blocks.json
test/en_us/items.json
test/en_us/Quests/chapter1.json
test/en_us/Quests/chapter2/chapter2.json
test/en_us/Quests/chapter3/chapter3/chapter3.json
```

不同扫描语法对应的结果如下：

| Pattern              | 说明                                   | 结果                                                                                                                                                                             |
|:---------------------|:-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test/en_us/*.json`    | 不包含子目录下的文件。                          | test/en_us/blocks.json<br/> test/en_us/items.json                                                                                                                              |
| `test/en_us/**.json`   | 所有文件，含子目录下的文件。                       | test/en_us/blocks.json<br/> test/en_us/items.json<br/> test/en_us/Quests/chapter1.json<br/> test/en_us/Quests/chapter2/chapter2.json<br/> test/en_us/Quests/chapter3/chapter3/chapter3.json |
| `test/en_us/**/*.json` | 只包含子目录下的文件，但不含 `test/en_us/` 目录下的文件。 | test/en_us/Quests/chapter1.json<br/> test/en_us/Quests/chapter2/chapter2.json<br/> test/en_us/Quests/chapter3/chapter3/chapter3.json                                                    |

**模组整合包，语言目录上级还有其他目录**

对于模组整合包，通常目录下首先会有多个mod目录，然后才是语言目录。以下面结构为例：

```
test/ae2/en_us/Quests/chapter.json
test/ae2/en_us/lang.json
test/tfg/en_us/Quests/chapter.json
test/tfg/en_us/lang.json
```

| Pattern              | 说明                                     | 结果                                                                                        |
|:---------------------|:---------------------------------------|-------------------------------------------------------------------------------------------|
| `test/**/en_us/*.json` | 不包含子目录下的文件。                            | test/ae2/en_us/lang.json<br/> test/tfg/en_us/lang.json                                    |
| `test/**/en_us/**.json` | 所有文件，含子目录下的文件。                         | test/ae2/en_us/Quests/chapter.json<br/> test/ae2/en_us/lang.json<br/> test/tfg/en_us/Quests/chapter.json<br/> test/tfc/en_us.json |
| `test/**/en_us/**/*.json` | 含子目录下的文件，但不包含 `test/**/en_us/` 目录下的文件。 | test/ae2/en_us/Quests/chapter.json<br/> test/tfg/en_us/Quests/chapter.json                     |

### translationPattern

再确认了翻译文件范围后，下一步是确定译文路径。

本应用支持4个占位符：

| 占位符                    | 说明                  |
|:-----------------------|:--------------------|
| `%language%`           | 目标语言，例如：`zh_cn`     |
| `%original_file_name%` | 原文文件名，例如：`lang.json` |
| `%original_path_pre%`  | 根于源语言分割路径，取语言前面的部分。 |
| `%original_path%`      | 根于源语言分割路径，取语言后面的部分。 |

当根据 `sourcePattern` 扫描到一个文件路径时，算法会查找 `srcLang` 首次出现的位置，并把路径分割成3个部分：

`%original_path_pre%/` srcLang `/%original_path%/` `%original_file_name%`

例如: 

`Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/en_us/entries/tfg_ores/ore_basic.json`

规则:
```json
{
  "sourcePattern" : "Modpack-Modern/kubejs/assets/**/en_us/**/*.json",
  "translationPattern" : "%original_path_pre%/%language%/%original_path%/%original_file_name%",
  "srcLang" : "en_us",
  "destLang" : "zh_cn"
}
```

根据 srcLang = en_us 找到的位置，将路径分割成3个部分：

- `original_path_pre`: `Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide`
- `original_path`: `entries/tfg_ores`
- `original_file_name`: `ore_basic.json`

根据 destLang = zh_cn，`%language%` 替换为 `zh_cn`，得到结果：

`Modpack-Modern/kubejs/assets/tfc/patchouli_books/field_guide/zh_cn/entries/tfg_ores/ore_basic.json`