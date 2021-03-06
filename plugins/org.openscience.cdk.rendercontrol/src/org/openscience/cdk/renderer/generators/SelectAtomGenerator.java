/* $Revision$ $Author$ $Date$
 *
 *  Copyright (C) 2008  Gilleain Torrance <gilleain.torrance@gmail.com>
 *
 *  Contact: cdk-devel@list.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.renderer.generators;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point2d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.OvalElement;
import org.openscience.cdk.renderer.elements.RectangleElement;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator.Shape;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.Scale;
import org.openscience.cdk.renderer.generators.parameter.AbstractGeneratorParameter;
import org.openscience.cdk.renderer.selection.IChemObjectSelection;
import org.openscience.cdk.renderer.selection.IncrementalSelection;

/**
 * @cdk.module rendercontrol
 */
public class SelectAtomGenerator implements IGenerator<IAtomContainer> {

    public static class SelectionShape extends
    AbstractGeneratorParameter<Shape> {
        public Shape getDefault() {
            return Shape.SQUARE;
        }
    }
    private IGeneratorParameter<Shape> selectionShape = new SelectionShape();
    
    /**
     * The radius on screen of the selection shape
     */
    public static class SelectionRadius extends 
                        AbstractGeneratorParameter<Double> {
        public Double getDefault() {
            return 2.0;
        }
    }
    
    private SelectionRadius selectionRadius = new SelectionRadius();
    
    public static class SelectionAtomColor extends 
                        AbstractGeneratorParameter<Color> {
        public Color getDefault() {
            return Color.LIGHT_GRAY;
        }
    }
    private SelectionAtomColor selectionAtomColor = new SelectionAtomColor();
    
    private boolean autoUpdateSelection = true;

    public SelectAtomGenerator() {}

    public IRenderingElement generate(IAtomContainer ac, RendererModel model) {
        Color selectionColor = 
            model.getParameter(SelectionAtomColor.class).getValue();
        Shape shape = selectionShape.getValue();
        IChemObjectSelection selection = model.getSelection();
        ElementGroup selectionElements = new ElementGroup();

        if(selection==null)
        	return selectionElements;
        if (this.autoUpdateSelection || selection.isFilled()) {
            double r = 
                model.getParameter(SelectionRadius.class).getValue() /
                model.getParameter(Scale.class).getValue();

            double d = 2 * r;
            IAtomContainer selectedAC = selection.getConnectedAtomContainer();
            if (selectedAC != null) {
                for (IAtom atom : selectedAC.atoms()) {
                    Point2d p = atom.getPoint2d();
                    IRenderingElement element;
                    switch (shape) {
                        case SQUARE:
                            element =
                                new RectangleElement(
                                    p.x - r, p.y - r, d, d, true,
                                    selectionColor);
                            break;
                        case OVAL:
                        default:
                            element = new OvalElement(
                                            p.x, p.y, d, false, selectionColor);
                    }
                    selectionElements.add(element);
                }
            }
        }

        if (selection instanceof IncrementalSelection) {
			IncrementalSelection sel = (IncrementalSelection) selection;
			if (!sel.isFinished())
				selectionElements.add(sel.generate(selectionColor));
		}
        return selectionElements;
    }

    public List<IGeneratorParameter<?>> getParameters() {
        return Arrays.asList(
                new IGeneratorParameter<?>[] {
                        selectionRadius,
                        selectionShape,
                        selectionAtomColor
                }
        );
    }
}
