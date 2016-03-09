/*
   Copyright (C) 2009 MTG, Stefan Kersten <stefan.kersten@upf.edu>
   
   This program is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the Free
   Software Foundation; either version 2 of the License, or (at your option)
   any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT
   ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
   FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
   more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc., 59
   Temple Place - Suite 330, Boston, MA 02111-1307, USA.

   Music Technology Group
   Universitat Pompeu Fabra
   http://mtg.upf.edu/

*/

/* This is the base node type for all nodes that contain only information
   without visual semantics.
*/
X3DInfoNode : X3DChildNode {
}

/* This abstract interface is the basis for all metadata nodes. The interface
   is inherited by all metadata nodes.
*/
X3DMetadataObject : X3DNode {
	declareFields {
		super.declareFields;
		this.declareX3D(
			[ SFString.type , [in, out] , \name      , "" ],
			[ SFString.type , [in, out] , \reference , "" ]
		);
	}
	containerField {
	    ^\metadata
    }
}

/* This abstract node type indicates that the concrete nodes that are
    instantiated based on it may be used in children, addChildren, and
    removeChildren fields.
*/
X3DChildNode : X3DNode {
    var <parent;

	containerField {
	    ^\children
    }

    // PRIVATE
    pr_setParent { | aNode |
        if (parent.notNil and: { parent !== aNode }) {
            throw(X3DError("X3DChildNode already has a parent"))
        };
        parent = aNode;
    }
}

X3DScene : X3DGroupingNode {
    *x3dName { ^'Scene' }
}
