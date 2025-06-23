 // Macro Name: FluoTileShadingCorrector
// Version: v26.0
// Author: An-Chi, Luo
// Date: [2025-06-23]
// Note: use BigStitcher to stitch Tiles and build a xml creator (Clean code), Lif file is accetable, Error log for no overlap
//
// Description:
// 1. Load a raw-tile CZI or Lif image via Bio-formats without auto-stitching, splitting it into individual tile stacks.
// 2. To eliminate discontinuities across tiles, apply "BaSiC" for the flat‐field and dark‐field correction on each tile stack
//    and save corrected outputs to the "BasicTemp" folder.
// 3. Assemble the corrected tiles via "BigStitcher".
// 4. To support "BigStitcher", parse the OME-XMl metadata to extract tile coordinates and generate a temporary Xml file.
// 5. Supports two run modes:
//     • Automatic: apply channel‐specific LUT and save results without interruption.
//     • Manual: pause after each channel’s stitching for user inspection, allowing Continue/Redo/Cancel.
// 6. Result images are saved to "Result" file
//
//Usage:
// 1. Run the macro, then select the CZI file, reference channel, and run mode from the dialog.
// 2. Output folders ("BasicTemp", "StitchTemp" and "Results") will be created inside a parent folder named after the source file:
// 3. When finished, only the final fused images in Results folder are retained;
//
//Notes:
//  • Only unstitched CZI and Lif images are supported!!
//  • Avoid space in file names.
//  • Requires Bio‐Formats, BaSiC and BigStitcher plugins to be installed.

import ij.IJ
import ij.WindowManager
import ij.gui.GenericDialog
import ij.Prefs
import ij.ImageStack
import ij.process.LUT
import loci.plugins.util.LociPrefs
import loci.plugins.in.ImportProcess
import ij.plugin.ChannelSplitter
import ij.gui.WaitForUserDialog
import ij.gui.YesNoCancelDialog
import ij.ImagePlus
import loci.plugins.BF
import loci.plugins.in.ImporterOptions
import loci.plugins.util.ImageProcessorReader
import ome.xml.meta.OMEXMLMetadata
import java.awt.Color as AwtColor
import javax.swing.*
import java.awt.*
import groovy.xml.MarkupBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import java.util.List
import java.util.Map




//// -- 0. Set the configuration of Bio-Formats importer --////
Prefs.set(LociPrefs.PREF_CZI_AUTOSTITCH, false)

//// -- 1. Ask the File path and set the options for importer --////
def para = new GenericDialog("Set parameter for running")
para.addFileField("Select file(czi):", null)
para.addNumericField("Reference channel for stitching:", 1, 0)
para.addChoice("Mode for Running:", ["Automatic", "Manual"] as String[], "Automatic")
para.showDialog()
/// -- Fetch the parameters from dialog -- ///
def filePath = para.getNextString()
def refChan = para.getNextNumber()
def mode = para.getNextChoice()
/// -- Create File for saving Images -- ///
def pathParent = new File(filePath).getParent()
def fileName = new File(filePath).getName()
fileName = fileName.split("\\.")[0]
def outputPath = new File(pathParent, fileName)
if( !outputPath.exists()) outputPath.mkdirs()
def TempList = ["BasicTemp", "StitchTemp", "Results"]
TempList.each {name ->
    def folder = new File(outputPath, name)
    if ( !folder.exists()) folder.mkdirs()
}

def basicTemp  = new File (outputPath, "BasicTemp")
def StitchTemp = new File (outputPath, "StitchTemp")
def Result = new File (outputPath, "Results")

/// -- Set the options -- ///
def opts = new ImporterOptions()
opts.setId(filePath)          // CZI path
opts.setQuiet(true)           // close progressinfo
opts.setSplitChannels(false)
opts.setOpenAllSeries(true)   // set to read all series（scene／tile）
opts.setStitchTiles(false)    // close AutoStitch
opts.parseArg("split_tiles")

//// —— 2. Run ImportProcess —— ////
def process = new ImportProcess(opts) // execute() goes through the READER → METADATA → STACK phase，parising out reader and metadata
if (!process.execute()) {
    IJ.log("ImportProcess ERROR！")
    return
}

//// —— 3. Fetch OME-XML Metadata —— ////
OMEXMLMetadata meta = process.getOMEMetadata() as OMEXMLMetadata

//// —— 4. Obtain he reader —— ////
ImageProcessorReader ipReader = process.getReader()

//// —— 5. Extract some info from OME  —— ////
/// -- Create a AwtColor function  for converting OME_color object to Color object
AwtColor AwtColorconvert(omeColor){
    r = omeColor.getRed()
    g = omeColor.getGreen()
    b = omeColor.getBlue()
    a = omeColor.getAlpha()
    AwtColor awt = new AwtColor(r, g, b ,a)
    return awt
}
/// -- Get channel information(OME_color obj), channel name and Bit depth -- ////
int bitDepth = meta.getPixelsSignificantBits(0).getValue()
int nChannels = meta.getChannelCount(1)
def channelData = [:]
(1..nChannels).each { c ->
    channelData ["Corrected_C${c}.tiff"] = [
            label : meta.getChannelName(1, c-1).replaceAll(/[\/\\ ]/, "_"),
            color : AwtColorconvert(meta.getChannelColor(1, c-1)),
    ]
}
/// --Get tile number , the PositionX and PositionY
int nTiles = process.getSeriesCount()
if(nTiles ==1){
    IJ.error("\"Input Error\", \"Only one tile detected.\\nPlease select a multi-tile image.\"")
}
/// —— Fetch the PositionX and PositionY and convert the originPoint from absoluteF to referenceF —— ///
def absoluteF =[]
def tilePosition = []
for (int i = 0; i < nTiles; i++) {
    def qx = meta.getPlanePositionX(i, 1)
    def qy = meta.getPlanePositionY(i, 1)
    if(qx.unit().getSymbol() != "reference frame"){
        absoluteF.add(i)
    }
    qx =qx.value()
    qy =qy.value()
    tilePosition.add( [ qx, qy ] )
}
/// —— Convert absolute coordinates from um to pixel(for widefield microscopy) —— ///
if (absoluteF.size() >1 && absoluteF.size() == nTiles){
    println "All of tile position is absolute frame "
	/// —— check the unit before convert  —— ///
    def pixelScaleX = meta.getPixelsPhysicalSizeX(0).value().doubleValue()
    def pixelScaleY = meta.getPixelsPhysicalSizeY(0).value().doubleValue()
    def positionUnit = meta.getPlanePositionX(0, 1).unit().getSymbol()
	def micro = new String([0x00B5] as int[], 0 ,1)
	def microM = micro +"m"
    Map <String, Number > unitConversion =["m":1000000 , "mm": 1000, "nm" : 0.001, "cm": 10000, (microM):1.0] 
    tilePosition.eachWithIndex{ p,i ->
        tilePosition[i][0] = Math.round(p[0] / pixelScaleX *10 * unitConversion[positionUnit])/10
        tilePosition[i][1] = Math.round(p[1] / pixelScaleY *10 * unitConversion[positionUnit])/10
    }
}

if (absoluteF.size() >1 && absoluteF.size() != nTiles){
    println "MetadataError!! check the image"
    return}
/// —— Convert absolute coordinates of tile0 to reference coordinate(for laser scanning microscopy) —— ///
else if(absoluteF.size() ==1 && absoluteF[0] == 0){
    def minX = tilePosition[1..-1].collect{ it[0] }.min()
    def maxX = tilePosition[1..-1].collect{ it[0] }.max()
    def minY = tilePosition[1..-1].collect{ it[1] }.min()
    def maxY = tilePosition[1..-1].collect{ it[1] }.max()
    def corners = [[minX,minY],[minX,maxY],[maxX,minY],[maxX,maxY]]
    def originPoint = corners - tilePosition[1..-1]
    tilePosition [0] = originPoint.flatten()
}
//println  tilePosition

/// -- Calculate tile-to-tile shift for mosaic imaging -- ///
int minRequiredOverlap = 5 
def frameSizeX = meta.getPixelsSizeX(0)
def frameSizeY = meta.getPixelsSizeY(0)
List<Map> tileDis =  tilePosition.findAll{it -> tilePosition.indexOf(it) !=0}.collect{ p ->
    double dx = p[0] - tilePosition [0][0]
    double dy = p[1] - tilePosition[0][1]
    double d2 = dx*dx + dy * dy
    return [tile: p, dist2: d2]//因為沒有要回傳最後一行 d2，所以要特別寫 return
}
tileDis.sort {a,b -> a.dist2 <=> b.dist2}
def nearestTwo = tileDis.take(2)
double x_shift = nearestTwo[1].tile[0] - nearestTwo[0].tile[0]
x_shift = x_shift.abs()
double x_maxTrustedTileShift = frameSizeX.getValue() - x_shift
double y_shift = nearestTwo[1].tile[1] - nearestTwo[0].tile[1]
y_shift = y_shift.abs()
double y_maxTrustedTileShift = frameSizeY.getValue() - y_shift

if(x_maxTrustedTileShift <= 4 || y_maxTrustedTileShift <=4){
	def ErrorOverlap = new GenericDialog("ERROR: No Overlap Detected")
	ErrorOverlap.addMessage("The estimated tile overlap is ${(x_maxTrustedTileShift + y_maxTrustedTileShift)/2} pixels, below the minimum required ${minRequiredOverlap} px.\n\n" +
						"The original dataset likely lacks sufficient overlap,so further stitching cannot be performed.")
	ErrorOverlap.showDialog()
	println "==== Terminated at ${new Date().format('yyyy-MM-dd HH:mm:ss')}due to insufficient tile overlap ===="
	new File(outputPath).deleteDir();
	return
	}

//// -- 7. Create the Xml file for Bigstitcher -- ////
def xmlPath = new File(StitchTemp, "IHC-Tileshading.xml")
//def outputFile = Paths.get(outputPath)
//def outputPath = "E:/My_projects/ImageJ/Stitch_project/Demo_img/myCreator.xml"
def writer =  new StringWriter()
def xml = new  MarkupBuilder(writer)
def xml_Info = ["xml_Tp": 0]
def attributeList = [
        "Ill": [name: "illumination", tag: "Illumination", value: 0],
        "Chn": [name: "channel", tag: "Channel", value: 0],
        "Tile": [name: "tile", tag: "Tile", value: nTiles],
        "Ang" : [name: "angle", tag: "Angle", value: 0]
]

xml.SpimData(version:"0.2") {
    BasePath(type: "relative", ".")
    SequenceDescription {
        ImageLoader(format: "spimreconstruction.stack.ij") {
            imagedirectory(type: "relative", ".")
            filePattern("tile_{x}.tiff")
            layoutTimepoints(xml_Info["xml_Tp"])
            layoutChannels(attributeList["Chn"].value)
            layoutIlluminations(attributeList["Ill"].value)
            layoutAngles(attributeList["Ang"].value)
            layoutTiles(attributeList["Tile"].value)
        }
        ViewSetups{
            for (int i=0; i < nTiles; i++){
                ViewSetup{
                    id(i)
                    name(i)
                    size("${frameSizeX} ${frameSizeY} 1")
                    voxelSize{
                        unit("pixel")
                        size("1.0 1.0 1.0")
                    }
                    attributes{
                        illumination(attributeList["Ill"].value)
                        channel(attributeList["Chn"].value)
                        tile(i)
                        angle(attributeList["Ang"].value)
                    }
                }
            }
            attributeList.each { key, value ->
                Attributes(name: value.name){
                    if(value.value ==0) {
                        delegate."${value.tag}"() {
                            id(value.value)
                            name(value.value)
                        }
                    }
                    else{
                        for(int j=0; j < value.value; j++){
                            delegate."${value.tag}"() {
                                id(j)
                                name(j+1)
                            }

                        }
                    }
                }

            }


        }
        Timepoints(type: "pattern"){
            integerpattern()
        }
    }
    ViewRegistrations{
        for(int k=0; k< nTiles; k++){
            ViewRegistration( timepoint: "0", setup: k){
                ViewTransform(type: "affine"){
                    Name("calibration")
                    affine("1.0 0.0 0.0 ${tilePosition[k][0]} 0.0 1.0 0.0 ${tilePosition[k][1]} 0.0 0.0 1.0 0.0")
                }
            }
        }
    }
    ViewInterestPoints()
    BoundingBoxes()
    PointSpreadFunctions()
    StitchingResults()
    IntensityAdjustments()
}

// === Save the XML ===
def xmlHeader = '<?xml version="1.0" encoding="UTF-8"?>\n'
def xmlBody = writer.toString()
def fullXml = xmlHeader + xmlBody
Files.write(xmlPath.toPath(), fullXml.getBytes(StandardCharsets.UTF_8))
println "A temporary dataset.xml saved to: ${xmlPath}"

//// —— 6. Open image and reshape —— ////
ImagePlus[] imps = BF.openImagePlus(opts)
def nChannel = imps[0].getNChannels()
/// -- Creat the new stack object -- ///
def width = imps[0].width
def height = imps[0].height
def Raw = new ImageStack(width, height)
for(imp in imps) {
    if (imp.getWidth() == width && imp.getHeight() == height) {
        def subStack = imp.getStack()
        for (int i =1; i <= subStack.getSize(); i++){
            Raw.addSlice(subStack.getProcessor(i))
        }
    }
    imp.close()
}
imps = null
/// -- Reshape Stack to multiple channel and tiles as z stack --///
def reshapeRaw = new ImagePlus("Raw", Raw)
reshapeRaw.setDimensions( nChannel, nTiles, 1)
reshapeRaw.setOpenAsHyperStack(true)
Raw = null
//reshapeRaw.show()
/// -- Save the Raw tiles to "BasicTemp" folder -- ///
def basicImps = []
imps = ChannelSplitter.split(reshapeRaw)
for( int nChan = 0; nChan <nChannel; nChan++){
    def outPath = "${outputPath.toString()}/BasicTemp/Raw_C${nChan+1}.tiff"
    IJ.saveAsTiff(imps[nChan], outPath)
    //println " Channel ${nChan+1} is saved"
    basicImps.add("Raw_C${nChan+1}.tiff")
}
imps =null
WindowManager.getImage("Raw")?.close()

//// -- 7. BaSiC correction -- ////
/// -- Reorder the image processing sequence based on the reference channel --///
/// -- Run Basic -- ///
for ( int i = 0; i < nChannel; i++ ){
    def f = new File (basicTemp, basicImps[i])
    def cf = new File (basicTemp, "Corrected${basicImps[i].replaceAll("Raw", "")}")
    ImagePlus basicImp = IJ.openImage(f.toString())
    basicImp.show()
    IJ.run("BaSiC ", "processing_stack="+basicImps[i]+" flat-field=None dark-field=None shading_estimation=[Estimate shading profiles] shading_model=[Estimate both flat-field and dark-field] setting_regularisationparametes=Automatic temporal_drift=Ignore correction_options=[Compute shading and correct images] lambda_flat=0.50 lambda_dark=0.50")
    basicImp.close()
    def closeList = [basicImps[i], "Flat-field:${basicImps[i]}", "Dark-field:${basicImps[i]}"]
    closeList.each{ title ->
        WindowManager.getImage(title)?.close()
    }
    ImagePlus CorrectedImg = WindowManager.getImage("Corrected:${basicImps[i]}")
    IJ.saveAsTiff( CorrectedImg, cf.toString())
    WindowManager.getImage("Corrected${basicImps[i].replaceAll("Raw", "")}")?.close()
    //new WaitForUserDialog("339-BaSiC").show()
}

def stitchImps = basicImps.collect{ imp ->
	imp.replaceAll("Raw", "Corrected")
	}
/// -- Save Basic result as split image with series name -- ///
for ( int i = 0; i < nChannel; i++ ){
	def Indx1 = ["Corrected_C${ (int)refChan }.tiff"]
	stitchImps = Indx1 + (stitchImps - Indx1)
	
	def tempPath = new File(basicTemp, stitchImps[i])
    def tempstack = IJ.openImage(tempPath.toString())
    //new WaitForUserDialog("Check").show()
    tempstack = tempstack.getStack()
	//new WaitForUserDialog("355").show()
    for(int t = 1; t <= tempstack.size(); t++){
        //def StitchTemp = new File (outputPath, "StitchTemp")
        def ff = new File (StitchTemp, "tile_${t}.tiff")
        ImagePlus tile = new ImagePlus("tile_${t}.tiff", tempstack.getProcessor(t))
        IJ.saveAsTiff( tile, ff.toString())
    }

//// -- 8. BigStitcher with temporary XML file(IHC-Tileshading.xml) -- ////
/// -- Perform pairwise shift calculation for Stitcher and discard links exceeding the shift threshold -- ///
    if ( i == 0 ) {
        //new WaitForUserDialog("Check").show()
        IJ.run("Calculate pairwise shifts ...", "select=${xmlPath.toString()} process_angle=[All angles] process_channel=[All channels] process_illumination=[All illuminations] process_tile=[All tiles] process_timepoint=[All Timepoints] method=[Phase Correlation] show_expert_algorithm_parameters downsample_in_x=1 downsample_in_y=1 number_of_peaks_to_check=5 minimal_overlap=0 subpixel_accuracy manually_set_number_of_parallel_tasks number_of_parallel_tasks=4")
        IJ.run("Filter pairwise shifts ...", "select=${xmlPath.toString()} min_r=0 max_r=1 filter_by_shift_in_each_dimension max_shift_in_x=${x_maxTrustedTileShift} max_shift_in_y=${y_maxTrustedTileShift} max_shift_in_z=0 max_displacement=0");
        IJ.run("Optimize globally and apply shifts ...", "select=${xmlPath.toString()} process_angle=[All angles] process_channel=[All channels] process_illumination=[All illuminations] process_tile=[All tiles] process_timepoint=[All Timepoints] relative=2.500 absolute=3.500 global_optimization_strategy=[Two-Round using Metadata to align unconnected Tiles and iterative dropping of bad links] fix_group_0-0")
    }
/// -- Fusion tiles -- ///
	//new WaitForUserDialog("Check").show()
    def fff = new File (Result, "Fused_${channelData[stitchImps[i]].label}.tiff")
    IJ.run("Image Fusion", "select=${xmlPath.toString()} process_angle=[All angles] process_channel=[All channels] process_illumination=[All illuminations] process_tile=[All tiles] process_timepoint=[All Timepoints] bounding_box=[All Views] downsampling=1 interpolation=[Linear Interpolation] fusion_type=[Avg, Blending] pixel_type=[32-bit floating point] interest_points_for_non_rigid=[-= Disable Non-Rigid =-] produce=[Each timepoint & channel] fused_image=[Display using ImageJ] display=[precomputed (fast, complete copy in memory before display)] ");
    ImagePlus Fused = WindowManager.getImage("fused_tp_0_ch_0")
    Fused.setTitle("Fused_${channelData[stitchImps[i]].label}")
    if(bitDepth == 16){
    	Fused.getProcessor().setMinAndMax(0,65535)
    	}
    else if(bitDepth == 8){
    	Fused.getProcessor().setMinAndMax(0,255)
    	}
    IJ.run("${bitDepth}-bit")
    //new WaitForUserDialog("Check").show()
    def iter = [value: i]
    if(mode == "Manual"){
        ///create the thread lock
        def locker = new Object()
        SwingUtilities.invokeLater {
            /// Create Dialog for manual check ///
            def frame = new JFrame("Check the Result of ${channelData[stitchImps[i]].label}")
            frame.setSize(520, 150)
            frame.setLayout(new BorderLayout(10, 10))
            def label = new JLabel("<html> <div style='font-size:14pt;'> Please review the alignment of the fused image (zooming is allowed). Select an action when done!!  <br> </div> </html>")
            label.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20))
            def btnContinue
            if(iter.value+1 < nChannel) {
                btnContinue = new JButton("Go Next")
            }
            else {btnContinue = new JButton("Finish")}
            def btnGoback = new JButton("Change the Ref. channel")
            def btnCancel = new JButton("Cancel ALL")
            ///set the function of buttons
            btnContinue.addActionListener {
                synchronized (locker) {
                    // genert the LUT
                    LUT lut = LUT.createLutFromColor(channelData [stitchImps[i]].color)
                    // Apply and reflash images
                    Fused.getProcessor().setLut(lut)
                    Fused.updateAndDraw()
                    //IJ.run(Fused, channelData[basicImps[i]].color, "" )
                    IJ.saveAsTiff(Fused, fff.toString())
                    IJ.log(" process to next channel")
                    locker.notify()
                }
                frame.dispose()
            }

            btnGoback.addActionListener {
                synchronized (locker) {
                    iter.value = -1
                    def paraRef = new GenericDialog("Reselect the Ref. channel for sitiching")
					paraRef.addNumericField("Reference channel for stitching:", 1, 0)
					paraRef.showDialog()
					refChan = paraRef.getNextNumber()
                    IJ.log(" redo this channel")
                    locker.notify()
                }
                frame.dispose()
            }
            btnCancel.addActionListener {
                synchronized (locker) {
                    iter.value = 9999
                    println "The process was terminated early by the user."
                    locker.notify()
                }
                frame.dispose()
            }
            def btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10))
            btnPanel.add(btnContinue)
            btnPanel.add(btnGoback)
            btnPanel.add(btnCancel)
            frame.add(label, BorderLayout.CENTER)
            frame.add(btnPanel, BorderLayout.SOUTH)
            frame.setResizable(false)
            frame.setAlwaysOnTop(false)
            frame.setVisible(true)
        }
        //Main thread wait here
        synchronized (locker){
            locker.wait()
        }
    }
    if(mode == "Automatic"){
        LUT lut = LUT.createLutFromColor(channelData [stitchImps[i]].color)
        Fused.getProcessor().setLut(lut)
        Fused.updateAndDraw()
        IJ.saveAsTiff(Fused, fff.toString())
    }
    i = iter.value
    Fused.changes = false
    Fused.close()
}
//// -- 9. Clean the temp folder -- ////
new File(outputPath, "BasicTemp").deleteDir();
new File(outputPath, "StitchTemp").deleteDir();

//// -- 10. cord the timestamp when the process finishes -- ////
println "==== ALL DONE at ${new Date().format('yyyy-MM-dd HH:mm:ss')} ===="