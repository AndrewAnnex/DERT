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

package gov.nasa.arc.dert.ui;

import gov.nasa.arc.dert.util.ColorMap;
import gov.nasa.arc.dert.view.Console;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.text.DecimalFormat;

/**
 * Provides a Canvas with a palette of colors in a vertical or horizontal bar
 * drawn with Java 2D.
 */
public class Palette extends Canvas {

	// Palette will be vertical
	private boolean vertical;

	// Formatter for tick marks
	private DecimalFormat formatter;

	// Colors
	private Color[] color;

	// Values associated with the colors
	private double[] value;

	// Tick mark text
	private String[] tickStr;

	// Dimensions
//	private int width, height;
	private String bigTick;

	// Draw as gradient
	private boolean isGradient;

	/**
	 * Constructor
	 * 
	 * @param colorMap
	 * @param vertical
	 */
	public Palette(ColorMap colorMap, boolean vertical) {
		super();
		this.vertical = vertical;
		build(colorMap);
	}

	/**
	 * Build the palette based on the given color map
	 * 
	 * @param colorMap
	 */
	public void build(ColorMap colorMap) {
		value = colorMap.getValues();
		isGradient = colorMap.isGradient();
		
		// determine fractional digits for tick marks
		double dMin = Double.MAX_VALUE;
		for (int i = 1; i < value.length; ++i) {
			dMin = Math.min(dMin, value[i] - value[i - 1]);
		}
		double fracDigits = 0;
		if (dMin != 0)
			fracDigits = Math.log10(dMin);
		else
			Console.println("One or more color map intervals is 0. Labels will have no fractional digits.");
		String str = "0";
		if (fracDigits > 0) {
			fracDigits = 0;
		} else {
			str += ".";
			fracDigits = Math.ceil(Math.abs(fracDigits));
			for (int i = 0; i < fracDigits; ++i) {
				str += "0";
			}
		}
		formatter = new DecimalFormat(str);
		
		
		color = colorMap.getColors();
		tickStr = new String[value.length];
		bigTick = "";
		for (int i = 0; i < value.length; ++i) {
			tickStr[i] = formatter.format(value[i]);
			if (tickStr[i].length() > bigTick.length()) {
				bigTick = tickStr[i];
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		doPaint(g);
	}

	protected void doPaint(Graphics g) {
		if (getWidth() == 0) {
			return;
		}
		FontMetrics fm = g.getFontMetrics();
		if (fm == null) {
			return;
		}
		Graphics2D g2d = (Graphics2D) g;
		Color textBackground = g2d.getBackground();
		Color textForeground = Color.black;
		int x0 = 0, x1 = 0, y0 = 0, y1 = 0;
		int wid = (int) Math.round(fm.stringWidth(bigTick) * 1.5);
		int hh = fm.getAscent();
		int hv = fm.getAscent() / 2;
		int hgt = getHeight()/(color.length+1);

		Rectangle colorRect = null;
		if (vertical) {
			colorRect = new Rectangle(0, 0, 40, getHeight()/(color.length+1));
			x1 = 60 + fm.stringWidth(bigTick);
			colorRect.height ++;
		} else {
			x0 = wid / 2;
			y0 = 0;
			y1 = 30;
			colorRect = new Rectangle(x0, y0, wid+1, 20);
		}

		// draw tick marks
		g2d.setPaint(textForeground);
		g2d.setBackground(textBackground);
		for (int i = 0; i < tickStr.length; ++i) {
			if (vertical) {
				y0 = (color.length - i) * hgt;
				int x = x1 - fm.stringWidth(tickStr[i]);
				g2d.drawLine(x0, y0, 50, y0);
				g2d.drawString(tickStr[i], x, y0 + hv);
			} else {
				x1 = x0;
				g2d.drawLine(x0, y0, x1, y1);
				int x = x1 - fm.stringWidth(tickStr[i]) / 2;
				g2d.drawString(tickStr[i], x, y1 + hh);
				x0 += wid;
			}

		}

		// draw color bars
		for (int i = 0; i < color.length-1; ++i) {
			if (vertical) {
				colorRect.y = (color.length-1 - i) * hgt;
				if (isGradient) {
					GradientPaint gp = new GradientPaint(colorRect.x, colorRect.y, color[i+1], colorRect.x,
						colorRect.y + colorRect.height, color[i]);
					g2d.setPaint(gp);
					g2d.fill(colorRect);
				} else {
					g2d.setBackground(color[i]);
					g2d.setPaint(color[i]);
					g2d.fill(colorRect);
					g2d.draw(colorRect);
				}
			} else {
				if (isGradient) {
					GradientPaint gp = new GradientPaint(colorRect.x, colorRect.y, color[i], colorRect.x
						+ colorRect.width, colorRect.y, color[i+1]);
					g2d.setPaint(gp);
					g2d.fill(colorRect);
					colorRect.x += wid;
				} else {
					g2d.setBackground(color[i]);
					g2d.setPaint(color[i]);
					g2d.fill(colorRect);
					// g2d.draw(colorRect);
					colorRect.x += wid;
				}
			}
		}
		g2d.setPaint(textForeground);
		g2d.setBackground(textBackground);
	}
}
