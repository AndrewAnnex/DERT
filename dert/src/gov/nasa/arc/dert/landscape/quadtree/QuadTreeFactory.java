package gov.nasa.arc.dert.landscape.quadtree;

import gov.nasa.arc.dert.landscape.io.QuadTreeTile;
import gov.nasa.arc.dert.landscape.io.TileSource;
import gov.nasa.arc.dert.landscape.layer.Layer;
import gov.nasa.arc.dert.landscape.layer.RasterLayer;
import gov.nasa.arc.dert.render.SharedTexture2D;
import gov.nasa.arc.dert.util.UIUtil;

import java.awt.Color;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Helper class for building QuadTree tiles.
 *
 */
public class QuadTreeFactory {
	
	private static QuadTreeFactory instance;

	// Fields used for missing vertices
	private float[] missingColor = {0, 0, 0, 0};
	private float missingFillValue;

	// List of layers
	private Layer[] layerList;

	// The base layer
	private RasterLayer baseLayer;

	// Threading service
	private ExecutorService executor;

	// The surface color
	private float[] surfaceColor;

	// Layers are showing
	private boolean layersEnabled = true;

	// Pixel scale factor for millimeter scale terrains
	private double pixelScale;
	private double minZ;

	// Dimensions
	private int tileWidth, tileLength;

	// The source of tile data
	private TileSource source;

	// A texture for empty tiles
	private Texture emptyTexture;

	// The dimensions of the entire terrain
	private double terrainWidth, terrainLength;
	
	private int bytesPerTile;
	
	private QuadTreeCache cache;
	
	public static QuadTreeFactory createInstance(TileSource source, RasterLayer baseLayer, Layer[] layerList, double pixelScale) {
		if (instance != null)
			throw new IllegalStateException("QuadTreeFactory already exists!");
		instance = new QuadTreeFactory(source, baseLayer, layerList, pixelScale);
		return(instance);
	}
	
	public static QuadTreeFactory getInstance() {
		return(instance);
	}
	
	public static void destroy() {
		if (instance == null)
			return;
		instance.dispose();
		instance = null;
		System.gc();
	}

	/**
	 * Constructor
	 * 
	 * @param label
	 * @param source
	 * @param baseLayer
	 * @param layerList
	 * @param pixelScale
	 */
	protected QuadTreeFactory(TileSource source, RasterLayer baseLayer, Layer[] layerList, double pixelScale) {
		this.source = source;
		this.layerList = layerList;
		this.baseLayer = baseLayer;
		this.pixelScale = pixelScale;
		this.tileWidth = baseLayer.getTileWidth();
		this.tileLength = baseLayer.getTileLength();
		terrainWidth = baseLayer.getRasterWidth() * baseLayer.getPixelWidth() * pixelScale;
		terrainLength = baseLayer.getRasterLength() * baseLayer.getPixelLength() * pixelScale;
		missingFillValue = baseLayer.getFillValue();
		minZ = baseLayer.getMinimumValue()[0];

		bytesPerTile = (tileWidth*tileLength*14+2*tileWidth+2*tileLength)*4;
		for (int i = 0; i < layerList.length; ++i) {
			if (layerList[i] != null) {
				bytesPerTile += layerList[i].getBytesPerTile();
			}
		}
		
		cache = new QuadTreeCache();

		executor = Executors.newFixedThreadPool(5);
	}

	/**
	 * Dispose of resources
	 */
	public void dispose() {
		executor.shutdown();
		cache.clear();
	}

	/**
	 * Get a QuadTree
	 * 
	 * @param key
	 * @param p	QuadTree will be translated to this point
	 * @param pixelWidth
	 * @param pixelLength
	 * @param wait	wait for tile source to load the data
	 * @return
	 */
	public QuadTree getQuadTree(QuadKey key, ReadOnlyVector3 p, double pixelWidth, double pixelLength, boolean wait) {
		QuadTree quadTree = cache.getQuadTree(key.toString());
		if (quadTree == null)
			quadTree = createQuadTree(key, p, pixelWidth, pixelLength, wait);
		if (quadTree.getMesh() != null)
			return (quadTree);
		return(null);
	}

	/**
	 * Given the key, get a QuadTree
	 * 
	 * @param key
	 * @return
	 */
	public QuadTree getQuadTree(QuadKey key) {
		QuadTree quadTree = cache.getQuadTree(key.toString());
		if (quadTree == null) {
			Vector3 p = keyToTileCenter(key);
			double s = Math.pow(2, key.getLevel());
			double pixelWidth = (terrainWidth / tileWidth) / s;
			double pixelLength = (terrainLength / tileLength) / s;
			quadTree = createQuadTree(key, p, pixelWidth, pixelLength, true);
		}
		else if (quadTree.getMesh() != null)
			return(quadTree);
		else
			loadQuadTreeContents(quadTree);
		return (quadTree);
	}

	/**
	 * Given a key, get the offset from the parent center
	 * 
	 * @param key
	 * @return
	 */
	private Vector3 keyToTileCenter(QuadKey key) {
		Vector3 p = new Vector3();
		byte[] path = key.getPath();
		if (path.length < 1) {
			return (p);
		}
		double n = Math.pow(2, path.length);
		double w = terrainWidth / n;
		double l = terrainLength / n;
		int q = key.getQuadrant();
		switch (q) {
		case 1:
			p.set(p.getX() - w, p.getY() + l, 0);
			break;
		case 2:
			p.set(p.getX() + w, p.getY() + l, 0);
			break;
		case 3:
			p.set(p.getX() - w, p.getY() - l, 0);
			break;
		case 4:
			p.set(p.getX() + w, p.getY() - l, 0);
			break;
		}
		return (p);
	}

	/**
	 * Given a key, find the OpenGl coordinates relative to the center of the
	 * terrain
	 * 
	 * @param key
	 * @return
	 */
	private Vector3 keyToTestPointCenter(QuadKey qp) {
		Vector3 p = new Vector3();
		byte[] path = qp.getPath();
		if (path.length < 1) {
			return (p);
		}
		double w = terrainWidth/2;
		double l = terrainLength/2;
		for (int i = 0; i < path.length; ++i) {
			w /= 2;
			l /= 2;
			switch (path[i]) {
			case 1:
				p.set(p.getX() - w, p.getY() + l, 0);
				break;
			case 2:
				p.set(p.getX() + w, p.getY() + l, 0);
				break;
			case 3:
				p.set(p.getX() - w, p.getY() - l, 0);
				break;
			case 4:
				p.set(p.getX() + w, p.getY() - l, 0);
				break;
			}
		}
		return (p);
	}
	
	/**
	 * Indicate if a QuadTree with the give key has children.
	 * 
	 * @param quadKey
	 * @return have children
	 */
	public boolean childrenExist(QuadKey quadKey) {
		return(source.tileExists(quadKey+File.separator+"1"));
	}

	/**
	 * Get the 4 child QuadTrees of the given parent out of the cache.
	 * If not all 4 children are present in the cache, return null.
	 * 
	 * @param qp
	 * @param parent
	 * @param wait
	 * @return the QuadTrees or null if not all are present in the cache
	 */
	public QuadTree[] getQuadTreeChildren(QuadKey qp, QuadTree parent, boolean wait) {

		// load the quadtrees
		double pixelWidth = parent.pixelWidth / 2;
		double pixelLength = parent.pixelLength / 2;
		double xCenter = parent.pixelWidth * tileWidth / 4;
		double yCenter = parent.pixelLength * tileLength / 4;
		int count = 0;
		QuadTree[] qt = new QuadTree[4];
		qt[0] = getQuadTree(qp.createChild(1), new Vector3(-xCenter, yCenter, 0), pixelWidth, pixelLength, wait);
		if (qt[0] != null)
			count ++;
		qt[1] = getQuadTree(qp.createChild(2), new Vector3(xCenter, yCenter, 0), pixelWidth, pixelLength, wait);
		if (qt[1] != null)
			count ++;
		qt[2] = getQuadTree(qp.createChild(3), new Vector3(-xCenter, -yCenter, 0), pixelWidth, pixelLength, wait);
		if (qt[2] != null)
			count ++;
		qt[3] = getQuadTree(qp.createChild(4), new Vector3(xCenter, -yCenter, 0), pixelWidth, pixelLength, wait);
		if (qt[3] != null)
			count ++;
		if (count == 4)
			return(qt);
		else
			return(null);
	}

	private QuadTree createQuadTree(QuadKey key, ReadOnlyVector3 p, double pixelWidth, double pixelLength, boolean wait) {

		// create the quad tree tile and put it in the cache as a place holder
		// while we load the contents
		// this keeps us from starting another load operation for this tile
		final QuadTree qt = new QuadTree(key, p, pixelWidth, pixelLength, bytesPerTile);
		qt.createCornerPoints(keyToTestPointCenter(key), tileWidth, tileLength);
		cache.putQuadTree(key.toString(), qt);

		// load the quad tree mesh contents
		if (wait) {
			loadQuadTreeContents(qt);
		} else {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					Thread.yield();
					loadQuadTreeContents(qt);
				}
			};
			executor.execute(runnable);
		}
		return(qt);
	}

	private void loadQuadTreeContents(QuadTree qt) {
		// load the mesh
		QuadTreeMesh mesh = createMesh(qt.getKey(), qt.pixelWidth, qt.pixelLength);
		if (mesh == null) {
			return;
		}

		// load the image layers as textures
		TextureState textureState = new TextureState();
		for (int i = 0; i < layerList.length; ++i) {
			Texture texture = null;
			if (layerList[i] != null) {
				if (mesh.isEmpty()) {
					// this is an empty quad tree tile (just for padding)
					texture = getEmptyTexture();
				}
				else {
					texture = getTexture(qt.getKey(), mesh, i, null);
				}
			}
			else if (i == 0)
				texture = getEmptyTexture();
			textureState.setTexture(texture, i);
		}
		textureState.setEnabled(layersEnabled);
		mesh.setRenderState(textureState);
		qt.setMesh(mesh, minZ);
	}

	private Texture getEmptyTexture() {
		if (emptyTexture == null) {
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(16);
			for (int i = 0; i < 16; ++i) {
				byteBuffer.put((byte) 0);
			}
			byteBuffer.flip();
			ImageDataFormat format = ImageDataFormat.Luminance;
			PixelDataType type = PixelDataType.UnsignedByte;
			ArrayList<ByteBuffer> list = new ArrayList<ByteBuffer>(1);
			list.add(byteBuffer);
			Image image = new Image(format, type, 4, 4, list, null);
			emptyTexture = new SharedTexture2D();
			TextureKey tKey = TextureKey.getKey(null, false, TextureStoreFormat.GuessNoCompressedFormat,
				"DertEmptyTexture", Texture.MinificationFilter.BilinearNoMipMaps);
			emptyTexture.setTextureKey(tKey);
			emptyTexture.setImage(image);
			emptyTexture.setMinificationFilter(Texture.MinificationFilter.BilinearNoMipMaps);
			emptyTexture.setTextureStoreFormat(ImageUtils.getTextureStoreFormat(tKey.getFormat(), image));
			emptyTexture.setApply(Texture2D.ApplyMode.Modulate);
			emptyTexture.setHasBorder(false);
			emptyTexture.setWrap(Texture.WrapMode.EdgeClamp);
			emptyTexture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
		}
		return (emptyTexture);
	}

	private Texture getTexture(QuadKey key, QuadTreeMesh mesh, int tUnit, Texture texture) {
		if (layerList[tUnit] == null) {
			return (null);
		}
		texture = layerList[tUnit].getTexture(key, mesh, texture);
		if (texture == null) {
			return (null);
		}

		texture.setApply(Texture2D.ApplyMode.Modulate);
		texture.setHasBorder(false);
		texture.setWrap(Texture.WrapMode.EdgeClamp);
		texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
		return (texture);
	}

	private synchronized QuadTreeMesh createMesh(QuadKey key, double pixelWidth, double pixelLength) {

		// Get the base layer tile data
		QuadTreeTile tile = baseLayer.getTile(key);
		if (tile == null)
			return (null);
		

		int dataSize = tile.columns * tile.rows;
		FloatBuffer data = tile.raster.asFloatBuffer();

		// create vertex and color buffers
		FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(dataSize * 3);

		boolean empty = true;
		double width = pixelWidth * (tile.columns-1);
		double length = pixelLength * (tile.rows-1);
		
		// fill vertexBuffer
		int k = 0;
		float y = (float) length / 2;
		for (int r = 0; r < tile.rows; ++r) {
			float x = -(float) width / 2;
			for (int c = 0; c < tile.columns; ++c) {
				float z = data.get(k);
				if (!Float.isNaN(z))
					empty = false;
				vertexBuffer.put(x).put(y).put( z * (float)pixelScale);
				k++;
				x += pixelWidth;
			}
			y -= pixelLength;
		}
		vertexBuffer.flip();
		
		QuadTreeMesh mesh = new QuadTreeMesh("_mesh_"+key, tile.columns, tile.rows, pixelWidth, pixelLength);

		// all NaNs, create "empty mesh"
		if (empty) {
			mesh.empty = true;
			mesh.setMeshData(new TileMeshData(vertexBuffer, tile.columns, tile.rows, missingFillValue, missingColor));
		}
		else {
			mesh.setMeshData(new TileMeshData(vertexBuffer, pixelScale, tile.columns, tile.rows, missingFillValue, surfaceColor, missingColor));
		}

		mesh.getSceneHints().setNormalsMode(NormalsMode.NormalizeIfScaled);
		CullState cullState = new CullState();
		cullState.setCullFace(CullState.Face.Back);
		cullState.setEnabled(true);
		mesh.setRenderState(cullState);
		mesh.setModelBound(new BoundingBox());
		mesh.updateModelBound();
		return (mesh);
	}

	/**
	 * Set the surface color for all QuadTrees
	 * 
	 * @param surfaceColor
	 */
	public void setSurfaceColor(Color color) {
		surfaceColor = UIUtil.colorToFloatArray(color);
		cache.updateSurfaceColor(surfaceColor);
	}
}
