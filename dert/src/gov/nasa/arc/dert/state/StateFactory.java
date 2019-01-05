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

package gov.nasa.arc.dert.state;

import gov.nasa.arc.dert.util.StateUtil;

import java.util.Map;

public class StateFactory {
	
	public static enum DefaultState {AnimationState, ColorBarsState, ConsoleState, HelpState, LightingState, LightPositionState, MapElementsState, MarbleState, SurfaceAndLayersState, ViewpointState, WorldState}
	
	public StateFactory() {
	}
	
	public State createState(DefaultState key) {
		return(createState(key.toString(), null));
	}
	
	public State createState(String key, Map<String,Object> map) {
		
		DefaultState sName = DefaultState.valueOf(key);
		
		if (sName != null) {
			switch (sName) {
			case AnimationState:
				if (map == null)
					return(new AnimationState());
				return(new AnimationState(map));
			case ColorBarsState:
				if (map == null)
					return(new ColorBarsState());
				return(new ColorBarsState(map));
			case ConsoleState:
				if (map == null)
					return(new ConsoleState());
				return(new ConsoleState(map));
			case HelpState:
				if (map == null)
					return(new HelpState());
				return(new HelpState(map));
			case LightingState:
				if (map == null)
					return(new LightingState());
				return(new LightingState(map));
			case LightPositionState:
				if (map == null)
					return(new LightPositionState());
				return(new LightPositionState(map));
			case MapElementsState:
				if (map == null)
					return(new MapElementsState());
				return(new MapElementsState(map));
			case MarbleState:
				if (map == null)
					return(new MarbleState());
				return(new MarbleState(map));
			case SurfaceAndLayersState:
				if (map == null)
					return(new SurfaceAndLayersState());
				return(new SurfaceAndLayersState(map));
			case ViewpointState:
				if (map == null)
					return(new ViewpointState());
				return(new ViewpointState(map));
			case WorldState:
				if (map == null)
					return(new WorldState());
				return(new WorldState(map));
			}
		}
		
		return(null);
	}		

	public MapElementState createMapElementState(Map<String,Object> map) {
		if (map == null)
			return(null);
		String str = StateUtil.getString(map, "MapElementType", null);
		if (str != null) {
			try {
				MapElementState.Type type = MapElementState.Type.valueOf(str);
				if (type != null)
					switch (type) {
					case Placemark:
						return(new PlacemarkState(map));
					case Figure:
						return(new FigureState(map));
					case Billboard:
						return(new ImageBoardState(map));
					case Model:
						return(new ModelState(map));
					case FeatureSet:
						return(new FeatureSetState(map));
					case Path:
						return(new PathState(map));
					case Plane:
						return(new PlaneState(map));
					case CartesianGrid:
						return(new GridState(map));
					case RadialGrid:
						return(new GridState(map));
					case Profile:
						return(new ProfileState(map));
					case FieldCamera:
						return(new FieldCameraState(map));
					case Waypoint:
						return(new WaypointState(map));
					case Marble:
						return(new MarbleState(map));
					case Scale:
						return(new ScaleBarState(map));
					case Feature:
						break;
					}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return(null);
	}

}
