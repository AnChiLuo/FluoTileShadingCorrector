# FluoTileShadingCorrector
The grid-like shading is a common artifact in whole slide images(WSIs). It may arise from vignetting, uneven illumination, sample surface irregularities. Affected by this artifact, tile images exhibit shading, where signal intensity gradually decays from the center toward the edges. When these tiles are stitched together, the resulting WSI displays brightness discontinuities and grid-like patterns, which complicates downstream image analysis tasks.Many prospective shading correction methods have been widely used to address this artifact. These methods rely on a high-quality reference image acquired from an empty field of view. However, obtaining such a reference image—entirely free of dust, residual fluorescence, or staining artifacts is often unrealistic, especially when working with fluorescence-labeled tissue sections. Furthermore, prospective shading correction is only applicable to camera-based imaging system, where shading patterns are spatially consistent across the tiles. In contrast, laser scanning confocal microscopy acquires images point by point, introducing dynamic intensity variations that vary not only between tiles but also within each tile.  As a result, static reference-based correction is generally ineffective for confocal datasets. performa
To address this limitation, we incorporated [BaSiC] (https://imagej.net/plugins/basic), a well-established retrospective shading method that estimates background variation directly from image data without requiring a reference images. BaSiC is suitable for fluorescence microscopy and has shown robust performance across diverse datasets. However, BaSiC requires the input images to be structured in a specific format and manually prepared, which can be a barrier for routine use. Another difficulty is to stitch the corrected tiles, we integrate with the **Grid/Collection Stitching** plugin to run seamless image reconstruction. To minimize manual effort, we extract tile coordinates directly from image metadata enabling users to run the full correction and stitching workfolow on the raw input files without manually export or rearrange the tiles beforehand.

## Feature
This tool provides:
- Automatic reorganization of tile images into BaSiC-compatible format
- Metadata-based extraction of tile coordinates for stitching
- Output reconstruction using Grid/Collection Stitching in Fiji

## Installation
This tool is implemented as a Groovy script designed to run in  [Fiji (ImageJ)](https://fiji.sc), with dependencies on the following components:

### Requirements
- [Fiji (ImageJ)](https://fiji.sc) -recommended to use the pre-packaged Fiji distribution
- Bio-Formats plugin (bundled with Fiji)
- [BaSiC plugin](https://imagej.net/plugins/basic) (available via Fiji update site)
- [Grid/Collection Stitching plugin](https://imagej.net/plugins/grid-collection-stitching) (bundled with Fiji)

### Setup Instructions
1. Download or clone this repository.
2. Open Fiji.
3. Open the script:
   - `File > Open` and select `FluoTileShadingCorrector.groovy`
4. Run the script:
   - `Run > Run` or use `Ctrl+R` (⌘+R on macOS)
Once 







