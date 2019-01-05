/**

DERT is a viewer for digital terrain models created from data collected during NASA missions.

DERT is Released in under the NASA Open Source Agreement (NOSA) found in the “LICENSE” folder where you
downloaded DERT.

DERT includes 3rd Party software. The complete copyright notice listing for DERT is:

Copyright © 2015 United States Government as represented by the Administrator of the National Aeronautics and
Space Administration.  No copyright is claimed in the United States under Title 17, U.S.Code. All Other Rights
Reserved.

Desktop Exploration of Remote Terrain (DERT) could not have been written without the aid of a number of free,
open source libraries. These libraries and their notices are listed below. Find the complete third party license
listings in the separate “DERT Third Party Licenses” pdf document found where you downloaded DERT in the
LICENSE folder.
 
JogAmp Ardor3D Continuation
Copyright © 2008-2012 Ardor Labs, Inc.
 
JogAmp
Copyright 2010 JogAmp Community. All rights reserved.
 
JOGL Portions Sun Microsystems
Copyright © 2003-2009 Sun Microsystems, Inc. All Rights Reserved.
 
JOGL Portions Silicon Graphics
Copyright © 1991-2000 Silicon Graphics, Inc.
 
Light Weight Java Gaming Library Project (LWJGL)
Copyright © 2002-2004 LWJGL Project All rights reserved.
 
Tile Rendering Library - Brian Paul 
Copyright © 1997-2005 Brian Paul. All Rights Reserved.
 
OpenKODE, EGL, OpenGL , OpenGL ES1 & ES2
Copyright © 2007-2010 The Khronos Group Inc.
 
Cg
Copyright © 2002, NVIDIA Corporation
 
Typecast - David Schweinsberg 
Copyright © 1999-2003 The Apache Software Foundation. All rights reserved.
 
PNGJ - Herman J. Gonzalez and Shawn Hartsock
Copyright © 2004 The Apache Software Foundation. All rights reserved.
 
Apache Harmony - Open Source Java SE
Copyright © 2006, 2010 The Apache Software Foundation.
 
Guava
Copyright © 2010 The Guava Authors
 
GlueGen Portions
Copyright © 2010 JogAmp Community. All rights reserved.
 
GlueGen Portions - Sun Microsystems
Copyright © 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 
SPICE
Copyright © 2003, California Institute of Technology.
U.S. Government sponsorship acknowledged.
 
LibTIFF
Copyright © 1988-1997 Sam Leffler
Copyright © 1991-1997 Silicon Graphics, Inc.
 
PROJ.4
Copyright © 2000, Frank Warmerdam

LibJPEG - Independent JPEG Group
Copyright © 1991-2018, Thomas G. Lane, Guido Vollbeding
 

Disclaimers

No Warranty: THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY KIND,
EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
THAT THE SUBJECT SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY
WARRANTY THAT THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE. THIS AGREEMENT
DOES NOT, IN ANY MANNER, CONSTITUTE AN ENDORSEMENT BY GOVERNMENT AGENCY OR ANY
PRIOR RECIPIENT OF ANY RESULTS, RESULTING DESIGNS, HARDWARE, SOFTWARE PRODUCTS OR
ANY OTHER APPLICATIONS RESULTING FROM USE OF THE SUBJECT SOFTWARE.  FURTHER,
GOVERNMENT AGENCY DISCLAIMS ALL WARRANTIES AND LIABILITIES REGARDING THIRD-PARTY
SOFTWARE, IF PRESENT IN THE ORIGINAL SOFTWARE, AND DISTRIBUTES IT "AS IS."

Waiver and Indemnity:  RECIPIENT AGREES TO WAIVE ANY AND ALL CLAIMS AGAINST THE UNITED
STATES GOVERNMENT, ITS CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR
RECIPIENT.  IF RECIPIENT'S USE OF THE SUBJECT SOFTWARE RESULTS IN ANY LIABILITIES,
DEMANDS, DAMAGES, EXPENSES OR LOSSES ARISING FROM SUCH USE, INCLUDING ANY DAMAGES
FROM PRODUCTS BASED ON, OR RESULTING FROM, RECIPIENT'S USE OF THE SUBJECT SOFTWARE,
RECIPIENT SHALL INDEMNIFY AND HOLD HARMLESS THE UNITED STATES GOVERNMENT, ITS
CONTRACTORS AND SUBCONTRACTORS, AS WELL AS ANY PRIOR RECIPIENT, TO THE EXTENT
PERMITTED BY LAW.  RECIPIENT'S SOLE REMEDY FOR ANY SUCH MATTER SHALL BE THE IMMEDIATE,
UNILATERAL TERMINATION OF THIS AGREEMENT.

**/

package gov.nasa.arc.dert.render;

import gov.nasa.arc.dert.camera.BasicCamera;

import java.nio.ByteBuffer;

import com.ardor3d.image.Texture;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.jogl.JoglTextureRenderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.geom.BufferUtils;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;

/**
 * This is an extension of Ardor3D's JoglTextureRenderer that filters objects
 * that shouldn't cast a shadow and can save a copy of the color buffer.
 *
 */

public class BasicTextureRenderer extends JoglTextureRenderer {

	// The camera used for rendering the texture
	protected BasicCamera basicCamera;

	// Flags
	protected boolean isShadow, saveRGBA;

	// Memory used
	protected int size;

	// RGBA buffer
	protected ByteBuffer rgbaBuffer;

	public BasicTextureRenderer(final int width, final int height, final int depthBits, final int samples,
		final Renderer parentRenderer, final ContextCapabilities caps) {
		super(width, height, depthBits, samples, parentRenderer, caps);

		size = _width * _height;
		basicCamera = new BasicCamera(_width, _height, 90, _width / (double) _height);
		basicCamera.setFrustum(1.0f, 1000.0f, -0.50f, 0.50f, 0.50f, -0.50f);
		final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
		final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
		final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
		final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
		basicCamera.setFrame(loc, left, up, dir);
	}

	@Override
	public Camera getCamera() {
		return (basicCamera);
	}

	/**
	 * This texture render is in shadow
	 * 
	 * @param isShadow
	 */
	public void setIsShadow(boolean isShadow) {
		this.isShadow = isShadow;
	}
	
	public void clear(final Texture tex, final int clear) {
//        final GL gl = GLContext.getCurrentGL();
//        gl.glClearColor(1, 1, 1, 1);

        setupForSingleTexDraw(tex);

        if (_samples > 0 && _supportsMultisample) {
            setMSFBO();
        }

        switchCameraIn(clear);
        switchCameraOut();

        if (_samples > 0 && _supportsMultisample) {
            blitMSFBO();
        }

        takedownForSingleTexDraw(tex);
        
//        final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
//        gl.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
	}

	@Override
	public void render(final Spatial toDraw, final Texture tex, final int clear) {
		if (toDraw == null) {
			return;
		}
		if (isShadow && !toDraw.getSceneHints().isCastsShadows()) {
			return;
		}
		((JoglRendererDouble) _parentRenderer).setInShadow(isShadow);
		_parentRenderer.clearBuffers(Renderer.BUFFER_COLOR_AND_DEPTH);
		super.render(toDraw, tex, clear);
		((JoglRendererDouble) _parentRenderer).setInShadow(false);
	}

	@Override
	protected void switchCameraOut() {
		_parentRenderer.flushFrame(false);
		if (saveRGBA) {
			saveRGBABuffer();
		}
		super.switchCameraOut();
	}

	/**
	 * Save the contents of the color buffer after rendering
	 * 
	 * @param saveRGBA
	 */
	public void setSaveRGBA(boolean saveRGBA) {
		this.saveRGBA = saveRGBA;
	}

	protected void saveRGBABuffer() {
		final GL gl = GLU.getCurrentGL();
		if (rgbaBuffer == null) {
			rgbaBuffer = BufferUtils.createByteBuffer(size * 4);
		}
		gl.glReadPixels(0, 0, _width, _height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, rgbaBuffer);
		rgbaBuffer.rewind();
		saveRGBA = false;
	}

	/**
	 * Get the saved color buffer
	 * 
	 * @return
	 */
	public ByteBuffer getRGBABuffer() {
		return (rgbaBuffer);
	}
}
