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

package gov.nasa.arc.dert.view;

import gov.nasa.arc.dert.render.SceneCanvas;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Manages input events for the SceneCanvas.
 *
 */
public abstract class InputManager {

	protected int mouseX, mouseY;
	protected int width = 1, height = 1;
	protected KeyListener keyListener;

	public InputManager(final SceneCanvas canvas) {

		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent event) {
				mouseX = event.getX();
				mouseY = height - event.getY();
				if (event.getClickCount() == 2) {
					mouseDoubleClick(mouseX, mouseY, event.getButton());
				} else {
					mouseClick(mouseX, mouseY, event.getButton());
				}
			}

			@Override
			public void mouseEntered(MouseEvent event) {
			}

			@Override
			public void mouseExited(MouseEvent event) {
			}

			@Override
			public void mousePressed(MouseEvent event) {
				mouseX = event.getX();
				mouseY = height - event.getY();
				mousePress(mouseX, mouseY, event.getButton(), event.isControlDown(), event.isShiftDown());
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				mouseX = event.getX();
				mouseY = height - event.getY();
				mouseRelease(mouseX, mouseY, event.getButton());
			}
		});

		canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent event) {
				mouseScroll(event.getWheelRotation());
			}
		});

		canvas.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent event) {
				int x = event.getX();
				int y = height - event.getY();
				int button = event.getButton();
				button |= getButton(event.getModifiers());
				mouseMove(x, y, x - mouseX, y - mouseY, button, event.isControlDown(), event.isShiftDown());
				mouseX = x;
				mouseY = y;
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				int x = event.getX();
				int y = height - event.getY();
				mouseMove(x, y, x - mouseX, y - mouseY, 0, event.isControlDown(), event.isShiftDown());
				mouseX = x;
				mouseY = y;
			}
		});

		keyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
				final int keyCode = event.getKeyCode();
				final boolean shiftDown = event.isShiftDown();
				handleStep(keyCode, shiftDown);
			}

			@Override
			public void keyReleased(KeyEvent event) {
				// nothing here
			}

			@Override
			public void keyTyped(KeyEvent event) {
			}
		};
		canvas.addKeyListener(keyListener);
		
		canvas.getParent().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				setComponentSize(event.getComponent().getWidth(), event.getComponent().getHeight());
			}
		});
	}

	private void handleStep(int keyCode, boolean shiftDown) {
		switch (keyCode) {
		case KeyEvent.VK_UP:
		case KeyEvent.VK_KP_UP:
			stepUp(shiftDown);
			break;
		case KeyEvent.VK_DOWN:
		case KeyEvent.VK_KP_DOWN:
			stepDown(shiftDown);
			break;
		case KeyEvent.VK_LEFT:
		case KeyEvent.VK_KP_LEFT:
			stepLeft(shiftDown);
			break;
		case KeyEvent.VK_RIGHT:
		case KeyEvent.VK_KP_RIGHT:
			stepRight(shiftDown);
			break;
		}
	}

	public KeyListener getKeyListener() {
		return (keyListener);
	}

	protected final int getButton(int modifiers) {
		if ((modifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
			return (1);
		}
		if ((modifiers & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) {
			return (2);
		}
		if ((modifiers & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			return (3);
		}
		return (0);
	}
	
	public void setComponentSize(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public abstract void setCanvasScale(double xScale, double yScale);

	// Mouse was scrolled
	protected abstract void mouseScroll(int delta);

	// Mouse button was pressed
	protected abstract void mousePress(int x, int y, int mouseButton, boolean isControlled, boolean shiftDown);

	// Mouse button was released
	protected abstract void mouseRelease(int x, int y, int mouseButton);

	// Mouse was moved
	protected abstract void mouseMove(int x, int y, int dx, int dy, int mouseButton, boolean isControlled, boolean shiftDown);

	// Mouse button was clicked
	protected abstract void mouseClick(int x, int y, int mouseButton);

	// Mouse button was double-clicked
	protected abstract void mouseDoubleClick(int x, int y, int mouseButton);

	// Up arrow was pressed
	protected abstract void stepUp(boolean shiftDown);

	// Down arrow was pressed
	protected abstract void stepDown(boolean shiftDown);

	// Right arrow was pressed
	protected abstract void stepRight(boolean shiftDown);

	// Left arrow was pressed
	protected abstract void stepLeft(boolean shiftDown);
}
