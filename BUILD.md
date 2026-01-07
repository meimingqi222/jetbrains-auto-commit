# 构建说明

本插件使用 Gradle (Kotlin DSL) 构建系统。

## 前置要求

- JDK 17 或更高版本
- Gradle 8.0 或更高版本（或使用 Gradle Wrapper）
- IntelliJ IDEA 2023.2 或更高版本（用于开发和测试）

## 构建步骤

### 方法 1：使用 Gradle Wrapper（推荐）

```bash
# Windows
gradlew.bat clean build

# Linux/Mac
./gradlew clean build
```

### 方法 2：使用系统 Gradle

```bash
gradle clean build
```

## 构建产物

构建成功后，插件包将生成在以下位置：

```
build/distributions/jetbrains-auto-commit-<version>.zip
```

## 在 IntelliJ IDEA 中运行和测试

### 方法 1：使用 Gradle 任务运行

```bash
# Windows
gradlew.bat runIde

# Linux/Mac
./gradlew runIde
```

这将启动一个新的 IntelliJ IDEA 实例，并自动加载插件。

### 方法 2：在 IDEA 中直接运行

1. 使用 IntelliJ IDEA 打开项目
2. 等待 Gradle 同步完成
3. 在 Run Configuration 中选择 "Run Plugin"
4. 点击运行按钮

## 调试插件

1. 在 IntelliJ IDEA 中打开项目
2. 在代码中设置断点
3. 选择 "Debug Plugin" Run Configuration
4. 点击调试按钮

## 常见问题

### 缺少 Gradle Wrapper

如果项目缺少 Gradle Wrapper 文件，可以使用以下命令生成：

```bash
gradle wrapper --gradle-version 8.5
```

### 依赖下载缓慢

如果依赖下载缓慢，可以配置国内镜像源。在 `build.gradle.kts` 中添加：

```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    mavenCentral()
}
```

### 插件无法加载

确保：
1. IntelliJ IDEA 版本 >= 2023.2
2. JDK 版本 >= 17
3. 所有依赖都已正确下载

## 发布插件

### 构建发布版本

```bash
gradlew.bat clean buildPlugin
```

### 上传到 JetBrains Marketplace

1. 访问 [JetBrains Plugin Repository](https://plugins.jetbrains.com/)
2. 登录并创建新插件或更新现有插件
3. 上传构建的 zip 文件

## 开发建议

- 定期运行 `gradlew.bat verifyPlugin` 来验证插件配置
- 使用 `gradlew.bat runIde` 进行快速测试
- 在提交代码前运行 `gradlew.bat check`（包括测试和代码检查）
- 参考官方文档：[IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
