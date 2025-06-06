# FluoTileShadingCorrector
The grid-like shading artifact is a common problem in whole slide images(WSIs), arising from uneven illumination or sample surface irregularities. Affected by this artifact, WSI displays brightness discontinuities and grid-like patterns, which not only degrade visual quality but slo ancomplicates downstream image analysis tasks . Many prospective shading correction methods have been widely used to address this artifact. These methods rely on a high-quality reference image acquired from an empty field of view. However, obtaining such a reference image—entirely free of dust, residual fluorescence, or staining artifacts is often unrealistic, especially when working with fluorescence-labeled tissue sections. Furthermore, prospective shading correction is only applicable to camera-based imaging system, where shading patterns are spatially consistent across the tiles. In contrast, laser scanning confocal microscopy acquires images point by point, introducing dynamic intensity variations that vary between tiles and within each tile.  As a result, static reference-based correction is generally ineffective for confocal datasets. performa
To address this limitation, we incorporated [BaSiC] (https://imagej.net/plugins/basic), a well-established retrospective shading method that estimates background variation directly from image data without requiring a reference image. BaSiC is suitable for fluorescence microscopy and has shown robust performance across diverse datasets. However, BaSiC requires the input images to be structured in a specific format and manually prepared, which can be a barrier for routine use. Another difficulty is to stitch the corrected tiles, we integrate with the **Grid/Collection Stitching** plugin to run seamless image reconstruction. To minimize manual effort, we extract tile coordinates directly from image metadata, enabling users to run the full correction and stitching workflow on the raw input files without manually exporting or rearranging the tiles beforehand.

## Feature
This tool provides:
- Supports multi-tile **Zeiss CZI** image import via Bio-Formats
- Automatic reorganization of tile images into BaSiC-compatible format
- Applies **BaSiC flat-field and dark-field correction** tile-wise
- Automatically creates compatible XML for **BigStitcher**
- Removes temporary files to keep workspace clean

## ⚙️ Installation
This tool is implemented as a Groovy script designed to run in  [Fiji (ImageJ)](https://fiji.sc), with dependencies on the following components:

### Requirements
-Fiji with the following plugins installed:
- [Bio-Formats](https://imagej.net/plugins/bio-formats) (bundled with Fiji)
- [BaSiC plugin](https://imagej.net/plugins/basic) (available via Fiji update site)
- [BigStitcher plugin]((https://imagej.net/plugins/bigstitcher/) (available via Fiji update site)

### Setup Instructions
1. Download or clone this repository.
2. Open Fiji.
3. Open the script:
   - `File > Open` and select `FluoTileShadingCorrector.groovy`
4. Run the script:
   - `Run > Run` or use `Ctrl+R` (⌘+R on macOS)

Once launchched, a dialog will prompt for a `.czi` file and guide you through the shading correction and stitching process.

## Step-by-step demo
 1. **Luanch the script** in Fiji.
    A dialog window will appear prompting you to(see Fig. 1):
    - **Select file (.czi)** : Browse and choose your raw tile image file.
    - **Reference channel for stitching** : Enter the index of the channel to be used for sititching consistency.
    - **Mode for Runing**:
      - `Automatic` : Performs BaSiC correction and  Grid/Collection Stitching in one go.
      - `Manual` : Pause after stitching to allow user inspection before proceeding to the next channel.
        
      ![Parameter Dialog](https://github.com/user-attachments/assets/2aca991f-b829-4e1e-aca6-c390072725d6)

      *Fig. 1: Dialog window for selecting input file, reference channel, and run mode.*

 2. The script will generate three output folders:
    - `BasicTemp` : restructured raw images for BaSiC.
    - `StitchTemp` : Per-tile images corrected by BaSiC.
    -  `Results` : Final fused images
      These folders are created under a parent directory automatically named after the source file.
 3. When finished, only the final fused images in the Results folder are retained.
 4. If  `Manual` mode is selected, a dialog will appear after stitching to allow the user to inspect the fused image (see Fig. 2). You can zoom and pan the image to check the stitching alignment. Options are provided to either proceed to the next channel,rerun BaSiC + Stitching for the same channel, or cancel the process.

![Manual review dialog](https://github.com/user-attachments/assets/358ba86e-dc64-46fa-96b3-904759ed1106)

*Fig. 2: Manual review window. Users can zoom to inspect stitching results and choose whether to proceed.*

## 🔍 Notes

- 📦 Input must be **unstitched** CZI files with positional metadata.
- ⛔ **Avoid spaces** in filenames and paths.
- ⚠️ Final stitched intensity values are **not calibrated for quantification** due to brightness normalization.
    
## Example result(before/after)
## Citation
1. Peng, T., Thorn, K., Schroeder, T. et al. A BaSiC tool for background and shading correction of optical microscopy images. Nat Commun 8, 14836 (2017). https://doi.org/10.1038/ncomms14836
2. Hörl, D., Rojas Rusak, F., Preusser, F. et al. BigStitcher: reconstructing high-resolution image datasets of cleared and expanded samples. Nat Methods 16, 870–874 (2019). https://doi.org/10.1038/s41592-019-0501-0
## License
Apache License 2.0








