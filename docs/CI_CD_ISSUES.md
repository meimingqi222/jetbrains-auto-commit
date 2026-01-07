# GitHub Actions CI/CD 构建问题汇总

本文档记录了 JetBrains 插件项目在 GitHub Actions 中遇到的构建问题及解决方案，供后续参考。

## 问题 1：gradlew 权限被拒绝

**错误信息**：
```
./gradlew: Permission denied
exit code: 126
```

**原因**：gradlew 文件在 Linux 上没有执行权限。

**解决方案**：
```yaml
- name: Build Plugin
  run: chmod +x gradlew && ./gradlew buildPlugin
```

---

## 问题 2：Plugin ID 前缀不允许

**错误信息**：
```
The plugin ID 'com.example.aicommit' has a prefix 'com.example' that is not allowed.
```

**原因**：JetBrains Marketplace 不允许使用 `com.example` 作为前缀。

**解决方案**：使用自己的域名或 GitHub username 作为前缀：
```xml
<id>com.github.meimingqi222.aicommit</id>
```

对应更新 `build.gradle.kts`：
```kotlin
group = "com.github.meimingqi222"
```

---

## 问题 3：Plugin 描述无效

**错误信息**：
```
Invalid plugin descriptor 'description'. The plugin description is shorter than 40 characters and/or the plugin description contains non-Latin characters.
```

**原因**：
- 描述太短（少于 40 字符）
- 包含非拉丁字符（如中文）

**解决方案**：使用英文描述，至少 40 字符：
```xml
<description><![CDATA[
<p>AI-powered Git commit message generator for JetBrains IDEs. Uses OpenAI-compatible APIs (like DeepSeek) to automatically generate meaningful commit messages based on staged changes.</p>
<p>Features: multi-language support, custom prompt templates, keyboard shortcuts, and seamless Git integration.</p>
]]></description>
```

---

## 问题 4：GitHub Action 权限不足

**错误信息**：
```
Resource not accessible by integration
HTTP 403: Resource not accessible by integration
```

**原因**：工作流没有足够的权限执行某些操作（如上传 release assets）。

**解决方案**：在 workflow 文件顶部添加 permissions：
```yaml
permissions:
  contents: write
  attestations: write
  id-token: write
```

---

## 问题 5：gh CLI 在 Actions 中缺少认证

**错误信息**：
```
To use GitHub CLI in a GitHub Actions workflow, set the GH_TOKEN environment variable.
```

**原因**：Actions 中的 gh CLI 默认没有认证。

**解决方案**：显式设置 GH_TOKEN 环境变量：
```yaml
- name: Upload to Release
  env:
    GH_TOKEN: ${{ github.token }}
  run: |
    gh release upload v1.0.0 build/distributions/*.zip --repo ${{ github.repository }}
```

---

## 问题 6：Download Artifact 路径问题

**错误信息**：
```
no matches found for `build/distributions/*.zip`
```

**原因**：`actions/download-artifact` 默认将文件下载到工作目录根目录，而非指定路径。

**解决方案**：正确配置 path 参数：
```yaml
- name: Download Build Artifact
  uses: actions/download-artifact@v4
  with:
    name: plugin-dist
    path: build/distributions
```

---

## 问题 7：generate_release_notes 权限问题

**错误信息**：
```
Resource not accessible by integration - https://docs.github.com/rest/releases/releases#generate-release-notes-content-for-a-release
```

**原因**：使用 `softprops/action-gh-release` 的 `generate_release_notes` 功能需要额外权限。

**解决方案**：改用 gh CLI 上传，或移除该选项：
```yaml
# 使用 gh CLI
- name: Upload to Release
  env:
    GH_TOKEN: ${{ github.token }}
  run: gh release upload v1.0.0 build/distributions/*.zip --repo ${{ github.repository }}
```

---

## 问题 8：文件换行符警告

**警告信息**：
```
warning: LF will be replaced by CRLF the next time Git touches it
```

**原因**：Windows 环境提交的文件换行符为 LF，Windows 期望 CRLF。

**解决方案**：这是无害警告，不影响构建。如需解决，可配置 `.gitattributes`：
```gitattributes
* text=auto eol=lf
gradlew text eol=lf
*.sh text eol=lf
```

---

## 最佳实践

### 1. Workflow 文件模板

```yaml
name: Build and Release Plugin

on:
  release:
    types: [created]

permissions:
  contents: write
  attestations: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      - name: Build Plugin
        run: chmod +x gradlew && ./gradlew buildPlugin
      - name: Upload Build Artifact
        uses: actions/upload-artifact@v4
        with:
          name: plugin-dist
          path: build/distributions/*.zip
      - name: Verify Plugin
        run: ./gradlew verifyPlugin

  release:
    needs: build
    if: startsWith(github.ref, 'refs/tags/')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with:
          name: plugin-dist
          path: build/distributions
      - name: Upload to Release
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release upload ${{ github.ref_name }} build/distributions/*.zip --repo ${{ github.repository }}
```

### 2. 常用命令

```bash
# 删除本地和远程 tag
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0

# 删除 GitHub release
gh release delete v1.0.0 -y

# 创建新的 release（触发工作流）
gh release create v1.0.0 --title "v1.0.0" --notes "Release notes"

# 查看工作流状态
gh run list --limit 5
gh run watch <run-id> --interval 15
```

### 3. Plugin 发布前检查清单

- [ ] Plugin ID 使用自己的域名/username 前缀
- [ ] Plugin 描述至少 40 字符，使用英文
- [ ] build.gradle.kts 中的 group 与 plugin.xml 中的 vendor 匹配
- [ ] 版本号格式正确（如 v1.0.0）
- [ ] 本地运行 `./gradlew verifyPlugin` 通过

---

## 相关文件

- Workflow 配置：`.github/workflows/build.yml`
- Plugin 描述：`src/main/resources/META-INF/plugin.xml`
- 构建配置：`build.gradle.kts`
