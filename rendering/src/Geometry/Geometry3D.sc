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

GVector {
    classvar epsilon = 1e-16;

	// Angle between two vectors in radians
	angle_v { | v |
		// var u = this.norm, v_ = v.norm;
		// ^acos(u dot: v_)
		^atan2(abs(cross(this,v)),dot(this,v))
	}
	
	// Length of vector
	abs {
		^this.subclassResponsibility(thisMethod)
	}
	// Alias for abs
	magnitude {
		^this.abs
	}
	neg {
        ^this.performUnaryOp(thisMethod.name)
    }
    reciprocal {
        ^this.performUnaryOp(thisMethod.name)
    }
    
	// AbstractFunction stuff
    // FIXME: Inherit from AbstractFunction?
    performUnaryOp { | selector |
		^this.subclassResponsibility(thisMethod)
    }

	// Conversion
	asVector2 {
		^this.subclassResponsibility(thisMethod)
	}
    asVector3 {
		^this.subclassResponsibility(thisMethod)
    }
    asVector4 {
		^this.subclassResponsibility(thisMethod)
    }
    asArray {
		^this.subclassResponsibility(thisMethod)
    }

	// Printing
    printOn { | stream |
        stream << this.class.name << "[";
        this.asArray.printItemsOn(stream);
        stream << "]";
    }
}

Vector2 : GVector {
	var <x, <y;
	
	*new { | x=0.0, y=0.0 |
		^this.newCopyArgs(x, y)
	}
	// Length of vector
	abs {
		^hypot(x,y)
	}
	// Angle to x-axis in [0, 2pi[
	angle {
		^atan2(y,x) mod: 2pi
	}

	// Conversion
	asVector2 {
		^this
	}
    asVector3 {
		^Vector3(x, y)
    }
    asVector4 {
		^Vector4(x, y)
    }
    asArray {
		^[x, y]
    }

	// AbstractFunction stuff
    performUnaryOp { | selector |
		^this.class.new(x.perform(selector), y.perform(selector))
    }
}

Vector3 : GVector {
    var <x, <y, <z;
    
    *new { | x=0.0, y=0.0, z=0.0 |
        ^this.newCopyArgs(x, y, z)
    }
    *newFrom { | vector |
        ^this.new(vector.x, vector.y, vector.z)
    }
    *with { | x=0.0 |
        ^this.new(x, x, x)
    }
    
	// Conversion
	asVector2 {
		^Vector2(x, y)
	}
    asVector3 {
        ^this
    }
    asVector4 {
        ^Vector4(x, y, z)
    }
    asArray {
        ^[x, y, z]
    }
    asMatrix4 {
        ^this.subclassResponsibility(thisMethod)
    }
    asScaling3 {
        ^Scaling3.newFrom(this)
    }
    asTranslation3 {
        ^Translation3.newFrom(this)
    }
    asRotation3 { | angle |
        ^Rotation3(x, y, z, angle)
    }
    
    norm {
        ^this.copy.pr_norm_
    }
    abs {
        ^sqrt(x.squared + y.squared + z.squared)
    }
    
	sub_s_ { | s |
		x = x - s;
		y = y - s;
		z = z - s;
	}
	sub_s { | s |
		^this.copy.sub_s_(s)
	}
	sub_v_ { | v |
		x = x - v.x;
		y = y - v.y;
		z = z - v.z;
	}
	sub_v { | v |
		^this.copy.sub_v_(v)
	}
	
	mul_s_ { | s |
		x = s * x;
		y = s * y;
		z = s * z;
	}
	mul_s { | s |
		^this.copy.mul_s_(s)
	}

	mul_v { | v |
		^(x * v.x) + (y * v.y) + (z * v.z)
	}
	dot { | v |
		^this.mul_v(v)
	}
	cross { | v |
		^Vector3(
			(this.x*v.z) - (this.z*v.y),
			(this.z*v.x) - (this.x*v.z),
			(this.x*v.y) - (this.y*v.x) )
	}
	
	// Collection stuff
	performArray { | selector ... args |
	    ^this.new(*this.asArray.performList(selector, args))
    }
	collect { | function |
	    ^this.performArray(thisMethod.name, function)
    }
    
    // AbstractFunction stuff
    performUnaryOp { | selector |
        ^this.class.new(x.perform(selector), y.perform(selector), z.perform(selector))
    }
        
    // PRIVATE
    pr_norm_ {
        var r = this.abs, a;
        if (r > epsilon) {
            a = r.reciprocal;
            x = a*x;
            y = a*y;
            z = a*z;
        };
    }
}

Vector4 : GVector {
    var <x, <y, <z, <w;
    
    *new { | x=0.0, y=0.0, z=0.0, w=1.0 |
        ^this.newCopyArgs(x, y, z, w)
    }

	// Conversion
    asVector2 {
        var a = w.reciprocal;
        ^Vector2(a*x, a*y)
	}
    asVector3 {
        var a = w.reciprocal;
        ^Vector3(a*x, a*y, a*z)
    }
    asVector4 {
        ^this
    }
    asArray {
        ^[x, y, z, w]
    }
    
	mul_s_ { | s |
		x = s * x;
		y = s * y;
		z = s * z;
		w = s * w;
	}
	mul_s { | s |
		^this.copy.mul_s_(s)
	}

	mul_v { | v |
		^(x * v.x) + (y * v.y) + (z * v.z) + (w * v.w)
	}
	dot { | v |
		^this.mul_v(v)
	}
	
    m_mul_ { | a |
        // v = A * v
        var xx = x, yy = y, zz = z, ww = w;
        x = (xx*a.at(0,0)) + (yy*a.at(0,1)) + (zz*a.at(0,2)) + (ww*a.at(0,3));
        y = (xx*a.at(1,0)) + (yy*a.at(1,1)) + (zz*a.at(1,2)) + (ww*a.at(1,3));
        z = (xx*a.at(2,0)) + (yy*a.at(2,1)) + (zz*a.at(2,2)) + (ww*a.at(2,3));
        w = (xx*a.at(3,0)) + (yy*a.at(3,1)) + (zz*a.at(3,2)) + (ww*a.at(3,3));        
    }
    m_mul { | a |
        // v' = A * v
        ^this.copy.m_mul_(a)
    }
    mul_m_ { | a |
        // v = v * A
        var xx = x, yy = y, zz = z, ww = w;
        x = (xx*a.at(0,0)) + (yy*a.at(1,0)) + (zz*a.at(2,0)) + (ww*a.at(3,0));
        y = (xx*a.at(0,1)) + (yy*a.at(1,1)) + (zz*a.at(2,1)) + (ww*a.at(3,1));
        z = (xx*a.at(0,2)) + (yy*a.at(1,2)) + (zz*a.at(2,2)) + (ww*a.at(3,2));
        w = (xx*a.at(0,3)) + (yy*a.at(1,3)) + (zz*a.at(2,3)) + (ww*a.at(3,3));
    }
    mul_m { | a |
        // v' = v * A
        ^this.copy.mul_m_(a)
    }

    // AbstractFunction stuff
    performUnaryOp { | selector |
        ^this.class.new(x.perform(selector), y.perform(selector), z.perform(selector), w.perform(selector))
    }
}

Matrix4[slot] : Matrix {
    *newClear {
        ^super.newClear(4, 4)
    }
	*with { | array |
	    if (array.size != 4 and: { array.first.size != 4 }) {
	        throw(Error("Invalid operand"));
        };
        ^super.with(array)
	}
	*withFlatArray { | array |
	    if (array.size != 16) {
	        throw(Error("Invalid operand"));
        };
	    ^super.withFlatArray(4, 4, array)
    }
    *fill { | function |
        ^super.fill(4, 4, function)
    }
	*newIdentity {
	    ^super.newIdentity(4)
    }
    *identity {
        ^this.newIdentity
    }
	*unit {
	    ^this.fill(1.0)
    }
    
    mul_m_ { | b |
        // A = A * B
        var a00 = this.at(0,0), a01 = this.at(0,1), a02 = this.at(0,2), a03 = this.at(0,3);
        var a10 = this.at(1,0), a11 = this.at(1,1), a12 = this.at(1,2), a13 = this.at(1,3);
        var a20 = this.at(2,0), a21 = this.at(2,1), a22 = this.at(2,2), a23 = this.at(2,3);
        var a30 = this.at(3,0), a31 = this.at(3,1), a32 = this.at(3,2), a33 = this.at(3,3);

        this.put(0,0,(a00*b.at(0,0))+(a01*b.at(1,0))+(a02*b.at(2,0))+(a03*b.at(3,0)));
        this.put(1,0,(a10*b.at(0,0))+(a11*b.at(1,0))+(a12*b.at(2,0))+(a13*b.at(3,0)));
        this.put(2,0,(a20*b.at(0,0))+(a21*b.at(1,0))+(a22*b.at(2,0))+(a23*b.at(3,0)));
        this.put(3,0,(a30*b.at(0,0))+(a31*b.at(1,0))+(a32*b.at(2,0))+(a33*b.at(3,0)));

        this.put(0,1,(a00*b.at(0,1))+(a01*b.at(1,1))+(a02*b.at(2,1))+(a03*b.at(3,1)));
        this.put(1,1,(a10*b.at(0,1))+(a11*b.at(1,1))+(a12*b.at(2,1))+(a13*b.at(3,1)));
        this.put(2,1,(a20*b.at(0,1))+(a21*b.at(1,1))+(a22*b.at(2,1))+(a23*b.at(3,1)));
        this.put(3,1,(a30*b.at(0,1))+(a31*b.at(1,1))+(a32*b.at(2,1))+(a33*b.at(3,1)));

        this.put(0,2,(a00*b.at(0,2))+(a01*b.at(1,2))+(a02*b.at(2,2))+(a03*b.at(3,2)));
        this.put(1,2,(a10*b.at(0,2))+(a11*b.at(1,2))+(a12*b.at(2,2))+(a13*b.at(3,2)));
        this.put(2,2,(a20*b.at(0,2))+(a21*b.at(1,2))+(a22*b.at(2,2))+(a23*b.at(3,2)));
        this.put(3,2,(a30*b.at(0,2))+(a31*b.at(1,2))+(a32*b.at(2,2))+(a33*b.at(3,2)));

        this.put(0,3,(a00*b.at(0,3))+(a01*b.at(1,3))+(a02*b.at(2,3))+(a03*b.at(3,3)));
        this.put(1,3,(a10*b.at(0,3))+(a11*b.at(1,3))+(a12*b.at(2,3))+(a13*b.at(3,3)));
        this.put(2,3,(a20*b.at(0,3))+(a21*b.at(1,3))+(a22*b.at(2,3))+(a23*b.at(3,3)));
        this.put(3,3,(a30*b.at(0,3))+(a31*b.at(1,3))+(a32*b.at(2,3))+(a33*b.at(3,3)));
    }
    mul_m { | b |
        // A' = A * B
        ^this.deepCopy.mul_m_(b)
    }

    mul_v_ { | v |
        // v = A * v
        v.m_mul_(this)
    }
    mul_v { | v |
        // v' = A * v
        ^v.m_mul(this)
    }

    mul_s_ { | x |
        // A = A * x
        this.doMatrix { | v, r, c|
            this.put(r, c, v*x);
        };
    }
    mul_s { | x |
        // A' = A * x
        ^this.deepCopy.mul_s_(x)
    }
    
    *compose { | array |
        var result;
        if (array.isEmpty) {
            throw(Error("Invalid operand"));
        };
        result = array.first.copy;
        (1..array.size-1).do { |i|
            result mul_m_: array[i];
        };
        ^result
    }
}

Scaling3 : Vector3 {
    asMatrix4 {
        ^Matrix4.withFlatArray([
            x   , 0.0 , 0.0 , 0.0 ,
            0.0 , y   , 0.0 , 0.0 ,
            0.0 , 0.0 , z   , 0.0 ,
            0.0 , 0.0 , 0.0 , 1.0
        ])
    }
}

Translation3 : Vector3 {
    asMatrix4 {
        ^Matrix4.withFlatArray([
            1.0 , 0.0 , 0.0 , x   ,
            0.0 , 1.0 , 0.0 , y   ,
            0.0 , 0.0 , 1.0 , z   ,
            0.0 , 0.0 , 0.0 , 1.0
        ])
    }
}

Rotation3 : Translation3 {
    var <angle;
    
    *new { | x=0.0, y=0.0, z=1.0, angle=0.0 |
        ^super.new(x, y, z).initRotation3(angle)
    }
    initRotation3 { | inPhi |
        angle = inPhi;
        this.pr_norm_;
    }
    asMatrix4 {
        var c = cos(angle), s = sin(angle), t = 1.0-c;
        ^Matrix4.withFlatArray([
            (t*x.squared)+c , (t*x*y)+(s*z)   , (t*x*z)-(s*y)   , 0.0 ,
            (t*x*y)-(s*z)   , (t*y.squared)+c , (t*y*z)+(s*x)   , 0.0 ,
            (t*x*z)+(s*y)   , (t*y*z)-(s*x)   , (t*z.squared)+c , 0.0 ,
            0.0             , 0.0             , 0.0             , 1.0
        ])
    }
}
