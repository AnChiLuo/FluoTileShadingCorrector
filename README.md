# FluoTileShadingCorrector
The grid-like shading is a common artifact in whole slide images(WSIs). It may arise from vignetting, uneven illumination, sample surface irregularities. Affected by this artifact, tile images exhibit shading, where signal intensity gradually decays from the center toward the edges. When these tiles are stitched together, the resulting WSI displays brightness discontinuities and grid-like patterns, which complicates downstream image analysis tasks.Many prospective shading correction methods have been widely used to address this artifact. These methods rely on a high-quality reference image acquired from an empty field of view. However, obtaining such a reference image—entirely free of dust, residual fluorescence, or staining artifacts is often unrealistic, especially when working with fluorescence-labeled tissue sections. Furthermore, prospective shading correction is only applicable to camera-based imaging system, where shading patterns are spatially consistent across the tiles. In contrast, laser scanning confocal microscopy acquires images point by point, introducing dynamic intensity variations that vary not only between tiles but also within each tile.  As a result, static reference-based correction is generally ineffective for confocal datasets.







