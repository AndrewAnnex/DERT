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

import gov.nasa.arc.dert.Dert;
import gov.nasa.arc.dert.icon.Icons;
import gov.nasa.arc.dert.scene.World;
import gov.nasa.arc.dert.scene.landmark.Figure;
import gov.nasa.arc.dert.scene.tool.Path;
import gov.nasa.arc.dert.state.ConfigurationManager;
import gov.nasa.arc.dert.state.FieldCameraState;
import gov.nasa.arc.dert.state.FigureState;
import gov.nasa.arc.dert.state.GridState;
import gov.nasa.arc.dert.state.PathState;
import gov.nasa.arc.dert.state.PlacemarkState;
import gov.nasa.arc.dert.state.PlaneState;
import gov.nasa.arc.dert.state.ProfileState;
import gov.nasa.arc.dert.state.ScaleBarState;

import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Provides controls for setting Landmark preferences and adding Landmarks.
 *
 */
public class AddElementPanel extends JPanel {

	/**
	 * Constructor
	 */
	public AddElementPanel() {
		super();
		setLayout(new GridLayout(2, 6));
		
		JButton newButton = new JButton(Icons.getImageIcon("placemark.png"));
		newButton.setToolTipText("add Placemark");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				PlacemarkState pState = new PlacemarkState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(pState, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("figure.png"));
		newButton.setToolTipText("add 3D Figure");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				ReadOnlyVector3 normal = World.getInstance().getMarble().getNormal();
				FigureState fState = new FigureState(position, normal, Figure.defaultShapeType);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(fState, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("billboard.png"));
		newButton.setToolTipText("add Image Billboard");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				ImageBoardDialog dialog = new ImageBoardDialog((Dialog)getTopLevelAncestor(), position);
				dialog.open();
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("path.png"));
		newButton.setToolTipText("add Path");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				PathState state = new PathState(position);
				Path path = (Path) ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
				if (path != null)
					Dert.getWorldView().getScenePanel().getInputHandler().setPath(path);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("plane.png"));
		newButton.setToolTipText("add Plane");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				PlaneState state = new PlaneState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("cartesiangrid.png"));
		newButton.setToolTipText("add Cartesian Grid");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				GridState state = GridState.createCartesianGridState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("radialgrid.png"));
		newButton.setToolTipText("add Radial Grid");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				GridState state = GridState.createRadialGridState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("fieldcamera.png"));
		newButton.setToolTipText("add Camera");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				FieldCameraState state = new FieldCameraState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("profile.png"));
		newButton.setToolTipText("add Profile");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				ProfileState state = new ProfileState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("scale.png"));
		newButton.setToolTipText("add Scale");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				ScaleBarState state = new ScaleBarState(position);
				ConfigurationManager.getInstance().getCurrentConfiguration().addMapElementState(state, null);
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("lineset.png"));
		newButton.setToolTipText("add FeatureSet");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				FeatureSetDialog dialog = new FeatureSetDialog((Dialog)getTopLevelAncestor());
				dialog.open();
			}
		});
		add(newButton);

		newButton = new JButton(Icons.getImageIcon("model.png"));
		newButton.setToolTipText("add Model");
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				ReadOnlyVector3 position = World.getInstance().getMarble().getTranslation();
				ReadOnlyVector3 normal = World.getInstance().getMarble().getNormal();
				ModelDialog dialog = new ModelDialog((Dialog)getTopLevelAncestor(), position, normal);
				dialog.open();
			}
		});
		add(newButton);
	}
}
