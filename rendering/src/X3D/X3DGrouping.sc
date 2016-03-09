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

/* This abstract node type is the basis for all node types that have bounds
specified as part of the definition.

The bboxCenter and bboxSize fields specify a bounding box that encloses the
grouping node's children. This is a hint that may be used for optimization
purposes. The results are undefined if the specified bounding box is smaller
than the actual bounding box of the children at any time. A default bboxSize
value, (-1, -1, -1), implies that the bounding box is not specified and, if
needed, is calculated by the browser. A description of the bboxCenter and
bboxSize fields is contained in 10.2.2 Bounding boxes.
*/
X3DBoundedObject : X3DNode {
    declareFields {
        super.declareFields;
        this.declareX3D(
            [ SFVec3f.type , [] , \bboxCenter , Vector3(0, 0, 0)    ],
            [ SFVec3f.type , [] , \bboxSize   , Vector3(-1, -1, -1) ]
        );
    }
}

/* This abstract node type indicates that concrete node types derived from it
contain children nodes and is the basis for all aggregation.

More details on the children, addChildren, and removeChildren fields can be
found in 10.2.1 Grouping and children node types.
*/
X3DGroupingNode : X3DChildNode { 
    var children;
    
    initX3DNode {
        super.initX3DNode;
        X3DFieldListener(this, \addChildren,    { | time, value | this.pr_addChildren(value)    });
        X3DFieldListener(this, \removeChildren, { | time, value | this.pr_removeChildren(value) });
        X3DFieldListener(this, \children,       { | time, value | this.pr_setChildren(value)    });
    }
    
    declareFields {
        super.declareFields;
        this.declareX3D(
            [ MFNode.type(X3DChildNode) , #[\in]      , \addChildren    , \default ],
            [ MFNode.type(X3DChildNode) , #[\in]      , \removeChildren , \default ],
            [ MFNode.type(X3DChildNode) , #[\in,\out] , \children       , []       ]
        );
        this.mixin(X3DBoundedObject);
    }

    children {
        if (children.isNil) {
            this.pr_setChildren(#[]);
        };
        ^children
    }
    children_ { | aNodeArray |
        this.put(\children, aNodeArray);
    }
    addChild { | aNode |
        this.put(\addChildren, [aNode]);
    }
    removeChild { | aNode |
        this.put(\removeChildren, [aNode]);
    }
    
    // PRIVATE
    pr_setChildren { | array |
        children = OrderedIdentitySet.with(*array);
    }
    pr_addChildren { | array |
        array.do(this.pr_addChild(_));
    }
    pr_addChild { | aNode |
        aNode.pr_setParent(this);
        this.children.add(aNode);
    }
    pr_removeChildren { | array |
        array.do(this.pr_removeChild(_));
    }
    pr_removeChild { | aNode |
        this.children.remove(aNode);
        aNode.pr_setParent(nil);
    }
}

/* A Group node contains children nodes without introducing a new
transformation. It is equivalent to a Transform node containing an identity
transform.

More details on the children, addChildren, and removeChildren fields can be
found in 10.2.1 Grouping and children node types.

The bboxCenter and bboxSize fields specify a bounding box that encloses the
Group node's children. This is a hint that may be used for optimization
purposes. The results are undefined if the specified bounding box is smaller
than the actual bounding box of the children at any time. A default bboxSize
value, (-1, -1, -1), implies that the bounding box is not specified and, if
needed, is calculated by the browser. A description of the bboxCenter and
bboxSize fields is contained in 10.2.2 Bounding boxes.
*/
X3DGroup : X3DGroupingNode {
    *x3dName { ^'Group' }
}

/* The Transform node is a grouping node that defines a coordinate system for
its children that is relative to the coordinate systems of its ancestors. See
4.3.5 Transformation hierarchy and 4.3.6 Standard units and coordinate system
for a description of coordinate systems and transformations.

10.2.1 Grouping and children node types, provides a description of the
children, addChildren, and removeChildren fields.

The bboxCenter and bboxSize fields specify a bounding box that encloses the
children of the Transform node. This is a hint that may be used for
optimization purposes. The results are undefined if the specified bounding box
is smaller than the actual bounding box of the children at any time. A default
bboxSize value, (-1, -1, -1), implies that the bounding box is not specified
and, if needed, shall be calculated by the browser. The bounding box shall be
large enough at all times to enclose the union of the group's children's
bounding boxes; it shall not include any transformations performed by the
group itself (i.e., the bounding box is defined in the local coordinate system
of the children). The results are undefined if the specified bounding box is
smaller than the true bounding box of the group. A description of the
bboxCenter and bboxSize fields is provided in 10.2.2 Bounding boxes.

The translation, rotation, scale, scaleOrientation and center fields define a
geometric 3D transformation consisting of (in order):

   1. a (possibly) non-uniform scale about an arbitrary point;
   2. a rotation about an arbitrary point and axis;
   3. a translation.

The center field specifies a translation offset from the origin of the local
coordinate system (0,0,0). The rotation field specifies a rotation of the
coordinate system. The scale field specifies a non-uniform scale of the
coordinate system. Scale values may have any value: positive, negative
(indicating a reflection), or zero. A value of zero indicates that any child
geometry shall not be displayed. The scaleOrientation specifies a rotation of
the coordinate system before the scale (to specify scales in arbitrary
orientations). The scaleOrientation applies only to the scale operation. The
translation field specifies a translation to the coordinate system.

Given a 3-dimensional point P and Transform node, P is transformed into point
P' in its parent's coordinate system by a series of intermediate
transformations. In matrix transformation notation, where C (center), SR
(scaleOrientation), T (translation), R (rotation), and S (scale) are the
equivalent transformation matrices,

  P' = T * C * R * SR * S * -SR * -C * P

The following Transform node:

Transform {
  center           C
  rotation         R
  scale            S
  scaleOrientation SR
  translation      T
  children         [
    # Point P (or children holding other geometry)
  ]
}

is equivalent to the nested sequence of:

Transform {
  translation T 
  children Transform {
    translation C
    children Transform {
      rotation R
      children Transform {
        rotation SR 
        children Transform {
          scale S 
          children Transform {
            rotation -SR 
            children Transform {
              translation -C
              children [
                # Point P (or children holding other geometry)
              ]
}}}}}}}
*/

X3DTransform : X3DGroupingNode {
    *x3dName { ^'Transform' }
    declareFields {
        super.declareFields;
        this.declareX3D(
            [ SFVec3f.type    , [in,out] , \center           , Vector3(0, 0, 0)      ],
            [ SFRotation.type , [in,out] , \rotation         , Rotation4(0, 0, 1, 0) ],
            [ SFVec3f.type    , [in,out] , \scale            , Vector3(1, 1, 1)      ],
            [ SFRotation.type , [in,out] , \scaleOrientation , Rotation4(0, 0, 1, 0) ],
            [ SFVec3f.type    , [in,out] , \translation      , Vector3(0, 0, 0)      ]
        );
    }
}
