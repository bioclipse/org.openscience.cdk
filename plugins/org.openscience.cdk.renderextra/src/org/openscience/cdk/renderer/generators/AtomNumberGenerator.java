/*  Copyright (C) 2009  Gilleain Torrance <gilleain@users.sf.net>
 *
 *  Contact: cdk-devel@lists.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *  All we ask is that proper credit is given for our work, which includes
 *  - but is not limited to - adding the above copyright notice to the beginning
 *  of your source code files, and to any copyright notice that you may distribute
 *  with programs based on this work.
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

import javax.vecmath.Vector2d;
import javax.vecmath.Point2d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.TextElement;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.Scale;
import org.openscience.cdk.renderer.generators.parameter.AbstractGeneratorParameter;

/**
 * @author maclean
 * @cdk.module renderextra
 */
public class AtomNumberGenerator implements IGenerator<IAtomContainer> {

    public static class AtomNumberTextColor extends
        AbstractGeneratorParameter<Color> {
        public Color getDefault() {
            return Color.BLACK;
        }
    }

    private IGeneratorParameter<Color> textColor = new AtomNumberTextColor();
    
    public static class WillDrawAtomNumbers extends
                        AbstractGeneratorParameter<Boolean> {
        public Boolean getDefault() {
            return Boolean.TRUE;
        }
    }
    private WillDrawAtomNumbers willDrawAtomNumbers =
    	new WillDrawAtomNumbers();

    Vector2d offset;

	public AtomNumberGenerator() {
	    offset = new Vector2d();
	}

	/**
	 * Allows for drawing the atom number offset from the atom position.
	 * @param offset vector in screen space.
	 */
	public AtomNumberGenerator(Vector2d offset) {
	    this.offset = new Vector2d(offset);
	}

	public IRenderingElement generate(IAtomContainer ac, RendererModel model) {
		ElementGroup numbers = new ElementGroup();
		if (!model.getParameter(WillDrawAtomNumbers.class).getValue())
		    return numbers;

		Vector2d offset = new Vector2d(this.offset.x,-this.offset.y);
		offset.scale( 1/model.getParameter(Scale.class).getValue() );

		int number = 1;
		for (IAtom atom : ac.atoms()) {
			Point2d p = new Point2d(atom.getPoint2d());
			p.add( offset );
			numbers.add(
					new TextElement(
						p.x, p.y, String.valueOf(number),
						textColor.getValue()
				    )
			);
			number++;
		}
		return numbers;
	}

    public List<IGeneratorParameter<?>> getParameters() {
        return Arrays.asList( new IGeneratorParameter<?>[] {
                textColor,
                willDrawAtomNumbers
            } 
        );
    }


}
