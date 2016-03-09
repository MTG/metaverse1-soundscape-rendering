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

GeometryUnitTest : UnitTest {
    var <n = 250, <myClass;
    
    setUp {
        super.setUp;
        myClass = this.pr_myClass;
    }
    
    pr_myClass {
        var name = this.class.name.asString;
        if (name.beginsWith("Test")) {
            ^name.replace("Test", "").asSymbol.asClass
        };
        ^this.class
    }

    // Utility
	assertVectorEquals { | a, b, message="", within, report=false, onFailure |
        var epsilon = within ?? { Vector4.with(0.001) };
        this.assert( ((a.asArray - b.asArray).abs < epsilon.asArray).flat.every { |x| x }, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report, onFailure );
    }
}

TestVector3 : GeometryUnitTest {
    var v_gen;
    
    setUp {
		super.setUp;
		v_gen = FuncStream({ myClass.new(*{ rrand(-100.0, 100.0) }.dup(3)) });
    }
    
    test_properties {
		// Dot product commutativity
		// A o B = B o A
		n.do {
			var a = v_gen.next, b = v_gen.next;
			this.assertVectorEquals( a dot: b, b dot: a );
		};
        // v / ||v|| = 1 
        n.do {
            var v = v_gen.next;
            this.assertFloatEquals( 1.0, v.norm.abs, report: false );
        };
    }
}

TestMatrix4 : GeometryUnitTest {
    var id, zero;
    var m_gen, v_gen;

	setUp {
		super.setUp;
		zero  = myClass.newClear;
		id    = myClass.identity;
		m_gen = FuncStream({ myClass.fill({ rrand(-100.0, 100.0) }) });
        v_gen = FuncStream({ Vector3(*{ rrand(-100.0, 100.0) }.dup(3)) });
	}

    // Tests
	test_multiply_m {
	    // A * Z = Z
	    n.do {
    	    var a = m_gen.next;
    	    this.assertMatrixEquals( zero, a mul_m: zero );
    	};
    	
    	// I * A * I = A
	    n.do {
    	    var a = m_gen.next;
    	    this.assertMatrixEquals( a, id mul_m: (a mul_m: id) );
    	};
    	
        // Z * ... * Z = Z
        this.assertMatrixEquals( zero, myClass.compose(zero.dup(100)) );
        
    	// I * ... * I = I
        this.assertMatrixEquals( id, myClass.compose(id.dup(100)) );
        
        // A * 0 = Z
        n.do {
            var a = m_gen.next;
            this.assertMatrixEquals( zero, a mul_s: 0 );
        };
        
        // A * 1 = A
        n.do {
            var a = m_gen.next;
            this.assertMatrixEquals( a, a mul_s: 1 );
        };
        
        // A * x * 1/x = A
        n.do {
            var a = m_gen.next, x = rrand(-100.0, 100.0);
            this.assertMatrixEquals( a, (a mul_s: x) mul_s: x.reciprocal );
        };  
	}

    test_multiply_v {
        // v * I = v
        // I * v = v
        n.do {
            var v = v_gen.next.asVector4;
            this.assertVectorEquals( v, v mul_m: id );
            this.assertVectorEquals( v, id mul_v: v );
        };
    }
    
    test_translation {
        // T' = -T
        n.do {
            var t = v_gen.next.asTranslation3;
            this.assertMatrixEquals( t.asMatrix4.inverse, t.neg.asMatrix4 );
        };
        // T * -T = I
        n.do {
            var t = v_gen.next.asTranslation3;
            this.assertMatrixEquals( id, t.asMatrix4 mul_m: t.neg.asMatrix4 );
        };
        // T * T' = I
        100.do {
            var t = v_gen.next.asTranslation3.asMatrix4;
            this.assertMatrixEquals( id, t mul_m: t.inverse );
        };
    }

    test_scaling {
        // S' = 1/S
        n.do {
            var s = v_gen.next.asScaling3;
            this.assertMatrixEquals( s.asMatrix4.inverse, s.reciprocal.asMatrix4 );
        };        
        // S * 1/S = I
        n.do {
            var s = v_gen.next.asScaling3;
            this.assertMatrixEquals( id, s.asMatrix4 mul_m: s.reciprocal.asMatrix4 );
        };
        // S * S' = I
        100.do {
            var s = v_gen.next.asTranslation3.asMatrix4;
            this.assertMatrixEquals( id, s mul_m: s.inverse );
        };
    }    
    
    test_rotation {
        // R' = -R
        n.do {
            var angle = rrand(0.0, 2pi);
            var v = v_gen.next;
            var r = v.asRotation3(angle), r_ = v.asRotation3(angle.neg);
            this.assertMatrixEquals( r.asMatrix4.inverse, r_.asMatrix4 );
        };        
        // R * -R = I
        n.do {
            var angle = rrand(0.0, 2pi);
            var v = v_gen.next;
            var r = v.asRotation3(angle), r_ = v.asRotation3(angle.neg);
            this.assertMatrixEquals( id, r.asMatrix4 mul_m: r_.asMatrix4 );
        };
        // R * R' = I
        100.do {
            var angle = rrand(0.0, 2pi);
            var r = v_gen.next.asRotation3(angle).asMatrix4;
            this.assertMatrixEquals( id, r mul_m: r.inverse );
        };
    }

    // Utility
	assertMatrixEquals { | a, b, message="", within, report=false, onFailure |
        var epsilon = within ?? { myClass.fill(0.001) };
        this.assert( ((a - b).abs < epsilon).flat.every { |x| x }, message + "\nIs:\n\t" + a + "\nShould be:\n\t" + b + "\n", report, onFailure );
    }
}
