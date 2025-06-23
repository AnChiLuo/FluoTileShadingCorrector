# FluoTileShadingCorrector
The grid-like shading artifact is a common problem in whole slide images(WSIs), arising from uneven illumination or sample surface irregularities. Affected by this artifact, WSI displays brightness discontinuities and grid-like patterns, which not only degrade visual quality but also complicates downstream image analysis tasks .  Prospective shading methods have been widely used on camera-based imaging systems, but they require a clean, empty field reference image—which is often impractical for fluorescence-labeled tissue sections—and generally fail on laser‐scanning confocal microscopes because the shading pattern varies dynamically per tile. 
To overcome these challenges, we incorporated [BaSiC](https://imagej.net/plugins/basic)—a robust retrospective shading method that estimates background variation directly from the data itself, without requiring any reference image. 

However, using BaSiC in practice poses two technical challenges:
   1. Input formatting: BaSiC expects a well-structured image stack, requiring users to manually prepare and rearrange the output stack for the downstream stitch process.
   2. Post-correction stitching: Users must manually stitch corrected tiles to reconstruct a seamless mosaic image.
      
To streamline this workflow, we present FluoTileShadingCorrector, an ImageJ tool that integrates:
   -	Tile-aware BaSiC flat-field correction (no reference image required),
   -	Metadata-based tile positioning extracted directly from OME-XML,
   -	Automatic dataset.xml generation for BigStitcher compatibility,
   -	Seamless tile stitching and fusion, either in batch or interactive mode.

With our tool, you can perform flat-field correction and generate a fused mosaic directly from raw CZI or Lif files—no manual data prep, custom XML editing, or fragmented pipeline required. It greatly streamlines the entire process, making it accessible, reproducible, and ready for batch processing. 

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
    - **Select file (.czi or .lif)** : Browse and choose your raw tile image file.
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
 4. If  `Manual` mode is selected, a dialog will appear after stitching to allow the user to inspect the fused image (see Fig. 2). You can zoom and pan the image to check the stitching alignment. Options are provided to either proceed to the next channel, reselecting reference channel for Stitching, or cancel the process.


![Dialog_Manual](https://github.com/user-attachments/assets/f3947631-ab5b-4f59-88eb-dcb70a2c50ac)




*Fig. 2: Manual review window. Users can zoom to inspect stitching results and choose whether to proceed.*

## 🔍 Notes

- 📦 Input must be **unstitched** CZI or Lif files with positional metadata.
-  👉 The file should contain **a single tile-scan dataset** from **one acquisition** only. Batch processing of multiple files or multiple scenes is not supported.
- ⛔ **Avoid spaces** in filenames and paths.
- ⚠️ Final stitched intensity values are **not calibrated for quantification** due to brightness normalization.
    
## Example result(before/after)
![Result](https://github.com/user-attachments/assets/8946135b-9c9c-4517-b900-ea1494a73adb)
*Demo image kindly provided by Dr. Lih-Chu Chiou's lab (used with permission).*


## Citation
1. Peng, T., Thorn, K., Schroeder, T. et al. A BaSiC tool for background and shading correction of optical microscopy images. Nat Commun 8, 14836 (2017). https://doi.org/10.1038/ncomms14836
2. Hörl, D., Rojas Rusak, F., Preusser, F. et al. BigStitcher: reconstructing high-resolution image datasets of cleared and expanded samples. Nat Methods 16, 870–874 (2019). https://doi.org/10.1038/s41592-019-0501-0
## License
This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
### Included/Invoked Dependencies

This tool **invokes but does not bundle or modify** the following third-party ImageJ/Fiji plugins:

- **[BigStitcher](https://imagej.net/plugins/bigstitcher)**  
  Licensed under the GNU General Public License v3.0 (GPL-3.0)  
  → Used for tile registration, global optimization, and image fusion.

- **[BaSiC](https://imagej.net/plugins/basic)**  
  Licensed under the GNU General Public License v3.0 (GPL-3.0)  
  → Used for flat-field and dark-field correction on image tiles.









