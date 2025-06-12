# Autel 无人机平板应用 UI 组件详细开发指南

---

## 一、项目结构与模块说明

- **module_widget**：无人机飞行主界面控件库，包含姿态、地图、相机、云台、状态栏等所有飞行相关UI组件。
- **module_setting**：无人机设置界面，包含飞行参数、相机参数、遥控器、系统等设置项，支持源码级定制。
- **uxddemo（主工程）**：主入口Activity（如UxsdkDemoActivity），负责各控件的集成、调度和主界面交互。

---

## 二、主界面 Widget 组件一览与用法

![20250612-121325](.\20250612-121325.jpg)

### 1. 组件目录结构

- attitude（姿态球/仪表）
- cancellanding（取消降落按钮）
- codectoolright（右侧工具栏）
- colouratla（色彩/直方图）
- compass（指南针）
- focusAndZoom（对焦与变焦）
- fullScreen（全屏切换）
- gimbalpitch（云台俯仰）
- histogram（直方图）
- lenszoom（镜头变焦）
- linkagezoom（联动变焦）
- map（地图）
- ranging（测距）
- remotelocation（遥控器定位）
- remotestatusbar（遥控器状态栏）
- statusbar（飞行器状态栏）
- temp（温度显示）

### 2. 典型控件用法举例

#### 2.1 姿态控件（AttitudeWidget）
```kotlin
val attitudeWidget = findViewById<AttitudeWidget>(R.id.attitude_widget)
attitudeWidget.setAttitude(pitch, roll, yaw)
```
- **可定制项**：显示样式、刷新频率、单位等

#### 2.2 地图控件（MapWidget）
```kotlin
val mapWidget = MapWidget(this)
container.addView(mapWidget, MATCH_PARENT, MATCH_PARENT)
mapWidget.setDroneLocation(lat, lng)
mapWidget.setHomePoint(homeLat, homeLng)
```
- **可定制项**：地图类型、航线显示、禁飞区、返航点等

#### 2.3 相机变焦控件（LensZoomWidget）
```kotlin
val zoomWidget = findViewById<LensZoomWidget>(R.id.zoom_widget)
zoomWidget.setZoomLevel(level)
```
- **可定制项**：最大/最小变焦、UI样式、滑动手势等

#### 2.4 状态栏控件（StatusBarWidget）
```kotlin
val statusBar = findViewById<StatusBarWidget>(R.id.status_bar)
statusBar.setBatteryLevel(level)
statusBar.setFlightMode(mode)
```
- **可定制项**：显示内容、图标、颜色、警告提示等

#### 2.5 云台俯仰控件（GimbalPitchWidget）
```kotlin
val gimbalWidget = findViewById<GimbalPitchWidget>(R.id.gimbal_widget)
gimbalWidget.setPitchAngle(angle)
```
- **可定制项**：角度范围、步进、动画效果等

---

## 三、主Activity（UxsdkDemoActivity）集成说明

### 1. 主要职责
- 作为APP主界面入口，负责初始化和集成所有UI控件。
- 负责主界面各控件的布局、显示/隐藏、交互逻辑。
- 负责分屏/全屏切换、功能栏样式切换、权限申请等全局功能。
- 通过事件总线、ViewModel等机制，实时响应无人机状态变化和用户操作。

### 2. 典型初始化流程
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkAndRequestAllFilesPermission()
    initView() // 初始化各控件
    // ... 其他初始化 ...
}
```
- **initView()**：动态添加MapWidget、设置状态栏、功能栏、悬浮球等控件的显示与交互。
- **分屏/全屏切换**：通过ScreenStateManager和动画工具AnimateUtil，动态调整控件布局和可见性。
- **功能栏样式切换**：支持工具栏/悬浮球两种模式，适应不同用户习惯。
- **权限处理**：自动检测并请求存储权限，兼容Android 11+。

---

## 四、设置模块功能详解

![20250612-121332](.\20250612-121332.jpg)

### 1. 设置页面主要功能

- **飞行参数设置**：最大高度、最大距离、返航高度、失控返航等
- **相机参数设置**：分辨率、曝光、白平衡、对焦、SD卡管理等
- **遥控器设置**：摇杆模式、灵敏度、校准、按键自定义等
- **图传与图像设置**：分辨率、码率、直方图、色彩风格等
- **系统设置**：语言、单位、固件升级、恢复出厂、关于设备等
- **安全与隐私**：数据加密、日志导出、隐私政策等

### 2. 设置模块源码定制说明
- 每个设置项对应一个Fragment或自定义View，位于`module_setting`下。
- 可自由增删设置项、修改UI样式、扩展功能、支持多语言。

---

## 五、UI主题与样式定制

- 所有控件支持主题色、字体、圆角、阴影等样式自定义
- 可通过`res/values/colors.xml`、`styles.xml`统一配置
- 支持夜间模式、深色模式

---

## 六、开发与调试建议

- **热重载**：建议用Android Studio的Layout Inspector和实时预览
- **模块化开发**：每个widget/setting都可单独开发、测试、复用
- **与SDK解耦**：UI层与Autel MSDK通过接口通信，便于后期升级和维护
- **单元测试/自动化测试**：建议为关键设置项和控件编写测试用例

---

## 七、常见定制场景举例

- **增加自定义飞行模式**：在设置模块添加新选项，并与SDK对接
- **更换主界面布局**：自由组合/隐藏/重排widget控件
- **品牌化UI**：更换LOGO、主色调、字体、动画
- **增加安全提示**：在状态栏或设置项中增加自定义警告/弹窗

---

## 八、参考与支持

- **Autel MSDK官方文档**：了解底层接口和数据结构  [Autel Developer Technologies](https://developer.autelrobotics.com/)
- **Android官方UI开发文档**：掌握自定义View、主题、动画等高级用法
- **本项目源码**：所有UI和设置模块均可二次开发，支持深度定制

