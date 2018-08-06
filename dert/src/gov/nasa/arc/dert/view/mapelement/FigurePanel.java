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
 
Tile Rendering Library - Brain Paul 
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

package gov.nasa.arc.dert.view.mapelement;

import gov.nasa.arc.dert.landscape.Landscape;
import gov.nasa.arc.dert.scene.MapElement;
import gov.nasa.arc.dert.scene.landmark.Figure;
import gov.nasa.arc.dert.scenegraph.Shape;
import gov.nasa.arc.dert.scenegraph.Shape.ShapeType;
import gov.nasa.arc.dert.ui.DoubleSpinner;
import gov.nasa.arc.dert.ui.DoubleTextField;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

/**
 * Provides controls for setting options for figures.
 *
 */
public class FigurePanel extends MapElementBasePanel {

	// Controls
	private DoubleTextField sizeText;
	private JLabel sizeLabel;
	private DoubleSpinner azSpinner, tiltSpinner;

	// Figure
	private Figure figure;

	// surface normal
	private JCheckBox surfaceButton, autoScaleButton;

	// select shape
	private JComboBox shapeCombo;

	/**
	 * Constructor
	 * 
	 * @param parent
	 */
	public FigurePanel(MapElement mapElement) {
		super(mapElement);
	}

	@Override
	protected void addFields(ArrayList<Component> compList) {
		super.addFields(compList);

		compList.add(new JLabel("Shape", SwingConstants.RIGHT));
		shapeCombo = new JComboBox(ShapeType.values());
		shapeCombo.setToolTipText("select figure shape");
		shapeCombo.setSelectedIndex(3);
		shapeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ShapeType shape = (ShapeType) shapeCombo.getSelectedItem();
				figure.setShape(shape);
				autoScaleButton.setSelected(figure.isAutoScale());
				autoScaleButton.setEnabled(Shape.SCALABLE[shape.ordinal()]);
				if (Shape.SCALABLE[shape.ordinal()]) {
					sizeLabel.setForeground(Color.BLACK);
					sizeText.setEnabled(true);
					sizeText.setValue(figure.getSize());
				}
				else {
					sizeLabel.setForeground(Color.GRAY);
					sizeText.setEnabled(false);
					sizeText.setText("");
				}
			}
		});
		compList.add(shapeCombo);

		sizeLabel = new JLabel("Size", SwingConstants.RIGHT);
		compList.add(sizeLabel);
		sizeText = new DoubleTextField(8, Figure.defaultSize, true, Landscape.format) {
			@Override
			protected void handleChange(double value) {
				if (Double.isNaN(value)) {
					return;
				}
				figure.setSize(value);
			}
		};
		compList.add(sizeText);
		compList.add(new JLabel("Autoscale", SwingConstants.RIGHT));
		autoScaleButton = new JCheckBox("enabled");
		autoScaleButton.setToolTipText("maintain size with change in viewpoint");
		autoScaleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				figure.setAutoScale(autoScaleButton.isSelected());
			}
		});
		compList.add(autoScaleButton);

		compList.add(new JLabel("Azimuth", SwingConstants.RIGHT));
		azSpinner = new DoubleSpinner(0, 0, 359, 1, true) {
			@Override
			public void stateChanged(ChangeEvent event) {
				super.stateChanged(event);
				double azimuth = ((Double) azSpinner.getValue());
				figure.setAzimuth(azimuth);
			}
		};
		azSpinner.setToolTipText("rotate figure around vertical axis");
		compList.add(azSpinner);

		compList.add(new JLabel("Tilt", SwingConstants.RIGHT));
		tiltSpinner = new DoubleSpinner(0, -90, 90, 1, true) {
			@Override
			public void stateChanged(ChangeEvent event) {
				super.stateChanged(event);
				double tilt = ((Double) tiltSpinner.getValue());
				figure.setTilt(tilt);
			}
		};
		tiltSpinner.setToolTipText("rotate figure around horizontal axis");
		compList.add(tiltSpinner);

		compList.add(new JLabel("Surface Normal", SwingConstants.RIGHT));
		surfaceButton = new JCheckBox("visible");
		surfaceButton.setToolTipText("display the vector for the surface normal");
		surfaceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				figure.setSurfaceNormalVisible(surfaceButton.isSelected());
			}
		});
		compList.add(surfaceButton);
	}

	@Override
	public void setMapElement(MapElement mapElement) {
		super.setMapElement(mapElement);
		figure = (Figure) mapElement;
		setLocation(locationText, locLabel, figure.getTranslation());
		ShapeType shapeType = figure.getShapeType();
		if (Shape.SCALABLE[shapeType.ordinal()]) {
			sizeLabel.setForeground(Color.BLACK);
			sizeText.setEnabled(true);
			sizeText.setValue(figure.getSize());
		}
		else {
			sizeLabel.setForeground(Color.GRAY);
			sizeText.setEnabled(false);
			sizeText.setText("");
		}
		shapeCombo.setSelectedItem(shapeType);
		autoScaleButton.setSelected(figure.isAutoScale());
		autoScaleButton.setEnabled(Shape.SCALABLE[shapeType.ordinal()]);
		surfaceButton.setSelected(figure.isSurfaceNormalVisible());
		azSpinner.setValueNoChange(figure.getAzimuth());
		tiltSpinner.setValueNoChange(figure.getTilt());
	}

}
