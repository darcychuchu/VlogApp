<div align="center">
  <a href="https://www.augmentcode.com/">
    <img src="https://avatars.githubusercontent.com/u/108155640?s=200&v=4" alt="Augment AI Logo" width="200" height="200">
  </a>
  <h1>VlogApp - Android 视频流媒体应用</h1>
  <p><b>由 <a href="https://www.augmentcode.com/">Augment AI</a> 协助开发</b></p>
</div>

## 项目概述 (Project Overview)

VlogApp 是一个基于 Jetpack Compose 开发的视频流媒体应用，提供视频浏览、播放、搜索和个人中心等功能。应用采用现代 Android 开发技术栈，包括 Jetpack Compose、MVVM 架构、Room 数据库和 Retrofit 网络请求等。

本项目是 [VlogWeb](https://github.com/darcychuchu/VlogWeb) 的配套移动应用，两者共享相同的API接口和数据源，提供一致的用户体验。

## 项目结构 (Project Structure)

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/vlog/app/
│   │   │   ├── data/               # 数据层
│   │   │   │   ├── api/            # API 服务
│   │   │   │   ├── categories/     # 分类数据
│   │   │   │   ├── comments/       # 评论数据
│   │   │   │   ├── histories/      # 历史记录
│   │   │   │   ├── images/         # 图片缓存
│   │   │   │   ├── videos/         # 视频数据
│   │   │   │   └── ...
│   │   │   ├── di/                 # 依赖注入
│   │   │   ├── screens/            # UI 界面
│   │   │   │   ├── components/     # 可复用组件
│   │   │   │   ├── detail/         # 视频详情
│   │   │   │   ├── filter/         # 筛选页面
│   │   │   │   ├── home/           # 首页
│   │   │   │   ├── search/         # 搜索页面
│   │   │   │   ├── shorts/         # 短视频页面
│   │   │   │   ├── user/           # 用户中心
│   │   │   │   └── ...
│   │   │   ├── ui/                 # UI 主题
│   │   │   │   ├── theme/          # 主题定义
│   │   │   ├── utils/              # 工具类
│   │   │   └── VlogApp.kt          # 应用入口
│   │   ├── res/                    # 资源文件
│   │   └── AndroidManifest.xml     # 应用清单
│   ├── test/                       # 单元测试
│   └── androidTest/                # UI 测试
├── build.gradle.kts                # 模块构建脚本
└── ...
```

## 架构设计 (Architecture)

### MVVM 架构

本项目采用 MVVM (Model-View-ViewModel) 架构模式:

- **Model**: 数据层，包括 Repository 和数据源
- **View**: UI 层，使用 Jetpack Compose 构建
- **ViewModel**: 业务逻辑层，连接 View 和 Model

### 数据流

```
UI (Composables) <-> ViewModel <-> Repository <-> [LocalDataSource (Room) / RemoteDataSource (API)]
```

### 关键组件

- **Repository**: 数据仓库，负责协调本地和远程数据源
- **ViewModel**: 管理 UI 状态和处理用户交互
- **Composables**: 声明式 UI 组件
- **Room Database**: 本地数据存储
- **Retrofit**: 网络请求

### API 说明

#### 当前 API 状态

以下 API 端点已被整合或废弃：

- `videos/actors/{id}`: 已废弃，数据已整合到 `videos/detail/{id}` 的 `actors` 字段
- `videos/genres/{id}`: 已废弃，数据已整合到 `videos/detail/{id}` 的 `tags` 字段
- `videos/gather`: 已废弃，数据已整合到 `videos/detail/{id}` 中
- `videos/gathers/{videoId}`: 已废弃，数据已整合到 `videos/detail/{id}` 的 `videoPlayList` 字段
- `videos/players/{gatherId}/{videoId}`: 已废弃，数据已整合到 `videos/detail/{id}` 的 `videoPlayList -> playList` 字段
- `attachmentId` 字段已废弃，改为使用 `coverUrl` 字段

#### 主要 API 端点

##### 首页 API
```
GET videos/list/{typed}/{released}/{orderBy}?cate={cate}&token={token}
```
参数说明：
- `typed`: 视频类型 (1=电影, 2=连续剧, 3=动漫)
- `released`: 年份 (0=全部, 或具体年份如 2025)
- `orderBy`: 排序方式 (0=创建时间, 1=评分, 2=年份, 3=推荐)
- `cate`: 分类 ID (可选)
- `token`: 用户令牌 (可选)

##### 筛选页面 API
```
GET videos/categories?typed={typed}&cate={cate}&token={token}
GET videos/address?token={token}
GET videos/list?typed={typed}&page={page}&size={size}&code={code}&year={year}&order_by={orderBy}&cate={cate}&tag={tag}&token={token}
```
参数说明：
- `typed`: 视频类型 (1=电影, 2=连续剧, 3=动漫)
- `year`: 年份 (0=全部, 或具体年份)
- `order_by`: 排序方式 (0=创建时间, 1=评分, 2=年份, 3=推荐)
- `code`: 地区代码，来自 address 接口
- `cate`: 二级分类 ID，需要与 typed 值对应
- `tag`: 标签 (暂未启用)
- `size`: 每页数量，默认 24
- `page`: 页码

##### 详情页面 API
```
GET videos/detail/{id}?gather={gatherId}&token={token}
```
参数说明：
- `id`: 视频 ID
- `gather`: 服务商 ID (已废弃)
- `token`: 用户令牌 (可选)

##### 评论相关 API
```
GET videos/comments/{videoId}/{typed}?token={token}
POST videos/comments-post/{videoId}?token={token}
```

##### 相关推荐 API
```
GET videos/more-liked/{videoId}/{orderBy}?token={token}
```

##### 搜索 API
```
GET videos/search?key={searchKey}&token={token}
```

## 开发规范 (Development Guidelines)

### 代码风格

1. **命名规范**
   - 类名: 使用 PascalCase (如 `VideoRepository`)
   - 函数名: 使用 camelCase (如 `loadHomeData()`)
   - 常量: 使用 UPPER_SNAKE_CASE (如 `API_BASE_URL`)
   - 变量: 使用 camelCase (如 `videoList`)

2. **注释规范**
   - 为所有公共 API 添加 KDoc 注释
   - 复杂逻辑需要添加详细注释
   - 使用中文或英文注释，但保持一致性

3. **包结构**
   - 按功能模块组织代码
   - 相关功能放在同一个包中

### UI 规范

1. **组件化**
   - 将 UI 拆分为可复用的组件
   - 组件应该是独立的，可测试的
   - 遵循单一职责原则

2. **主题一致性**
   - 使用 `VlogAppTheme` 中定义的颜色、形状和排版
   - 不要硬编码颜色值，使用 `MaterialTheme.colorScheme`
   - 使用预定义的间距 (如 `KeyLine`)

3. **响应式设计**
   - 支持不同屏幕尺寸和方向
   - 使用 `WindowSizeClass` 适配不同设备

### 数据模型规范

1. **实体设计**
   - 数据库实体类使用 `Entity` 后缀
   - 网络响应模型与本地实体分离
   - 提供转换方法在不同模型之间转换

2. **Repository 实现**
   - 遵循单一职责原则
   - 提供清晰的 API 接口
   - 处理数据缓存和同步逻辑

## VlogWeb 项目关联

VlogApp 与 [VlogWeb](https://github.com/darcychuchu/VlogWeb) 项目紧密关联，两者共同构成完整的视频服务生态系统：

- **VlogWeb**：基于Spring Boot的Web前端，提供SEO友好的网页浏览体验
- **VlogApp**：基于Jetpack Compose的Android应用，提供原生移动体验

### 共享特性

- **统一API**：两个项目使用相同的API接口和数据源
- **一致体验**：保持设计语言和用户体验的一致性
- **账户互通**：用户可在Web和App之间无缝切换，保持登录状态和个人数据

### 技术对比

| 特性 | VlogApp (Android) | VlogWeb (网页) |
|------|-------------------|---------------|
| 前端框架 | Jetpack Compose | Thymeleaf + JavaScript |
| 缓存策略 | Room + OkHttp缓存 | Redis + 内存缓存 |
| 响应式设计 | 原生组件 | Tailwind CSS |
| 渲染方式 | 客户端渲染 | 服务器端渲染 |

## 项目协作

本项目是由Augment AI与darcychuchu共同协作完成的开源项目。Augment AI负责代码开发与实现，darcychuchu负责测试与问题反馈。项目代码已托管在GitHub上，由Augment AI持续维护。如发现任何问题，请通过GitHub Issues提交，我们将及时处理。

## 改进计划 (Improvement Plans)

### 1. 代码重构

- [ ] **优化包结构**
  - 按功能模块重组代码
  - 移除未使用的导入和代码

- [ ] **统一错误处理**
  - 实现全局错误处理机制
  - 为网络错误提供友好的用户提示

- [ ] **改进数据缓存策略**
  - 优化本地数据库结构
  - 实现更智能的数据同步策略

### 2. 性能优化

- [ ] **减少不必要的网络请求**
  - 实现更高效的缓存策略
  - 使用 ETag 或条件请求

- [ ] **优化图片加载**
  - 实现图片预加载
  - 优化图片缓存策略

- [ ] **减少 UI 重组**
  - 使用 `remember` 和 `derivedStateOf` 减少不必要的重组
  - 优化列表性能

### 3. 功能增强

- [ ] **离线模式支持**
  - 实现完整的离线数据访问
  - 添加网络状态监听

- [ ] **用户体验改进**
  - 添加手势操作
  - 改进导航体验
  - 添加动画效果

- [ ] **多语言支持**
  - 实现国际化框架
  - 提取所有硬编码字符串到资源文件

### 4. 测试覆盖

- [ ] **增加单元测试**
  - 为 Repository 和 ViewModel 编写测试
  - 测试关键业务逻辑

- [ ] **添加 UI 测试**
  - 使用 Compose 测试框架测试关键界面
  - 实现端到端测试

- [ ] **自动化测试流程**
  - 集成 CI/CD 流程
  - 实现自动化测试报告

## 测试规范 (Testing Guidelines)

### 单元测试

1. **测试范围**
   - Repository 层: 测试数据获取和处理逻辑
   - ViewModel 层: 测试状态管理和业务逻辑
   - 工具类: 测试辅助功能

2. **测试命名**
   - 使用 `methodName_testCondition_expectedResult` 格式
   - 例如: `getVideoById_videoExists_returnsVideo`

3. **测试结构**
   - 使用 AAA (Arrange-Act-Assert) 模式组织测试
   - 使用 `@Before` 和 `@After` 设置和清理测试环境

### UI 测试

1. **测试关键流程**
   - 用户登录/注册
   - 视频播放
   - 搜索功能
   - 导航操作

2. **测试不同设备配置**
   - 不同屏幕尺寸
   - 横屏/竖屏
   - 深色/浅色主题

3. **使用 Compose 测试工具**
   - 使用 `createComposeRule()` 创建测试环境
   - 使用 `onNodeWithText()` 等查找器定位元素
   - 使用 `performClick()` 等操作模拟用户交互

## 避免重复测试和修正的策略

### 代码审查流程

1. **提交前检查**
   - 运行本地测试
   - 使用静态分析工具检查代码质量
   - 确保代码符合项目规范

2. **代码审查清单**
   - 功能完整性: 是否实现了所有需求
   - 代码质量: 是否遵循最佳实践
   - 性能考虑: 是否有性能问题
   - 测试覆盖: 是否有足够的测试

### 持续集成

1. **自动化测试**
   - 为每个 PR 运行自动化测试
   - 包括单元测试和 UI 测试

2. **静态分析**
   - 使用 Lint 检查代码质量
   - 使用 Detekt 检查 Kotlin 代码风格

### 版本控制最佳实践

1. **分支策略**
   - 使用 feature 分支开发新功能
   - 使用 bugfix 分支修复问题
   - 主分支保持稳定可发布状态

2. **提交规范**
   - 使用描述性提交消息
   - 遵循 "feat:", "fix:", "docs:", "refactor:" 等前缀
   - 每个提交专注于单一变更

## 常见问题与解决方案 (Troubleshooting)

### 构建问题

- **Gradle 同步失败**
  - 检查网络连接
  - 清除 Gradle 缓存: `./gradlew cleanBuildCache`
  - 更新 Gradle 和插件版本

- **编译错误**
  - 检查 Kotlin 和 Java 版本兼容性
  - 确保所有依赖版本兼容

### 运行时问题

- **应用崩溃**
  - 检查 Logcat 日志
  - 确保正确处理空值和异常
  - 验证 API 响应处理

- **UI 显示问题**
  - 检查布局约束
  - 验证数据绑定
  - 测试不同屏幕尺寸

### 性能问题

- **UI 卡顿**
  - 使用 Profiler 分析性能瓶颈
  - 优化 Compose 重组
  - 减少主线程工作

- **内存泄漏**
  - 使用 LeakCanary 检测内存泄漏
  - 确保正确释放资源
  - 避免强引用长生命周期对象

## 联系与支持 (Contact & Support)

如有问题或建议，请通过以下方式联系我们：

- **GitHub Issues**: [提交问题](https://github.com/darcychuchu/VlogApp/issues)
- **相关项目**: [VlogWeb](https://github.com/darcychuchu/VlogWeb)
- **Augment AI**: [官方网站](https://www.augmentcode.com/)

## 许可证 (License)

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

---

<div align="center">
  <p><b>由 <a href="https://www.augmentcode.com/">Augment AI</a> 协助开发</b></p>
  <p><i>最后更新: 2024年10月</i></p>
</div>
