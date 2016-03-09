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

/* This abstract node type is the base type for all nodes in the X3D system.
*/
X3DNode : X3DObject {
	var <name, fields, <impl;
	
	*x3dName {
	    ^this.class.name
    }
    
	*new { | world, name=nil |
		^super.new(world).initX3DNode(name)
	}
	
	initX3DNode { | inName |
		this.declareFields;
		name = inName;
		impl = world.implementationForNode(this);
	}
	
	//* Field declaration.
	
	/* Declare the node's fields.
	   
	   This is called by the node's constructor and needs to be overridden in
       each subclass that adds new fields. Don't forget to call the
       superclass' implementation!
       
       declareFields {
           super.declareFields;
           // Declare new fields here.
           // ...   
       }
	*/
	declareFields {
		this.declareX3D(
			[ SFNode.type(X3DMetadataObject) , [in,out] , \metadata , nil ]
		);
	}
	
	// Declare a single field.
	declare { | name, type, access, value |
		// [name, type, access, value].postln;
		this.pr_addField(name, type.makeField(world, access, value));
	}
	/* Declare a list of fields in the X3D spec format.
	
	   See the various X3DNode subclasses for examples.
	*/
	declareX3D { | ... specs |
		var name, type, access, value;
		specs.do { | spec |
			#type, access, name, value = spec;
			this.declare(name, type, access, value);
		}
	}
	/* Mix the fields declared by a node class into this node.
	
	   This is sometimes necessary because SuperCollider doesn't have multiple
       inheritance.
	*/
	mixin { | nodeClass |
	    nodeClass.new(world).fieldsDo { | name, field |
	        this.pr_addField(name, field);
        };
    }
    
    //* Field access.
    
    fieldNames {
        ^fields.keys
    }
	field { | name |
		^fields.at(name)
	}
	fieldsDo { | function |
	    fields.keysValuesDo(function);
    }
	at { | name |
		^this.field(name).value
	}
	put { | name, value |
		this.field(name).value_(value);
	}
	containerField {
	    ^this.subclassResponsibility(thisMethod)
    }
	
	release {
	    impl.release;
		fields.do(_.release);
	}
	
	// PRIVATE
	pr_addField { | name, field |
		// [name, type, access, value].postln;
		if (fields.isNil) {
			// Lazy initialization
			fields = IdentityDictionary.new;
		};
		if (fields.includesKey(name)) {
		    if (fields.at(name) != field) {
			    throw(X3DError("Field already declared: " ++ name.asString));
			};
		} {
		    fields.put(name, field);
		};
    }
}
