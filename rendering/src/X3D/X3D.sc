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

X3DException : Exception { }
X3DError : X3DException { }

X3DRoute {
	var <fromNode, <fromField, <toNode, <toField;
	
	*new { | fromNode, fromField, toNode, toField |
		^this.newCopyArgs(fromNode, fromNode.field(fromField), toNode, toNode.field(toField)).initX3DRoute
	}
	initX3DRoute {
		fromField.addDependant(toField);
	}
	remove {
		fromField.removeDependant(toField);
	}
	release {
		this.remove;
	}
}

X3D {
	*addRoute { | fromNode, fromField, toNode, toField |
		^X3DRoute(fromNode, fromField, toNode, toField)
	}
}

X3DObject {
	var <world;
	classvar kInitializeOnly = #[], kInput = #[\in], kOutput = #[\out], kInputOutput = #[\in, out];
	classvar in = \in, out = \out;

	*new { | world |
		^this.newCopyArgs(world)
	}
	
}

X3DAbstractNodeImplementation {
    release {
    }
}

X3DWorld {
    var <nodeFactory;
    
    /* Return a concrete implementation for `aNode'.
    
       The idea is to facilitate different (pluggable) implementations of the
       X3D scene graph.
    */
    implementationForNode { | aNode |
        ^this.subclassResponsibility(thisMethod)
    }
    
	scheduleEvent { | src, dst, time, value |
		//[\scheduleEvent, src, dst, time, value].postln;
		dst.updateValue(src, time, value);
	}
}

X3DScheduler {
}

X3DArray {
	var <itemClass, <items;
	
	*new { | itemClass, items |
		^this.newCopyArgs(itemClass, items ?? { [] }).validate
	}
	validate {
		items.do { |x|
			if (x.isKindOf(itemClass).not) {
				throw(X3DError("Invalid item type: " ++ x.class.name.asString));
			};
		};
	}
	printOn { | stream |
		stream << this.class.name << "[ " ;
		items.printItemsOn(stream);
		stream << " ]" ;
	}
}

X3DTestNode : X3DNode {
	declareFields {
	    super.declareFields;
		this.declareX3D(
			[SFFloat.type, [in, out], \value, 0]
		);
	}
}
