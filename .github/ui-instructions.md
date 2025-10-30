# SciJava and ImageJ Swing UI - Usage Guide

## Overview
This guide covers using the **SciJava UI Swing** and **ImageJ UI Swing** libraries for building Java Swing-based user interfaces within the SciJava/ImageJ ecosystem. These libraries provide UI components, widgets, viewers, and image display capabilities using Java Swing, JHotDraw, and the SciJava plugin framework.

**Key Technologies:**
- Java Swing for UI components
- SciJava plugin framework for component discovery and dependency injection
- JHotDraw for interactive drawing/editing of image overlays
- MigLayout for component layouts

## SciJava Plugin System

### Plugin Discovery and Dependency Injection
All components use `@Plugin` annotations for automatic discovery:
- **Plugin Discovery**: Components marked with `@Plugin` are auto-discovered at runtime
- **Dependency Injection**: Use `@Parameter` annotation for automatic service injection
- **Context Management**: All components operate within a SciJava `Context` which manages plugin lifecycle

Example plugin pattern:
```java
@Plugin(type = InputWidget.class)
public class SwingNumberWidget extends SwingInputWidget<Number> {
    @Parameter
    private ThreadService threadService;
    
    @Parameter
    private ModuleService moduleService;
}
```

### Creating Commands
```java
@Plugin(type = Command.class, menuPath = "Plugins>Your Menu>Command Name")
public class YourCommand implements Command {
    @Parameter
    private UIService uiService;
    
    @Override
    public void run() {
        // Implementation
    }
}
```

## UI Architecture

### Two UI Modes
- **SDI** (Single Document Interface): `SwingSDIUI` - default, each window independent
- **MDI** (Multiple Document Interface): `SwingMdiUI` - uses internal frames within parent window

**Core Component Hierarchy:**
```
AbstractSwingUI (base for both SDI/MDI)
├── SwingApplicationFrame (main window)
├── SwingToolBar (tool buttons)
├── SwingStatusBar (bottom status)
└── SwingConsolePane (console/logging UI)
```

### Widget System
Input widgets extend `SwingInputWidget<T>`:
- **MigLayout**: Standard pattern `new MigLayout("fillx,ins 3 0 3 0", "[fill,grow|pref]")`
- **Type Safety**: Generic type `<T>` matches the input value type
- **Available widgets**: `SwingNumberWidget`, `SwingColorWidget`, `SwingDateWidget`, `SwingFileWidget`, `SwingTextWidget`, etc.

### Display Viewers
Implement `DisplayViewer` interface with `@Plugin(type = DisplayViewer.class)`:
- `SwingPlotDisplayViewer` - plotting/charting
- `SwingTableDisplayViewer` - tabular data
- `SwingTextDisplayViewer` - text display

### Event Dispatchers
Register AWT event dispatchers on display windows:
```java
new AWTInputEventDispatcher(display).register(displayWindow, true, false);
new AWTWindowEventDispatcher(display).register(displayWindow);
new AWTDropTargetEventDispatcher(display, eventService);
```

## ImageJ Overlay System

### Adapter-Based Architecture
ImageJ bridges domain objects with GUI representations using **bidirectional adapters**:

```
ImageJ Overlay ←→ JHotDrawAdapter ←→ JHotDraw Figure
```

- **Overlays** (`net.imagej.overlay.*`): Domain objects representing ROIs (lines, rectangles, ellipses, etc.)
- **Figures** (JHotDraw): Swing components for interactive drawing/editing
- **Adapters** (`*JHotDrawAdapter.java`): Synchronize overlays and figures bidirectionally

**Key Service:** `JHotDrawService` discovers and manages all adapters.

### Image Display Architecture
Image display supports both SDI and MDI modes via:
- **SDI**: `SwingSdiImageDisplayViewer` - each image in separate `JFrame`
- **MDI**: `SwingMdiImageDisplayViewer` - images in `JInternalFrame`s

**Component hierarchy:**
```
DisplayViewer → SwingImageDisplayPanel → JHotDrawImageCanvas
                     ↓                         ↓
              (sliders, color bar)      (JHotDraw DrawingView)
```

### Using Overlay Adapters
Example adapter pattern:
```java
@Plugin(type = JHotDrawAdapter.class, priority = SwingLineTool.PRIORITY)
public class LineJHotDrawAdapter extends AbstractJHotDrawAdapter<LineOverlay, LineFigure> {
    @Parameter
    private OverlayService overlayService;  // Auto-injected
}
```

Adapters provide bidirectional synchronization:
- `updateFigure(OverlayView, Figure)` - sync overlay → figure
- `updateOverlay(Figure, OverlayView)` - sync figure → overlay

## Common Usage Patterns

### Look & Feel Management
- Service: `SwingLookAndFeelService` handles theme initialization
- FlatLaf themes: `FlatLightLaf`, `FlatDarkLaf`, `FlatIntelliJLaf`, `FlatDarculaLaf`
- Must call `lafService.initLookAndFeel()` before creating Swing components

### Menu System
Shadow menu pattern for creating menus:
- `SwingJMenuBarCreator` - creates JMenuBar from ShadowMenu
- `SwingJPopupMenuCreator` - creates context menus
- `AbstractSwingMenuCreator` - base menu creation logic

### Console & Logging
`SwingConsolePane` provides two tabs:
- **Console tab**: `ConsolePanel` for general output
- **Log tab**: `LoggingPanel` for structured logging with filtering

### Event-Driven Updates
Components communicate via SciJava's event bus:
```java
@EventHandler
protected void onEvent(final SomeEvent event) {
    // Handle event
}
```

Publish events:
```java
eventService.publish(new SomeEvent(...));
```

Key events: `FigureCreatedEvent`, `AxisPositionEvent`, `LUTsChangedEvent`, `PanZoomEvent`

### Threading Considerations
- **EDT Safety**: All Swing UI updates must run on Event Dispatch Thread
- Use `threadService.invoke()` for Swing operations
- Use `StaticSwingUtils.invokeOnEDT()` when updating UI from background threads
- `JFileChooser` must run on EDT to avoid deadlocks (especially macOS)

## Dependencies

### External Libraries
- **MigLayout** (`com.miglayout:miglayout-swing`) - layout manager
- **JFreeChart** (`org.jfree:jfreechart`) - charting components
- **FlatLaf** (`com.formdev:flatlaf`) - modern look and feel themes
- **JHotDraw** (`org.jhotdraw:jhotdraw`) - interactive drawing framework

### SciJava/ImageJ Stack
- `org.scijava:scijava-common` - plugin framework, event system, contexts
- `org.scijava:scijava-ui-swing` - base Swing UI components
- `net.imagej:imagej-common` - core ImageJ services and overlays
- `net.imglib2:imglib2` - image data structures

## Common Pitfalls

1. **Forgetting `@Plugin` annotation**: Components won't be discovered
2. **Wrong plugin type**: Use correct type (e.g., `type = JHotDrawAdapter.class` not `type = Plugin.class`)
3. **Not calling `setContext()`**: Required before using `@Parameter` injection in manually instantiated objects
4. **Modifying UI off EDT**: Leads to race conditions and visual artifacts
5. **Breaking bidirectional sync**: Both `updateFigure()` and `updateOverlay()` must maintain consistency in overlay adapters

