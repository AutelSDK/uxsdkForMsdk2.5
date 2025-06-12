

# Autel Drone Tablet App – UI Component Development Guide

------

## 1. Project Structure & Module Overview

- **module_widget**: The core UI component library for the drone flight interface. Includes attitude indicator, map, camera, gimbal, status bar, and all flight-related widgets.
- **module_setting**: Settings UI for the drone, including flight parameters, camera settings, remote controller, system settings, and more. Fully customizable at source code level.
- **uxddemo (Main Project)**: Main entry Activity (e.g., `UxsdkDemoActivity`) responsible for integrating and managing all widgets and handling main interface interaction.

------

## 2. Main Interface Widget Components Overview & Usage

![20250612-121325](.\20250612-121325.jpg)

### 1. Component Directory Structure

- **attitude**: Attitude indicator / flight instrument
- **cancellanding**: Cancel landing button
- **codectoolright**: Right-side toolbar
- **colouratla**: Color histogram display
- **compass**: Compass widget
- **focusAndZoom**: Focus & zoom controls
- **fullScreen**: Fullscreen toggle
- **gimbalpitch**: Gimbal pitch control
- **histogram**: Histogram display
- **lenszoom**: Lens zoom slider
- **linkagezoom**: Linked zoom control
- **map**: Map widget
- **ranging**: Rangefinder / distance display
- **remotelocation**: Remote controller GPS position
- **remotestatusbar**: Remote controller status bar
- **statusbar**: Aircraft status bar
- **temp**: Temperature display

### 2. Example Widget Usage

#### 2.1 Attitude Widget

```kotlin
val attitudeWidget = findViewById<AttitudeWidget>(R.id.attitude_widget)
attitudeWidget.setAttitude(pitch, roll, yaw)
```

- **Customizable Options**: Display style, update rate, units, etc.

#### 2.2 Map Widget

```kotlin
val mapWidget = MapWidget(this)
container.addView(mapWidget, MATCH_PARENT, MATCH_PARENT)
mapWidget.setDroneLocation(lat, lng)
mapWidget.setHomePoint(homeLat, homeLng)
```

- **Customizable Options**: Map type, flight path display, no-fly zones, return-to-home point, etc.

#### 2.3 Lens Zoom Widget

```kotlin
val zoomWidget = findViewById<LensZoomWidget>(R.id.zoom_widget)
zoomWidget.setZoomLevel(level)
```

- **Customizable Options**: Zoom range, UI style, gesture control, etc.

#### 2.4 Status Bar Widget

```kotlin
val statusBar = findViewById<StatusBarWidget>(R.id.status_bar)
statusBar.setBatteryLevel(level)
statusBar.setFlightMode(mode)
```

- **Customizable Options**: Display content, icons, colors, alerts, etc.

#### 2.5 Gimbal Pitch Widget

```kotlin
val gimbalWidget = findViewById<GimbalPitchWidget>(R.id.gimbal_widget)
gimbalWidget.setPitchAngle(angle)
```

- **Customizable Options**: Angle range, step size, animation, etc.

------

## 3. Main Activity (UxsdkDemoActivity) Integration Guide

### 1. Primary Responsibilities

- Acts as the main entry point for the app, initializing and integrating all UI widgets.
- Manages layout, visibility, and interaction of widgets.
- Handles screen split/fullscreen toggling, toolbar style switching, and global functions like permissions.
- Listens to drone status updates and user interactions via event bus or ViewModel mechanisms.

### 2. Typical Initialization Flow

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkAndRequestAllFilesPermission()
    initView() // Initialize UI widgets
    // ... other initializations ...
}
```

- **initView()**: Dynamically adds `MapWidget`, configures status bar, toolbar, and floating buttons.
- **Split/Fullscreen Mode**: Use `ScreenStateManager` and `AnimateUtil` to dynamically change layout and visibility.
- **Toolbar Style Switching**: Supports both traditional toolbar and floating button styles to suit user preferences.
- **Permissions**: Automatically checks and requests storage permissions, compatible with Android 11+.

------

## 4. Settings Module Feature Overview

![20250612-121332](.\20250612-121332.jpg)

### 1. Main Settings Features

- **Flight Parameters**: Max altitude, max distance, return height, failsafe return, etc.
- **Camera Settings**: Resolution, exposure, white balance, focus, SD card management, etc.
- **Remote Controller**: Stick mode, sensitivity, calibration, button mapping, etc.
- **Video Transmission & Image Settings**: Resolution, bitrate, histogram, color profile, etc.
- **System Settings**: Language, units, firmware update, factory reset, about device, etc.
- **Privacy & Security**: Data encryption, log export, privacy policy, etc.

### 2. Customizing Settings Module

- Each setting item corresponds to a Fragment or custom View under `module_setting`.
- Freely add/remove settings, change UI styles, extend functionality, and support localization.

------

## 5. UI Theme & Style Customization

- All widgets support theme color, font, corner radius, shadows, and other styling options.
- Unified configuration through `res/values/colors.xml` and `styles.xml`.
- Supports dark mode and night themes.

------

## 6. Development & Debugging Recommendations

- **Hot Reload**: Use Android Studio’s Layout Inspector and Live Preview for real-time editing.
- **Modular Development**: Each widget/setting is independently developable, testable, and reusable.
- **MSDK Decoupling**: UI layer communicates with Autel MSDK via interfaces for easier upgrades and maintenance.
- **Unit/Automation Testing**: Recommended for critical settings and widget functionality.

------

## 7. Common Customization Scenarios

- **Add Custom Flight Modes**: Add new options in the settings module and integrate with SDK.
- **Redesign Main Layout**: Rearrange, hide, or combine widgets as needed.
- **Branding Customization**: Replace logo, theme color, fonts, and animations.
- **Add Safety Prompts**: Insert custom warnings/popups in status bar or settings.

------

## 8. References & Support

- **Autel MSDK Official Docs**: Learn about low-level APIs and data structures
   [Autel Developer Technologies](https://developer.autelrobotics.com/)
- **Android Official UI Docs**: Master custom views, themes, animations, and more
- **Project Source Code**: All UI and settings modules are fully customizable for deep integration

