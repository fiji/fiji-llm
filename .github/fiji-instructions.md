# Fiji Usage Guide

> **Related files**: Start with `scijava-foundation.md` for shared concepts. See `imagej2.md` for the core application architecture. For Python integration, see `pyimagej.md`.

## Overview

Fiji is a "batteries-included" distribution of ImageJ for life sciences—a scientific image processing application built on Java. Fiji = ImageJ + SciJava ecosystem + 100+ plugin dependencies.

**Key principle**: Fiji bundles ImageJ2 with a comprehensive set of plugins for scientific image analysis, particularly focused on life sciences workflows.

## Architecture

### Core Components
- **Main class**: `sc.fiji.Main` (launcher wrapping `net.imagej.ImageJ`)
- **Plugin architecture**: Uses SciJava's plugin discovery system
- **Plugin loading**: Plugins are dynamically discovered from JAR files at runtime

### Launcher System
Fiji uses **Jaunch** launcher with the following configuration:
- Config: `config/jaunch/fiji.toml` - defines CLI options and launch modes
- Python bridge: `config/jaunch/fiji.py` - enables `--python` mode via PyImageJ
- Environment: `config/environment.yml` - Conda environment for Python integration

### Directory Structure
```
config/              # Jaunch launcher config + conda environment
plugins/             # ImageJ 1.x style plugins (macros, scripts)
macros/              # ImageJ macros and toolsets
scripts/             # Structured scripts (File/, Image/, Plugins/)
luts/                # Color lookup tables
jars/                # Java libraries and runtime dependencies
```

## Running Fiji

### Command Line Usage
```bash
# Standard launch
fiji

# Python mode (requires PyImageJ)
fiji --python

# Headless mode (no GUI)
fiji --headless

# Debug mode
fiji --jdb
```

### Running from Java/IDE
Execute `sc.fiji.Main` with optional system property:
```bash
-Dplugins.dir=/path/to/Fiji.app
```
This allows access to the full plugin set when running from an IDE.

## Python Integration

Fiji supports **dual-mode launch** (Java or Python):
- Use `--python` flag to activate Python mode via `config/jaunch/fiji.py`
- Requires **PyImageJ** (`pyimagej>=1.7.0`) to wrap ImageJ in Python
- Python environment defined in `config/environment.yml` (includes napari, scikit-image, ndv)
- **Appose** provides bidirectional Python↔Java communication

## Plugin System

### Plugin Discovery
- Plugins use `@Plugin` annotations for automatic discovery
- Plugins are loaded from JAR files in `jars/` and `plugins/` directories
- Uses SciJava's dependency injection via `@Parameter` annotations

### Plugin Locations
- **Runtime plugins**: Located in `jars/` directory
- **ImageJ 1.x plugins**: Located in `plugins/` directory
- **Platform-specific natives**: Located in `jars/{platform}/` subdirectories (e.g., `jars/macosx/`, `jars/win64/`)

## Key Dependencies

### Core Libraries
- **ImageJ**: `net.imagej:imagej` (ImageJ2) + `net.imagej:ij` (ImageJ 1.x)
- **SciJava**: `org.scijava:scijava-common` - plugin framework, event system
- **ImgLib2**: Core image processing library
- **Bio-Formats**: Multi-format microscopy image I/O
- **JOGL**: Java 3D rendering (platform-specific natives)

### Dependency Management
- Plugins inherit versions from `pom-scijava` parent POM (centrally managed)
- Runtime plugins use `<scope>runtime</scope>` (dynamically loaded, not compile-time)

## Utility Scripts

Useful scripts in `bin/` directory:
- `bin/which-jar-has-plugin.py` - Find which JAR contains a plugin class
- `bin/find-jar-for-class.bsh` - Locate JAR for a given class

