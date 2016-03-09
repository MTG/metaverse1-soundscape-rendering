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

X3DUnitTest : UnitTest {
    var n = 250;
}

TestX3DFieldParser : X3DUnitTest {
    test_parser {
        var a = ["foo", "blah", "1.0", "2.0", "1.0", "42"];
        
        this.assertEquals(a, X3DFieldParser(a.join(" ")).all);
        this.assertEquals(a, X3DFieldParser(a.join(",")).all);
        this.assertEquals(a, X3DFieldParser(a.join(", ")).all);
        this.assertEquals(a, X3DFieldParser("foo, blah, 1.0 2.0 , 1.0,42,  ,").all);

        n.do {
            var a = Array.rand(32, -100.0, 100.0).collect(_.asString);
            this.assertEquals(a, X3DFieldParser(a.join(" ")).all, report: false);
            this.assertEquals(a, X3DFieldParser(a.join(",")).all, report: false);
            this.assertEquals(a, X3DFieldParser(a.join(", ")).all, report: false);
        }
    }
}

TestOrderedIdentitySet : UnitTest {
    test_order {
        var x = OrderedIdentitySet[\foo, \gee, \bar];
        this.assertEquals(
            x.copy.remove(\gee),
            OrderedIdentitySet[\foo, \bar]
        );
        this.assertEquals(
            x.copy.clear,
            OrderedIdentitySet.new
        );
        this.assertEquals(
            x.copy.add(\blah),
            OrderedIdentitySet[\foo, \gee, \bar, \blah]
        );
    }
}
