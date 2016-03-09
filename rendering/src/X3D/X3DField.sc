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

/* Field constructor.
*/
X3DType : Message {
	makeField { | world, access, value, epoch |
		^this.value(*(this.args ++ [world, access, value, epoch]))
	}
}

/* X3DField is the abstract field type from which all single values field
   types are derived. All fields derived from X3DField have names beginning with
   SF. SFxxxx fields may only contain a single value of the type indicated by the
   name of the field type.
*/
X3DField : X3DObject {
	var <access, <epoch, value, dependants;
	
	*new { | world, access, value, epoch |
		^super.new(world).initX3DField(access, value, epoch ? thisThread.seconds)
	}
	*type {
		^X3DType(this, \new)
	}
    
	initX3DField { | inAccess, inValue, inEpoch |
		access = inAccess;
		value  = if (inValue === \default) { this.defaultValue } { inValue };
		epoch  = inEpoch;
	}
	
	isReadable {
		^access.includes(out)
	}
	isWritable {
		^access.includes(in)
	}
	
	// High level interface
	value {
		if (this.isReadable.not) {
			throw(X3DError("Access error: reading input only field"))
		};
		^this.read
	}
	value_ { | inValue |
		if (this.isWritable.not) {
			throw(X3DError("Access error: writing output only field"))
		};
		if (this.canContain(inValue).not) {
			throw(X3DError("Type error"));
		};
		this.put(thisThread.seconds, inValue);
	}
	
	// Low level interface
	put { | time, inValue |
		//if (value != inValue) {
			this.pr_setValue(inValue);
			this.valueChanged(time, inValue);
		//};
	}
	read {
		^value
	}
	
	dependants {
		if (dependants.isNil) {
			dependants = IdentitySet.new(4);
		};
		^dependants
	}
	
	addDependant { | other |
		if (other.isWritable.not) {
			throw(X3DError("Connection to output only field"));
		};
		if (this.isReadable.not) {
			throw(X3DError("Connection from input only field"));
		};
		this.dependants.add(other);
	}
	removeDependant { | other |
		this.dependants.remove(other);
	}
	release {
		dependants = nil;
	}
	
	valueChanged { | time, value |
		// schedule updates to dependants
		dependants.do { |x|
			world.scheduleEvent(this, x, time, value);
		};
	}
	updateValue { | who, time, value |
		if (epoch < time and: { who !== this }) {
			//[\updateValue, who, time, value].postln;
			epoch = time;
			// propagate
			this.put(time, value);
		};
	}
	
	// Field typing
	itemClass {
	    ^this.subclassResponsibility(thisMethod)
    }
	defaultValue {
	    ^this.subclassResponsibility(thisMethod)
    }
	canContain { | aValue |
		^aValue.isKindOf(this.itemClass)
	}
	
	// XML encoding
	valueFromXML { | aString |
    }
    valueToXML { | aString |
    }
    fromXML { | xml |
    }
    toXML { | xml |
    }
    
	// PRIVATE
	pr_setValue { | inValue |
		value = inValue;
	}
}

/* Listens for changes to a field.

   Used in node implementations.
*/
X3DFieldListener {
	var <action, epoch;
	
	*new { | node, fieldName, action |
		^this.newCopyArgs(action, thisThread.seconds).initX3DFieldController(node, fieldName)
	}
	initX3DFieldController { | node, fieldName |
		node.field(fieldName).addDependant(this);
	}
	
	isReadable {
		^true
	}
	isWritable {
		^true
	}
	
	updateValue { | who, time, value |
		if (epoch < time) {
			epoch = time;
			action.value(time, value);
		};
	}
}

X3DBasicField : X3DField {		
}

SFBool : X3DBasicField {
    itemClass { ^Bool }
    defaultValue { ^false }
}

SFDouble : X3DBasicField {
    itemClass { ^Float }
    defaultValue { ^0.0 }
}

SFFloat : X3DBasicField {
    itemClass { ^Float }
    defaultValue { ^0.0 }
}

SFInt32 : X3DBasicField {
    itemClass { ^Integer }
    defaultValue { ^0 }
}

SFRotation : X3DBasicField {
    itemClass { ^Rotation4 }
    defaultValue { ^this.itemClass.new(0.0, 0.0, 1.0, 0.0) }
}

SFString : X3DBasicField {
    itemClass { ^String }
    defaultValue { ^"" }
}

SFTime : X3DBasicField {
    itemClass { ^Float }
    defaultValue { ^-1.0 }
}

SFVec2d : X3DBasicField {
    itemClass { ^Vector2D }
    defaultValue { ^this.itemClass.new }
}

SFVec2f : X3DBasicField {
    itemClass { ^Vector2D }
    defaultValue { ^this.itemClass.new }
}

SFVec3d : X3DBasicField {
    itemClass { ^Vector3 }
    defaultValue { ^this.itemClass.new }
}

SFVec3f : X3DBasicField {
    itemClass { ^Vector3 }
    defaultValue { ^this.itemClass.new }
}

SFVec4d : X3DBasicField {
    itemClass { ^Vector4 }
    defaultValue { ^this.itemClass.new }
}

SFVec4f : X3DBasicField {
    itemClass { ^Vector4 }
    defaultValue { ^this.itemClass.new }
}

SFNode : X3DField {
	var <itemClass;
	
	*new { | itemClass, world, access, value, epoch |
		^super.new(world, access, value, epoch).initSFNode(itemClass)
	}
	*type { | itemClass |
		^X3DType(this, \new, [itemClass])
	}
	
	initSFNode { | inItemClass |
		itemClass = inItemClass;
	}

	defaultValue {
	    ^nil
    }
}

/* X3DArrayField is the abstract field type from which all field types that
   can contain multiple values are derived. All fields derived from X3DArrayField
   have names beginning with MF. MFxxxx fields may zero or more values, each of
   which shall be of the type indicated by the corresponding SFxxxx field type.
   It is illegal for any MFxxxx field to mix values of different SFxxxx field
   types.
*/
X3DArrayField : X3DField {
	defaultValue {
	    ^X3DArray(this.itemClass)
    }
	canContain { | aValue |
		^aValue.isKindOf(X3DArray) and: { aValue.itemClass.isKindOf(this.itemClass.class) }
	}
}

X3DBasicArrayField : X3DArrayField {
}

MFBool     : X3DBasicArrayField { itemClass { ^Bool        } }
MFDouble   : X3DBasicArrayField { itemClass { ^Float       } }
MFFloat    : X3DBasicArrayField { itemClass { ^Float       } }
MFInt32    : X3DBasicArrayField { itemClass { ^Integer     } }
MFRotation : X3DBasicArrayField { itemClass { ^Rotation4  } }
MFString   : X3DBasicArrayField { itemClass { ^String      } }
MFTime     : X3DBasicArrayField { itemClass { ^Float       } }
MFVec2d    : X3DBasicArrayField { itemClass { ^Vector2D    } }
MFVec2f    : X3DBasicArrayField { itemClass { ^Vector2D    } }
MFVec3d    : X3DBasicArrayField { itemClass { ^Vector3    } }
MFVec3f    : X3DBasicArrayField { itemClass { ^Vector3    } }
MFVec4d    : X3DBasicArrayField { itemClass { ^Vector4    } }
MFVec4f    : X3DBasicArrayField { itemClass { ^Vector4    } }

MFNode : X3DArrayField {
	var <itemClass;

	*new { | itemClass, world, access, value, epoch |
		^super.new(world, access, value, epoch).initMFNode(itemClass)
	}
	*type { | itemClass |
		^X3DType(this, \new, [itemClass])
	}
	
	initMFNode { | inItemClass |
		itemClass = inItemClass;
	}
}
