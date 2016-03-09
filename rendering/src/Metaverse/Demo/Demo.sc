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

MVDemo {
    var <dir, <server, <room, <roomOrigin;
	var <buffers;
    var <position, <angle;
	var debugPosition = false;

    *new { | dir, server, room, roomOrigin |
        ^this.newCopyArgs(dir, server, room, roomOrigin)
    }
    
	mapVector { | v |
		^Vector3(v.z, v.x, v.y)
	}
	
	mapOrientation { | v |
		^Vector3(v.z.neg, v.x.neg, v.y)
		//^this.mapVector(v)
	}
	
    mapPosition { | v, offset=nil |
		var x = v.x, y = v.y, z = v.z;
		var rx = room.room[3], ry = room.room[4];
		if (offset.notNil) {
			x = x - offset.x;
			y = y - offset.y;
			z = z - offset.z;
		};
        ^Vector3(rx - z, ry - x, y)
		//^this.mapVector(v sub_v: roomOrigin)
	}
    
    sounds {
        ^(dir +/+ "sounds/demo_sounds_mono/*.wav").pathMatch;
    }
    
    init {
        buffers = this.sounds.collectAs({ |path| PathName(path).fileNameWithoutExtension -> Buffer.read(server, path) }, Dictionary);
		position = Vector3.new;
		angle = 0;
		VirtualRoom.kemarPath = dir +/+ "sounds/impulse_responses/KemarHRTF/";
		room.init;
    }

	position_ { | v |
		position = this.mapPosition(v, roomOrigin);
		this.updateListenerPosition(position, angle);
	}
	orientation_ { | v |
		//var axis = Vector3(1, 0, 0), o = Vector3(v.z.neg, v.x.neg, 0);
		//angle = (axis angle: o).x;
		angle = this.mapOrientation(v).asVector2.angle;
		this.updateListenerPosition(position, angle);
	}
	updateListenerPosition { | pos, angle |
		// Update listener position and orientation
		room.listener.set(\x, pos.x, \y, pos.y, \z, pos.z, \o, angle);
		// Update step sound position
		room.sources[\steps].set(\xpos, pos.x, \ypos, pos.y, \zpos, 0);
		if (debugPosition) {
			[\position, pos.x, pos.y, pos.z, angle].postln;
		};
	}
}