/* 
 * Copyright (C) 2008  Gilleain Torrance <gilleain.torrance@gmail.com>
 *               2009  Mark Rijnbeek <mark_rynbeek@users.sourceforge.net>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All I ask is that proper credit is given for my work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.controller;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.controller.edit.IEdit;
import org.openscience.cdk.controller.edit.OptionalUndoEdit;
import org.openscience.cdk.controller.edit.Rotate;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.selection.IChemObjectSelection;
import org.openscience.cdk.tools.ILoggingTool;
import org.openscience.cdk.tools.LoggingToolFactory;

/**
 * Module to rotate a selection of atoms (and their bonds).
 *
 * @cdk.module controlbasic
 */
public class RotateModule extends ControllerModuleAdapter {

    private ILoggingTool logger =
        LoggingToolFactory.createLoggingTool(RotateModule.class);

    private double rotationAngle;
    private boolean selectionMade = false;
    private IChemObjectSelection selection;
    private Point2d rotationCenter;
    private Vector2d[] startCoordsRelativeToRotationCenter;
    private Map<IAtom, Point2d[]> atomCoordsMap;
    private boolean rotationPerformed;

    /**
     * Constructor 
     * @param chemModelRelay
     */
    public RotateModule(IChemModelRelay chemModelRelay) {
        super(chemModelRelay);
        logger.debug("constructor");
    }

    /**
     * Initializes possible rotation. Determines rotation center and stores 
     * coordinates of atoms to be rotated. These stored coordinates are relative 
     * to the rotation center.
     */
    public void mouseClickedDown(Point2d worldCoord) {
        logger.debug("rotate mouseClickedDown, initializing rotation");
        rotationCenter = null;
        selection = super.chemModelRelay.getRenderer().getRenderer2DModel()
                .getSelection();

        if (   selection == null 
            ||!selection.isFilled()
            || selection.getConnectedAtomContainer() == null
            || selection.getConnectedAtomContainer().getAtomCount()==0) {

            /*
             * Nothing selected- return. Dragging the mouse will not result in
             * any rotation logic.
             */
            logger.debug("Nothing selected for rotation");
            selectionMade = false;
            return;
        
        } else {
            
            rotationAngle = 0.0;
            selectionMade = true;

            /* Keep original coordinates for possible undo/redo */
            rotationPerformed = false;
            atomCoordsMap = new HashMap<IAtom, Point2d[]>();
            for (IAtom atom : selection.getConnectedAtomContainer().atoms()) {
                Point2d[] coordsforatom = new Point2d[2];
                coordsforatom[1] = atom.getPoint2d();
                atomCoordsMap.put(atom, coordsforatom);
            }

            /*
             * Determine rotationCenter as the middle of a region defined by
             * min(x,y) and max(x,y) of coordinates of the selected atoms.
             */
            IAtomContainer selectedAtoms = 
                selection.getConnectedAtomContainer();

            
            Double upperX = null, lowerX = null, upperY = null, lowerY = null;
            for (int i = 0; i < selectedAtoms.getAtomCount(); i++) {
                if (upperX == null) {
                    upperX = selectedAtoms.getAtom(i).getPoint2d().x;
                    lowerX = upperX;
                    upperY = selectedAtoms.getAtom(i).getPoint2d().y;
                    lowerY = selectedAtoms.getAtom(i).getPoint2d().y;
                } else {
                    double currX = selectedAtoms.getAtom(i).getPoint2d().x;
                    if (currX > upperX)
                        upperX = currX;
                    if (currX < lowerX)
                        lowerX = currX;

                    double currY = selectedAtoms.getAtom(i).getPoint2d().y;
                    if (currY > upperY)
                        upperY = currY;
                    if (currY < lowerY)
                        lowerY = currY;
                }
            }
            rotationCenter = new Point2d();
            rotationCenter.x = (upperX + lowerX) / 2;
            rotationCenter.y = (upperY + lowerY) / 2;
            logger.debug("rotationCenter " 
                    + rotationCenter.x + " "
                    + rotationCenter.y);

            /* Store the original coordinates relative to the rotation center.
             * These are necessary to rotate around the center of the
             * selection rather than the draw center. */
            startCoordsRelativeToRotationCenter = new Vector2d[selectedAtoms
                    .getAtomCount()];
            for (int i = 0; i < selectedAtoms.getAtomCount(); i++) {
                Vector2d relativeAtomPosition = new Vector2d();
                relativeAtomPosition.sub( selectedAtoms.getAtom( i ).getPoint2d(), rotationCenter );
                startCoordsRelativeToRotationCenter[i] = relativeAtomPosition;
            }
        }
    }

    /**
     * On mouse drag, actual rotation around the center is done
     */
    public void mouseDrag(Point2d worldCoordFrom, Point2d worldCoordTo) {

        if (selectionMade) {
            double partAngle = 0;
            rotationPerformed=true;
            /*
             * Determine the quadrant the user is currently in, relative to the
             * rotation center.
             */
            int quadrant = 0;
            if ((worldCoordFrom.x >= rotationCenter.x))
                if ((worldCoordFrom.y <= rotationCenter.y))
                    quadrant = 1; // 12 to 3 o'clock
                else
                    quadrant = 2; // 3 to 6 o'clock
            else if ((worldCoordFrom.y <= rotationCenter.y))
                quadrant = 4; // 9 to 12 o'clock
            else
                quadrant = 3; // 6 to 9 o'clock

            /*
             * The quadrant and the drag combined determine in which direction
             * the rotation will be done. For example, dragging in direction
             * left/down in quadrant 4 means rotating counter clockwise.
             */
            switch (quadrant) {
            case 1:
                partAngle += (worldCoordTo.x - worldCoordFrom.x)
                        + (worldCoordTo.y - worldCoordFrom.y);
                break;
            case 2:
                partAngle += (worldCoordFrom.x - worldCoordTo.x)
                        + (worldCoordTo.y - worldCoordFrom.y);
                break;
            case 3:
                partAngle += (worldCoordFrom.x - worldCoordTo.x)
                        + (worldCoordFrom.y - worldCoordTo.y);
                break;
            case 4:
                partAngle += (worldCoordTo.x - worldCoordFrom.x)
                        + (worldCoordFrom.y - worldCoordTo.y);
                break;
            }
            rotationAngle+=partAngle;
            IEdit edit = Rotate.rotate( atomCoordsMap.keySet(),
                                        partAngle,
                                        rotationCenter );
            chemModelRelay.execute( OptionalUndoEdit.wrap( edit, false ) );
        }
    }
    
    /**
     * After the rotation (=mouse up after drag), post the undo/redo information
     * with the old and the new coordinates
     */
    public void mouseClickedUp(Point2d worldCoord) {
        if(rotationPerformed && atomCoordsMap!=null) {
            IEdit edit = Rotate.rotate( atomCoordsMap.keySet(),
                                                   rotationAngle,
                                                   rotationCenter );
            chemModelRelay.execute( OptionalUndoEdit.wrap( edit, true ) );
        }
    }

    
    public void setChemModelRelay(IChemModelRelay relay) {
        this.chemModelRelay = relay;
    }

    public String getDrawModeString() {
        return "Rotate";
    }

}
