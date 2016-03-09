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

/* This abstract interface is inherited by all nodes that contain data located
   on the World Wide Web, such as AudioClip, ImageTexture and Inline.

   All url fields can hold multiple string values. The strings in these fields
   indicate multiple locations to search for data in the order listed. If the
   browser cannot locate or interpret the data specified by the first
   location, it shall try the second and subsequent locations in order until a
   location containing interpretable data is encountered. X3D browsers only
   have to interpret a single string. If no interpretable locations are found,
   the node type defines the resultant default behaviour.

   For more information on URLs, see 9.2.1 URLs.
*/
X3DUrlObject : X3DNode {
    declareFields {
        super.declareFields;
        declareX3D(
            [ MFString.type , [in,out] , \url , [] ]
        );
    }
}
