package gov.nasa.arc.dert.landscape;

import gov.nasa.arc.dert.camera.BasicCamera;
import gov.nasa.arc.dert.landscape.io.TileSource;
import gov.nasa.arc.dert.landscape.layer.Layer;
import gov.nasa.arc.dert.landscape.layer.RasterLayer;
import gov.nasa.arc.dert.landscape.quadtree.QuadKey;
import gov.nasa.arc.dert.landscape.quadtree.QuadTree;
import gov.nasa.arc.dert.landscape.quadtree.QuadTreeFactory;
import gov.nasa.arc.dert.landscape.srs.SpatialReferenceSystem;
import gov.nasa.arc.dert.render.LayerEffects;
import gov.nasa.arc.dert.util.MathUtil;
import gov.nasa.arc.dert.view.Console;

import java.awt.Color;
import java.util.Arrays;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.scenegraph.hint.LightCombineMode;

/**
 * Provides a class for handling the Landscape.
 *
 */
public class Landscape
	extends Node {

	// numeric field formats based on landscape size
	public static String format, stringFormat;
	public static double defaultCellSize;

	public static int MAX_LEVELS = 50;

	// terrain tile source
	protected TileSource source;

	// the quad tree
	protected QuadTree quadTree;

	// the height map
	protected RasterLayer baseLayer;

	// list of image and other layers
	protected Layer[] layerList;

	// base layer tile dimensions
	protected int tileWidth, tileLength;

	// full terrain dimensions
	protected double terrainWidth, terrainLength;

	// pixel dimensions
	protected double pixelWidth, pixelLength;

	// the terrain vertical exaggeration
	protected double vertExaggeration = 1;

	// the maximum level of the height map
	protected int baseMapLevel;

	// a scene graph node to hold a translated quad tree so it can be scaled
	protected Node contents;

	// minimum Z value, maximum Z value
	protected double minZ, maxZ;

	// surface mesh color
	protected MaterialState materialState;
	protected Color surfaceColor;

	// texture state for field camera footprints and view sheds
	protected TextureState textureState;

	// object to manage layers
	protected LayerManager layerManager;

	// scale factor for millimeter scale terrains
	protected float pixelScale = 1;
	
	// bounds of the terrain
	protected double[] bounds;

	// spatial reference system for base layer
	private SpatialReferenceSystem srs;
	
	private static Landscape INSTANCE;
	
	public static Landscape createInstance(TileSource source, LayerManager layerManager, Color surfaceColor) {
		INSTANCE = new Landscape(source, layerManager, surfaceColor);
		return(INSTANCE);
	}
	
	public static Landscape getInstance() {
		return(INSTANCE);
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            source of tiles
	 * @param layerManager
	 *            manager of layers
	 * @param surfaceColor
	 *            surface color
	 */
	protected Landscape(TileSource source, LayerManager layerManager, Color surfaceColor) {
		super("Landscape");
		this.source = source;
		this.layerManager = layerManager;
		this.surfaceColor = surfaceColor;
		layerList = layerManager.getLayers();
		baseLayer = layerManager.getBaseLayer();
		pixelWidth = baseLayer.getPixelWidth();
		pixelLength = baseLayer.getPixelLength();
		// if we have a millimeter scale terrain we need to scale it up
		// so we can calculate normals
		if ((pixelWidth < 0.0001) || (pixelLength < 0.0001))
			pixelScale = 100;
		pixelWidth *= pixelScale;
		pixelLength *= pixelScale;
		minZ = baseLayer.getMinimumValue()[0];
		maxZ = baseLayer.getMaximumValue()[0];
		tileWidth = baseLayer.getTileWidth();
		tileLength = baseLayer.getTileLength();
		baseMapLevel = baseLayer.getNumberOfLevels() - 1;
		terrainWidth = baseLayer.getRasterWidth() * pixelWidth;
		terrainLength = baseLayer.getRasterLength() * pixelLength;
		bounds = new double[6];
		bounds[0] = -terrainWidth/2;
		bounds[3] = terrainWidth/2;
		bounds[1] = -terrainLength/2;
		bounds[4] = terrainLength/2;
		bounds[2] = minZ;
		bounds[5] = maxZ;
		textureState = new TextureState();
		textureState.setEnabled(true);
		setRenderState(textureState);
		// determine the default grid cell sizes and number formats based on
		// base layer physical size
		computeDefaultSizes();
		srs = new SpatialReferenceSystem(baseLayer.getProjectionInfo());
	}

	/**
	 * User changed the landscape layers. Reinitialize the landscape contents.
	 */
	public void resetLayers() {
		// free up existing tiles and run the Java garbage collector
		detachChild(contents);
		contents = null;
		quadTree = null;
		QuadTreeFactory.destroy();
		// get the new layer configuration
		if (!layerManager.initialize(source)) {
			return;
		}
		layerList = layerManager.getLayers();
		initialize();
	}

	/**
	 * Initialize the landscape. Create the factory, quad tree, and layers.
	 */
	public void initialize() {
		QuadTreeFactory factory = QuadTreeFactory.createInstance(source, baseLayer, layerList, pixelScale);
		factory.setSurfaceColor(surfaceColor);
//		factory.enableLayers(layerManager.layersEnabled);

		// create the top level quad tree tile
		quadTree = factory.getQuadTree(new QuadKey(), terrainWidth / tileWidth, terrainLength / tileLength, true);
		if (quadTree == null)
			throw new IllegalStateException("Root quadTree for "+getName()+" is empty or invalid.");
		quadTree.inUse = true;
		quadTree.updateWorldBound(true);

		// material state for surface color and shading
		materialState = new MaterialState();
		materialState.setColorMaterial(MaterialState.ColorMaterial.AmbientAndDiffuse);
		materialState.setEnabled(true);
		materialState.setColorMaterialFace(MaterialFace.Front);
		quadTree.setRenderState(materialState);
		if (isShadingFromSurface()) {
			quadTree.getSceneHints().setLightCombineMode(LightCombineMode.Inherit);
		} else {
			quadTree.getSceneHints().setLightCombineMode(LightCombineMode.Off);
		}

		// layer effects shader
		LayerEffects layerEffects = layerManager.getLayerEffects();
		quadTree.setRenderState(layerEffects);

		// make sure changes are realized
		quadTree.markDirty(DirtyType.RenderState);

		// translate to minimum elevation for terrain exaggeration
		contents = new Node("_landscape_contents");
		contents.setTranslation(0, 0, -minZ * pixelScale);
		contents.attachChild(quadTree);

		// attach the contents of this landscape
		attachChild(contents);
		updateGeometricState(0, true);
	}

	private void computeDefaultSizes() {
		format = "0.000";
		stringFormat = "%5.3f";
		double extent = Math.max(terrainWidth, terrainLength);
		int d = (int) Math.log10(extent);
		if (d >= 3) {
			defaultCellSize = Math.pow(10, d - 1)/2;
		} else if (d > 1) {
			defaultCellSize = 1;
		} else {
			defaultCellSize = Math.pow(10, d) / 100;
			format = "0.00000";
			stringFormat = "%7.5f";
		}
		if (layerManager.getGridCellSize() == 0) {
			layerManager.setGridCellSize(defaultCellSize);
		}
		Console.println(
			"Landscape size: East/West range = " + String.format(stringFormat, terrainWidth/pixelScale) + ", North/South range = " + String.format(stringFormat, terrainLength/pixelScale) + " "
				+ ", Elevation range = " + String.format(stringFormat, (baseLayer.getMaximumValue()[0] - minZ)) + "\n");

	}

	/**
	 * Get the name of the globe of this landscape.
	 * 
	 * @return
	 */
	public String getGlobeName() {
		return (srs.getProjection().getGlobeName());
	}

	/**
	 * Get the landscape spatial reference system.
	 * 
	 * @return
	 */
	public SpatialReferenceSystem getSpatialReferenceSystem() {
		return (srs);
	}

	/**
	 * Get the longitude and latitude of the center of the landscape.
	 * 
	 * @return
	 */
	public ReadOnlyVector3 getCenterLonLat() {
		Vector3 cntr = srs.getCenterLonLat();
		cntr.setZ(getElevation(0,0));
		return (cntr);
	}

	/**
	 * Get the minimum elevation of the landscape
	 * 
	 * @return
	 */
	public double getMinimumElevation() {
		return (minZ);
	}

	/**
	 * Get the maximum elevation of the landscape
	 * 
	 * @return
	 */
	public double getMaximumElevation() {
		return (maxZ);
	}

	/**
	 * Get the center of the landscape in contents frame (minimum Z subtracted
	 * from the elevation).
	 * 
	 * @return
	 */
	public ReadOnlyVector3 getCenter() {
		Vector3 center = new Vector3(quadTree.getCenter());
		double z = getZ(center.getX(), center.getY());
		if (Double.isNaN(z))
			z = minZ;
		center.setZ(z);
		return (center);
	}

	/**
	 * Get the vertices (x,y,z) between two points on the landscape.
	 * 
	 * @param vertex
	 *            the array to put the vertices
	 * @param p0
	 *            first point
	 * @param p1
	 *            second point
	 * @param maxLevel
	 *            get the data from the highest level possible
	 * @param contentsFrame
	 *            get the Z coordinate in the contents frame
	 * @return the number of vertices
	 */
	public int getVertices(float[] vertex, Vector3 p0, Vector3 p1, boolean maxLevel, boolean contentsFrame) {
		if (!quadTree.contains(p0.getX(), p0.getY())) {
			return (-1);
		}
		if (!quadTree.contains(p1.getX(), p1.getY())) {
			return (-1);
		}
		int n = getLineRaster(vertex, 0, p0, p1, pixelWidth, pixelLength, maxLevel);
		// subtract minimum Z to translate line to contents
		if (contentsFrame) {
			for (int i = 2; i < n; i += 3) {
				vertex[i] -= minZ * pixelScale;
			}
		}
		return (n);
	}

	/**
	 * Get the elevation at the given coordinate
	 * 
	 * @param x
	 * @param y
	 * @return NaN if outside the landscape
	 */
	public double getElevation(double x, double y) {
		if (quadTree.contains(x, y)) {
			return (quadTree.getElevation(x, y));
		}
		return (Double.NaN);
	}

	/**
	 * Get the surface normal at the given coordinate
	 * 
	 * @param x
	 * @param y
	 * @param store
	 *            container for result
	 * @return true if success
	 */
	public boolean getNormal(double x, double y, Vector3 store) {
		if (quadTree.contains(x, y)) {
			return (quadTree.getNormal(x, y, store));
		}
		return (false);
	}

	/**
	 * Get the Z coordinate in the contents object frame at the given X,Y
	 * coordinate
	 * 
	 * @param x
	 * @param y
	 * @return NaN if outside the landscape
	 */
	public double getZ(double x, double y) {
		if (quadTree.contains(x, y)) {
			return (quadTree.getElevation(x, y) - minZ * pixelScale);
		}
		return (Double.NaN);
	}

	/**
	 * Get the Z coordinate in the contents object frame at the given X,Y
	 * coordinate in the given quad tree.
	 * 
	 * @param x
	 * @param y
	 * @param qTree
	 * @return
	 */
	public double getZ(double x, double y, QuadTree qTree) {
		return (qTree.getElevation(x, y) - minZ * pixelScale);
	}

	/**
	 * Get the elevation at the given X,Y coordinate from the highest level tile
	 * that can be found.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double getElevationAtHighestLevel(double x, double y) {
		QuadKey key = source.getKey(x, y, terrainWidth, terrainLength);
		if (key == null) {
			return (Double.NaN);
		}
		QuadTree qt = QuadTreeFactory.getInstance().getQuadTree(key);
		if (qt == null) {
			return (Double.NaN);
		}
		return (qt.getElevation(x, y));
	}

	/**
	 * Get the elevation at the given X,Y coordinate from the specified level
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public double getElevation(double x, double y, int level) {
		QuadKey key = source.getKey(x, y, terrainWidth, terrainLength, level);
		if (key == null) {
			return (Double.NaN);
		}
		QuadTree qt = QuadTreeFactory.getInstance().getQuadTree(key);
		if (qt == null) {
			return (Double.NaN);
		}
		return (qt.getElevation(x, y));
	}

	/**
	 * Get the surface normal at the given X,Y coordinate from the highest level
	 * tile that can be found.
	 * 
	 * @param x
	 * @param y
	 * @param store
	 * @return
	 */
	public boolean getNormalAtHighestLevel(double x, double y, Vector3 store) {
		QuadKey key = source.getKey(x, y, terrainWidth, terrainLength);
		if (key == null) {
			return (false);
		}
		QuadTree qt = QuadTreeFactory.getInstance().getQuadTree(key);
		if (qt == null) {
			return (false);
		}
		return (qt.getNormal(x, y, store));
	}

	/**
	 * Convert OpenGL coordinates in contents object frame to world (planetary,
	 * projected) coordinates.
	 * 
	 * @param coord
	 */
	public void localToWorldCoordinate(Vector3 coord) {
		coord.multiplyLocal(1.0 / pixelScale);
		coord.setZ(coord.getZ() + minZ);
		srs.getProjection().localToWorld(coord);
	}

	/**
	 * Convert planetary (projected) coordinates to OpenGL coordinates in
	 * contents object frame.
	 * 
	 * @param coord
	 */
	public void worldToLocalCoordinate(Vector3 coord) {
		srs.getProjection().worldToLocal(coord);
		coord.setZ(coord.getZ() - minZ);
		coord.multiplyLocal(pixelScale);
	}

	/**
	 * Convert unprojected (Lon/Lat degrees) coordinates to projected (planetary)
	 * coordinates.
	 * 
	 * @param coord
	 */
	public void sphericalToWorldCoordinate(Vector3 coord) {
		try {
			srs.getProjection().sphericalToWorld(coord);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Convert projected (planetary) coordinates to unprojected (Lon/Lat)
	 * coordinates.
	 * 
	 * @param coord
	 */
	public void worldToSphericalCoordinate(Vector3 coord) {
		srs.getProjection().worldToSpherical(coord);
	}

	/**
	 * Convert unprojected (Lon/Lat degrees) coordinates to OpenGL coordinates in
	 * contents object frame.
	 * 
	 * @param coord
	 */
	public void sphericalToLocalCoordinate(Vector3 coord) {
		try {
			srs.getProjection().sphericalToWorld(coord);
			srs.getProjection().worldToLocal(coord);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Given a region, return the mean elevation sampled from the landscape.
	 * Sample size is the same as the original raster pixel dimensions.
	 * 
	 * @param vertex
	 *            array of vertices defining the region
	 * @param lowerBound
	 *            the lower bound of the region
	 * @param upperBound
	 *            the upper bound of the region
	 * @return
	 */
	public double getSampledMeanElevationOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound,
		ReadOnlyVector3 upperBound) {
		double xMin = lowerBound.getX();
		double yMin = lowerBound.getY();
		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
		Vector3 vert = new Vector3();
		double meanElevation = 0;
		int count = 0;
		for (int i = 0; i < rSampleSize; ++i) {
			for (int j = 0; j < cSampleSize; ++j) {
				if (Thread.currentThread().isInterrupted())
					return(Double.NaN);
				vert.set(xMin + j * pixelWidth, yMin + i * pixelLength, 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					double el = getElevationAtHighestLevel(vert.getX(), vert.getY());
					if (!Double.isNaN(el)) {
						meanElevation += el;
						count++;
					}
				}
			}
		}
		meanElevation /= count;
		return (meanElevation);
	}

	/**
	 * Given a region, return the mean slope sampled from the landscape.
	 * 
	 * @param vertex
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public double getSampledMeanSlopeOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound) {
		double xMin = lowerBound.getX();
		double yMin = lowerBound.getY();
		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
		Vector3 vert = new Vector3();
		Vector3 meanNormal = new Vector3();
		Vector3 store = new Vector3();
		int count = 0;
		for (int i = 0; i < rSampleSize; ++i) {
			for (int j = 0; j < cSampleSize; ++j) {
				if (Thread.currentThread().isInterrupted())
					return(Double.NaN);
				vert.set((float) (xMin + j * pixelWidth), (float) (yMin + i * pixelLength), 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					boolean success = getNormalAtHighestLevel(vert.getX(), vert.getY(), store);
					if (success) {
						meanNormal.addLocal(store);
						count++;
					}
				}
			}
		}
		vert.set(meanNormal);
		vert.multiplyLocal(1.0 / count);
		return (MathUtil.getSlopeFromNormal(vert));
	}

	/**
	 * Estimate the volume of the landscape inside the given polygon by
	 * sampling. Use the minimum elevation of the polygon region as the lower
	 * bound.
	 * 
	 * @param vertex
	 *            the vertices of the polygon
	 * @param samples
	 *            the number of samples on a side of the rectangular region
	 *            defined by the polygon bounds
	 * @return the volume
	 */
//	public double getSampledVolumeOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound,
//		Spatial polygon) {
//		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
//		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
//		Vector3 vert = new Vector3();
//		double volume = 0;
//		// sample the landscape for elevation
//		for (int i = 0; i < rSampleSize; ++i) {
//			for (int j = 0; j < cSampleSize; ++j) {
//				vert.set((float) (lowerBound.getX() + j * pixelWidth), (float) (lowerBound.getY() + i * pixelLength), 0);
//				if (MathUtil.isInsidePolygon(vert, vertex)) {
//					double el = getElevationAtHighestLevel(vert.getX(), vert.getY())-minZ;
//					if (!Double.isNaN(el)) {
//						vert.setZ(el);
//						if (el < lowerBound.getZ()) {
//							el = -getSample(vert, Vector3.UNIT_Z, polygon);
//						} else if (el > upperBound.getZ()) {
//							el = getSample(vert, Vector3.NEG_UNIT_Z, polygon);
//						} else {
//							el = getSample(vert, Vector3.NEG_UNIT_Z, polygon);
//							if (Double.isNaN(el)) {
//								el = -getSample(vert, Vector3.UNIT_Z, polygon);
//							}
//						}
//						if (!Double.isNaN(el)) {
//							volume += el * pixelWidth * pixelLength;
//						}
//					}
//				}
//			}
//		}
//		return (volume);
//	}

	public double[] getSampledVolumeOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound, Spatial polygon) {
		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
		Vector3 vert = new Vector3();
		double volumeAbove = 0;
		double volumeBelow = 0;
		// sample the landscape for elevation
		for (int i = 0; i < rSampleSize; ++i) {
			for (int j = 0; j < cSampleSize; ++j) {
				if (Thread.currentThread().isInterrupted())
					return(null);
				vert.set((float) (lowerBound.getX() + j * pixelWidth), (float) (lowerBound.getY() + i * pixelLength), 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					double el = getElevationAtHighestLevel(vert.getX(), vert.getY())-minZ * pixelScale;
					if (!Double.isNaN(el)) {
						vert.setZ(upperBound.getZ()+1);
						double pZ = sampleSpatial(vert, Vector3.NEG_UNIT_Z, polygon);
//							System.err.println("Landscape.getSampledVolumeOfRegion "+el+" "+maxZ+" "+minZ+" "+pZ+" "+vert);
						if (!Double.isNaN(pZ)) {
							if (el < pZ) {
								volumeBelow += (pZ-el);
							}
							else {
								volumeAbove += (el-pZ);
							}
						}
					}
				}
			}
		}
		return (new double[] {volumeAbove*pixelWidth*pixelLength, volumeBelow*pixelWidth*pixelLength});
	}

	public double[] getSampledVolumeOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound, double elev) {
		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
		Vector3 vert = new Vector3();
		double volumeAbove = 0;
		double volumeBelow = 0;
		// sample the landscape for elevation
		for (int i = 0; i < rSampleSize; ++i) {
			for (int j = 0; j < cSampleSize; ++j) {
				if (Thread.currentThread().isInterrupted())
					return(null);
				vert.set((float) (lowerBound.getX() + j * pixelWidth), (float) (lowerBound.getY() + i * pixelLength), 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					double el = getElevationAtHighestLevel(vert.getX(), vert.getY());
					if (!Double.isNaN(el)) {
						if (el < elev)
							volumeBelow += (elev-el);
						else
							volumeAbove += (el-elev);
					}
				}
			}
		}
		return (new double[] {volumeAbove*pixelWidth*pixelLength, volumeBelow*pixelWidth*pixelLength});
	}

	private double sampleSpatial(Vector3 p0, ReadOnlyVector3 dir, Spatial node) {
		// Create a ray starting from the point, and going in the given
		// direction
		PrimitivePickResults pr = new PrimitivePickResults();
		final Ray3 ray = new Ray3(p0, dir);
		pr.setCheckDistance(true);
//		System.err.println("Landscape.sampleSpatial "+ray+" "+node.getWorldBound());
		PickingUtil.findPick(node, ray, pr, false);
		if (pr.getNumber() == 0) {
			return (Double.NaN);
		}
		double dist = Double.MAX_VALUE;
		for (int i = 0; i < pr.getNumber(); ++i) {
			PickData pd = pr.getPickData(i);
			IntersectionRecord ir = pd.getIntersectionRecord();
			int closestIndex = ir.getClosestIntersection();
			double d = ir.getIntersectionDistance(closestIndex);
			if (d < dist) {
				dist = d;
			}
		}
		return (p0.getZ()-dist);
	}

	/**
	 * Estimate the elevation difference of the landscape with the given polygon
	 * by sampling.
	 * 
	 * @param vertex
	 *            the vertices of the polygon
	 * @param samples
	 *            the number of samples on a side of the rectangular region
	 *            defined by the polygon bounds
	 * @return an array of elevation difference
	 */
	public int[] getSampledDifferenceOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound,
		double[] planeEq, double sampleSize, float[][] result, float[] minMaxElev) {
		int columns = (int) ((upperBound.getX() - lowerBound.getX()) / sampleSize);
		int rows = (int) ((upperBound.getY() - lowerBound.getY()) / sampleSize);
		Vector3 vert = new Vector3();
		minMaxElev[0] = Float.MAX_VALUE;
		minMaxElev[1] = -Float.MAX_VALUE;
		// sample the landscape for elevation
		for (int i = 0; i < rows; ++i) {
			for (int j = 0; j < columns; ++j) {
				vert.set((float) (lowerBound.getX() + j * sampleSize), (float) (lowerBound.getY() + i * sampleSize), 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					double el = getElevationAtHighestLevel(vert.getX(), vert.getY())-minZ;
					double elPoly = MathUtil.getPlaneZ(vert.getX(), vert.getY(), planeEq);
//					System.err.println("Landscape.getSampledDifferenceOfRegion "+el+" "+elPoly);
					result[i][j] = (float) (el - elPoly);
					if (result[i][j] < minMaxElev[0]) {
						minMaxElev[0] = result[i][j];
					}
					if (result[i][j] > minMaxElev[1]) {
						minMaxElev[1] = result[i][j];
					}
				} else {
					result[i][j] = Float.NaN;
				}
			}
			for (int j = columns; j < result[0].length; ++j) {
				result[i][j] = Float.NaN;
			}
		}
		for (int i = rows; i < result.length; ++i) {
			Arrays.fill(result[i], Float.NaN);
		}
		return (new int[] { rows, columns });
	}

//	private double getSample(Vector3 p0, ReadOnlyVector3 dir, Spatial node) {
//		// Create a ray starting from the point, and going in the given
//		// direction
//		PrimitivePickResults pr = new PrimitivePickResults();
//		final Ray3 ray = new Ray3(p0, dir);
//		pr.setCheckDistance(true);
//		PickingUtil.findPick(node, ray, pr, false);
//		if (pr.getNumber() == 0) {
//			return (Double.NaN);
//		}
//		double dist = Double.MAX_VALUE;
//		for (int i = 0; i < pr.getNumber(); ++i) {
//			PickData pd = pr.getPickData(i);
//			IntersectionRecord ir = pd.getIntersectionRecord();
//			int closestIndex = ir.getClosestIntersection();
//			double d = ir.getIntersectionDistance(closestIndex);
//			if (d < dist) {
//				dist = d;
//			}
//		}
//		return (dist);
//	}

	/**
	 * Given a region, return its surface area sampled from the landscape.
	 * 
	 * @param vertex
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public double getSampledSurfaceAreaOfRegion(Vector3[] vertex, ReadOnlyVector3 lowerBound, ReadOnlyVector3 upperBound) {
		Vector3 vert = new Vector3();
		int cSampleSize = (int) ((upperBound.getX() - lowerBound.getX()) / pixelWidth);
		int rSampleSize = (int) ((upperBound.getY() - lowerBound.getY()) / pixelLength);
		double surfaceArea = 0;
		for (int i = 0; i < rSampleSize; ++i) {
			for (int j = 0; j < cSampleSize; ++j) {
				if (Thread.currentThread().isInterrupted())
					return(Double.NaN);
				vert.set((float) (lowerBound.getX() + (j+0.5) * pixelWidth), (float) (lowerBound.getY() + (i+0.5) * pixelLength), 0);
				if (MathUtil.isInsidePolygon(vert, vertex)) {
					double sa = getSurfaceArea(vert);
					if (!Double.isNaN(sa)) {
						surfaceArea += sa;
					}
				}
			}
		}
		return (surfaceArea);
	}

	private double getSurfaceArea(ReadOnlyVector3 point) {
		double xd = pixelWidth / 2;
		double yd = pixelLength / 2;
		double x = point.getX();
		double y = point.getY();
		double surfaceArea = 0;
		surfaceArea += getAreaOfTriangle(x, y, x - xd, y + yd, x, y + yd);
		surfaceArea += getAreaOfTriangle(x, y, x + xd, y + yd, x, y + yd);
		surfaceArea += getAreaOfTriangle(x, y, x - xd, y, x - xd, y + yd);
		surfaceArea += getAreaOfTriangle(x, y, x + xd, y, x + xd, y + yd);
		surfaceArea += getAreaOfTriangle(x, y, x - xd, y, x - xd, y - yd);
		surfaceArea += getAreaOfTriangle(x, y, x + xd, y, x + xd, y - yd);
		surfaceArea += getAreaOfTriangle(x, y, x - xd, y - yd, x, y - yd);
		surfaceArea += getAreaOfTriangle(x, y, x + xd, y - yd, x, y - yd);
		return (surfaceArea);
	}

	private double getAreaOfTriangle(double x0, double y0, double x1, double y1, double x2, double y2) {
		double z0 = getElevationAtHighestLevel(x0, y0);
		if (Double.isNaN(z0)) {
			return (0);
		}
		double z1 = getElevationAtHighestLevel(x1, y1);
		if (Double.isNaN(z1)) {
			return (0);
		}
		double z2 = getElevationAtHighestLevel(x2, y2);
		if (Double.isNaN(z2)) {
			return (0);
		}
		double area = MathUtil.getAreaOfTriangle(x0, y0, z0, x1, y1, z1, x2, y2, z2);
		return(area);
	}

	/**
	 * Given two points, get the vertices between them.
	 * 
	 * @param vertex
	 * @param start
	 * @param p0
	 * @param p1
	 * @param stepWidth
	 * @param stepLength
	 * @param maxLevel
	 * @return
	 */
	private int getLineRaster(float[] vertex, int start, Vector3 p0, Vector3 p1, double stepWidth, double stepLength,
		boolean maxLevel) {
		double dx = p1.getX() - p0.getX();
		double dy = p1.getY() - p0.getY();
		double lineLength = Math.sqrt(dx * dx + dy * dy);
		double step = Math.min(stepWidth, stepLength);
		int n = (int) (lineLength / step);
		if (n == 0) {
			vertex[0] = p0.getXf();
			vertex[1] = p0.getYf();
			if (maxLevel) {
				vertex[2] = (float) getElevationAtHighestLevel(p0.getX(), p0.getY());
			} else {
				vertex[2] = (float) getElevation(p0.getX(), p0.getY());
			}
			vertex[3] = p1.getXf();
			vertex[4] = p1.getYf();
			if (maxLevel) {
				vertex[5] = (float) getElevationAtHighestLevel(p1.getX(), p1.getY());
			} else {
				vertex[5] = (float) getElevation(p1.getX(), p1.getY());
			}
			return (6);
		}
		dx = stepWidth * dx / lineLength;
		dy = stepLength * dy / lineLength;
		if (n * step < lineLength) {
			n++;
		}
		double x = p0.getX();
		double y = p0.getY();
		for (int i = 0; i < n - 1; ++i) {
			vertex[i * 3] = (float) x;
			vertex[i * 3 + 1] = (float) y;
			if (maxLevel) {
				vertex[i * 3 + 2] = (float) getElevationAtHighestLevel(x, y);
			} else {
				vertex[i * 3 + 2] = (float) getElevation(x, y);
			}
			x += dx;
			y += dy;
		}
		vertex[(n - 1) * 3] = p1.getXf();
		vertex[(n - 1) * 3 + 1] = p1.getYf();
		if (maxLevel) {
			vertex[(n - 1) * 3 + 2] = (float) getElevationAtHighestLevel(p1.getX(), p1.getY());
		} else {
			vertex[(n - 1) * 3 + 2] = (float) getElevation(p1.getX(), p1.getY());
		}
		return (n * 3);
	}

	/**
	 * Get the landscape texture state
	 * 
	 * @return
	 */
	public TextureState getTextureState() {
		return (textureState);
	}

	/**
	 * Get the layer manager
	 * 
	 * @return
	 */
	public LayerManager getLayerManager() {
		return (layerManager);
	}

	/**
	 * Get the pixel width
	 * 
	 * @return
	 */
	public double getPixelWidth() {
		return (pixelWidth);
	}

	/**
	 * Get the pixel length
	 * 
	 * @return
	 */
	public double getPixelLength() {
		return (pixelLength);
	}

	/**
	 * Add field camera layers
	 * 
	 * @param fieldCamera
	 */
	public void addFieldCamera(String cameraName) {
		layerManager.addFieldCamera(cameraName);
	}

	/**
	 * Remove layers for a field camera.
	 * 
	 * @param fieldCamera
	 */
	public void removeFieldCamera(String cameraName) {
		if (layerManager.removeFieldCamera(cameraName)) {
			LayerEffects layerEffects = layerManager.getLayerEffects();
			layerEffects.setEnabled(true);
			quadTree.setRenderState(layerEffects);
			quadTree.markDirty(DirtyType.RenderState);
		}
	}

	/**
	 * Dispose of landscape resources.
	 */
	public void dispose() {
		quadTree = null;
		QuadTreeFactory.destroy();
		for (int i = 0; i < layerList.length; ++i) {
			if (layerList[i] != null) {
				layerList[i].dispose();
			}
		}
	}

	/**
	 * Set the surface color.
	 * 
	 * @param surfaceColor
	 */
	public void setSurfaceColor(Color surfaceColor) {
		this.surfaceColor = surfaceColor;
		QuadTreeFactory.getInstance().setSurfaceColor(surfaceColor);
	}

	/**
	 * Get the surface color.
	 * 
	 * @return
	 */
	public Color getSurfaceColor() {
		return (surfaceColor);
	}

	/**
	 * Change the vertical exaggeration of the Z coordinate by scaling.
	 * 
	 * @param val
	 */
	public void setVerticalExaggeration(double val) {
		setScale(1, 1, val);
		vertExaggeration = val;
	}

	/**
	 * Get the vertical exaggeration scale factor.
	 * 
	 * @return
	 */
	public double getVerticalExaggeration() {
		return (vertExaggeration);
	}

	/**
	 * Get the width of the original raster used for the base layer.
	 * 
	 * @return
	 */
	public int getRasterWidth() {
		return (baseLayer.getRasterWidth());
	}

	/**
	 * Get the length of the original raster used for the base layer.
	 * 
	 * @return
	 */
	public int getRasterLength() {
		return (baseLayer.getRasterLength());
	}

	/**
	 * Get the level that is at the original raster resolution. This should be
	 * the highest level unless a subpyramid has been added.
	 * 
	 * @return
	 */
	public int getBaseMapLevel() {
		return (baseMapLevel);
	}

	/**
	 * Get the pixel scale factor
	 * 
	 * @return
	 */
	public float getPixelScale() {
		return (pixelScale);
	}

	/**
	 * Update the resolution of the tiles in the landscape.
	 */
	public boolean update(BasicCamera camera) {
		boolean qtChanged = false;
		if (quadTree != null) {
			qtChanged = quadTree.update(camera);
			if (qtChanged) {
				quadTree.stitch();
//				quadTree.isDirty();
			}
		}
		return(qtChanged);
	}

	/**
	 * Show the image and derivative layers
	 * 
	 * @param enable
	 */
	public void enableLayers(boolean enable) {
		layerManager.enableLayers(enable);
		quadTree.markDirty(DirtyType.RenderState);
	}

	/**
	 * Are the layers showing.
	 * 
	 * @return
	 */
	public boolean isLayersEnabled() {
		return (layerManager.layersEnabled);
	}

	/**
	 * Is the blend factor auto-adjusting
	 * 
	 * @return
	 */
	public boolean isAutoAdjustBlendFactor() {
		return (layerManager.autoAdjustOpacity);
	}

	/**
	 * Is shading provided by surface normals.
	 * 
	 * @return
	 */
	public boolean isShadingFromSurface() {
		return (layerManager.shadingFromSurface);
	}
	
	public boolean isWireFrame() {
		WireframeState wfs = (WireframeState) getLocalRenderState(RenderState.StateType.Wireframe);
		if (wfs == null) {
			return (false);
		}
		if (!wfs.isEnabled()) {
			return (false);
		}
		return (true);
	}


	/**
	 * Set the shading.
	 * 
	 * @param shading
	 */
	public void setShadingFromSurface(boolean shading) {
		layerManager.shadingFromSurface = shading;
		if (shading) {
			quadTree.getSceneHints().setLightCombineMode(LightCombineMode.Inherit);
			quadTree.markDirty(DirtyType.RenderState);
		} else {
			quadTree.getSceneHints().setLightCombineMode(LightCombineMode.Off);
			quadTree.markDirty(DirtyType.RenderState);
		}
	}
	
	public double[] getBounds() {
		return(bounds);
	}
}
