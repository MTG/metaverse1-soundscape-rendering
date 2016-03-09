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

/* This abstract node type is the base node type from which all time-dependent
   nodes are derived. [8.2 Concepts], contains a detailed discussion of
   time-dependent nodes.
*/
X3DTimeDependentNode : X3DChildNode {
	declareFields {
		super.declareFields;
		this.declareX3D(
			[ SFBool.type , [in,out] , \loop         , false    ],
			[ SFTime.type , [in,out] , \pauseTime    , 0        ],
			[ SFTime.type , [in,out] , \resumeTime   , 0        ],
			[ SFTime.type , [in,out] , \startTime    , 0        ],
			[ SFTime.type , [in,out] , \stopTime     , 0        ],
			[ SFTime.type , [out]    , \elapsedTime  , \default ],
			[ SFBool.type , [out]    , \isActive     , \default ],
			[ SFBool.type , [out]    , \isPaused     , \default ]
		)
	}
}
