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

X3DFieldParser : CollStream {
    classvar <whiteSpace = #[$ , $,];

    on {
        ^this.shouldNotImplement(thisMethod)
    }
    
    isWhiteSpace { | c |
        ^whiteSpace.includes(c)
    }
    
    next {
        var c = this.peek;
        if (c.isNil) {
            ^nil
        };
        if (this.isWhiteSpace(c)) {
            this.pr_skipWhiteSpace;
            ^this.next
        };
        ^this.pr_nextToken
    }
    
	nextN { | n |
		var result = Array.fill(n, {
		    var x = this.next;
    		if (x.isNil) {
    		    throw(Error("Parse error: stream exhausted"));
    	    };
		});
	    ^result
	}
    
    // PRIVATE
    pr_next {
        ^super.next
    }
    pr_skipWhiteSpace {
        var c;
        while { (c = this.peek).notNil and: { this.isWhiteSpace(c) } } {
            this.pr_next;
        };
    }
    pr_nextToken {
        var c, result = String.new;
        while { (c = this.peek).notNil and: { this.isWhiteSpace(c).not } } {
            result = result.add(this.pr_next);
        };
        ^if (result.notEmpty) { result }
    }
}
