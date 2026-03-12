# MoneyTracker - 记账本 Android App

一款简约现代风格的个人记账应用，基于Android原生技术开发。

## 功能特性

### 核心功能
- **收支记录**: 快速添加收入和支出记录
- **分类管理**: 预设10种支出分类和5种收入分类，支持自定义分类
- **统计分析**: 月度收支统计、分类饼图、趋势折线图
- **数据管理**: 数据备份与恢复功能

### 界面特点
- 简约现代的Material Design 3设计风格
- 清晰的视觉层次和舒适的配色方案
- 流畅的页面切换动画

## 技术栈

- **开发语言**: Kotlin
- **最低SDK版本**: API 24 (Android 7.0)
- **目标SDK版本**: API 34 (Android 14)
- **架构模式**: MVVM (Model-View-ViewModel)
- **数据库**: Room Persistence Library
- **UI框架**: Material Design Components
- **图表库**: MPAndroidChart
- **异步处理**: Kotlin Coroutines + Flow

## 项目结构

```
app/
├── src/main/java/com/example/moneytracker/
│   ├── MoneyTrackerApp.kt          # Application类
│   ├── data/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt      # Room数据库
│   │   │   ├── dao/                # 数据访问对象
│   │   │   └── entities/           # 实体类
│   │   └── repository/             # 数据仓库
│   ├── ui/
│   │   ├── main/                   # 主Activity
│   │   ├── home/                   # 首页模块
│   │   ├── transactions/           # 交易管理模块
│   │   ├── statistics/             # 统计分析模块
│   │   ├── categories/             # 分类管理模块
│   │   └── settings/               # 设置模块
│   ├── utils/                      # 工具类
│   └── adapters/                   # RecyclerView适配器
├── src/main/res/
│   ├── layout/                     # 布局文件
│   ├── values/                     # 资源文件
│   ├── drawable/                   # 图标资源
│   ├── navigation/                 # 导航配置
│   └── menu/                       # 菜单配置
└── build.gradle.kts               # 模块配置
```

## 数据库设计

### Transaction (交易记录表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| amount | Double | 金额 |
| type | Int | 类型：0=支出，1=收入 |
| categoryId | Long | 分类ID |
| description | String | 备注 |
| date | Long | 交易日期时间戳 |
| createdAt | Long | 创建时间 |

### Category (分类表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| name | String | 分类名称 |
| icon | String | 图标资源名 |
| color | Int | 颜色值 |
| type | Int | 类型：0=支出，1=收入 |
| isDefault | Boolean | 是否为预设分类 |

## 预设分类

### 支出分类
- 餐饮、交通、购物、娱乐、医疗、教育、居住、通讯、服饰、其他支出

### 收入分类
- 工资、兼职、投资、红包、其他收入

## 编译运行

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

### 编译步骤
1. 使用Android Studio打开项目
2. 等待Gradle同步完成
3. 点击Run按钮或使用快捷键Shift+F10运行

### 命令行编译
```bash
./gradlew assembleDebug
```

## 配色方案

- **主色调**: #4A90E2 (蓝色)
- **辅助色**: #50E3C2 (青绿色)
- **收入色**: #4CAF50 (绿色)
- **支出色**: #FF5252 (红色)
- **背景色**: #FAFAFA
- **卡片背景**: #FFFFFF
- **主文字**: #212121
- **次文字**: #757575

## 版本历史

### v1.0.0 (当前版本)
- 基础记账功能
- 分类管理
- 统计分析
- 数据备份与恢复

## 许可证

本项目仅供学习和参考使用。

## 联系方式

如有问题或建议，欢迎反馈。
