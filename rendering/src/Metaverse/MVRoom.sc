/*
    Filename: VirtualRoom.sc 
    created: 21.4.2005 

    Copyright (C) IEM 2005, Christopher Frauenberger [frauenberger@iem.at] 

    This program is free software; you can redistribute it and/or 
    modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation; either version 2 
    of the License, or (at your option) any later version. 

    This program is distributed in the hope that it will be useful, 
    but WITHOUT ANY WARRANTY; without even the implied warranty of 
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
    GNU General Public License for more details. 

    You should have received a copy of the GNU General Public License 
    along with this program; if not, write to the Free Software 
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. 

    IEM - Institute of Electronic Music and Acoustics, Graz 
    Inffeldgasse 10/3, 8010 Graz, Austria 
    http://iem.at
*/

MVAbstractRoomModel : Model {
    refs10polar { ^this.subclassResponsibility(thisMethod) }
}

MVSourceDistance {
    var <phi, <theta, <dist;
    
    *new { | s, l |
        var planeDist = hypot(s.y-l.y, s.x-l.x);
        ^this.newCopyArgs(
            atan2(l.y-s.y, s.x-l.x),
            atan2(s.z-l.z, planeDist),
            hypot(planeDist, s.z-l.z)
        )       
    }
    asArray {
        ^[phi, theta, dist]
    }
}

MVRectRoomModel : MVAbstractRoomModel {
    // refCoefs: w1, w2, w3, w4, floor, ceiling
    var <size, <refCoefs, <refGain, <revGain, <hfDamping, <temperature;
    
    *new { | size, refCoefs(1.dup(10)), refGain=1, revGain=1, hfDamping=0, temperature=20 |
        if (refCoefs.size != 10) {
            throw(Error("Invalid refCoefs array"));
        };
        ^super.new.init(size, refCoefs, refGain, revGain, hfDamping, temperature)
    }
    init { | ... args |
        #size, refCoefs, refGain, revGain, hfDamping, temperature = args;
    }
    
    diagonal {
        ^sqrt(size.x.squared + size.y.squared + size.z.squared)
    }
    speedOfSound {
        ^313.3 + (0.606 * temperature)
    }
    delayTime { | distance |
        ^distance/this.speedOfSound
    }
    maxDelayTime {
        ^this.delayTime(this.diagonal) + 0.05
    }
    reverbRoomSize {
        ^this.diagonal.linlin(0, 50, 0, 1)
    }
/*    refs10polar { | source, listener |
        ^room3D.refs10polar(source.x, source.y, source.z, listener.x, listener.y, listener.z)
    }
*/
    /*  method: refs10
        Calculates the first 10 relfections of a given source position in the room
        (4 x wall, 4 x corners and the first reflection at the floor and ceiling)
        Prameters: 
            px The source position (x)
            py The source position (y)
            pz The source position (z)
        Result:
            Array with 30 values as (x,y,z) for each of the 10 reflections
    */
    mirrorSourcePos { | p |
        var x, x1, x2, y, y1, y2, z, z1, z2;
        
        x  = p.x;
        x2 = x.neg;
        x1 = (2 * size.x) + x2;
        
        y  = p.y;
        y2 = y.neg;
        y1 = (2 * size.y) + y2;
        
        z  = p.z;
        z2 = z.neg;
        z1 = (2 * size.z) + z2;
        
        // FIXME: I think this needs to be revised when coordinate system is
        //        flipped by the x-axis (as in X3D).
        ^[
            Vector3( x2 , y2 , z  ), // upper right corner
            Vector3( x2 , y  , z  ), // right wall
            Vector3( x2 , y1 , z  ), // upper right corner
            Vector3( x  , y1 , z  ), // upper wall
            Vector3( x1 , y1 , z  ), // upper left corner
            Vector3( x1 , y  , z  ), // left wall
            Vector3( x1 , y2 , z  ), // lower left corner
            Vector3( x  , y2 , z  ), // lower wall
            Vector3( x  , y  , z1 ), // ceiling
            Vector3( x  , y  , z2 )  // floor
        ]           
    }
    *mirrorSourceDist { | ps, l |
        ^ps.collect { | ref | 
            MVSourceDistance(ref, l);
        }
    }
    /*  method: refs10polar
        Calculates the polar coordinates of the mirror sources 
        (phi, theta, distance) with the listener position as reference
        Parameters: 
            px, py, pz The source position 
            lx, ly, lz The listener position 
        Result: 
            Array with 30 values as (phi, theta, distance) for the 10 relflections 
    */
    mirrorSourceDist { | p, l |
        ^this.class.mirrorSourceDist(this.mirrorSourcePos(p), l)
    }

}

MVRadiationCone {
    var <level, <scale, <angle;
    
    *new { | level=0, angle=0 |
        ^this.newCopyArgs(level, level.dbamp, angle.wrap(0, pi))
    }
    
    isOmni {
        ^angle == 0
    }
    
    containsAngle { | otherAngle |
        ^otherAngle.abs <= angle
    }
}

MVSource {
    var <source, <position;
    var <axis, <innerCone, <outerCone, <cutoff;
    
    *new { | source, position, axis, innerCone(MVRadiationCone.new), outerCone(MVRadiationCone.new), cutoff=0 |
        ^this.newCopyArgs(source, position, axis, innerCone, outerCone, cutoff)
    }
    
    initMVSource { | ... args |
        #source, position, axis, innerCone, outerCone, cutoff = args;
        // Make position's elements NodeProxies
        position = position.collect { | x |
            if (x.isKindOf(NodeProxy))
                { x }
                { NodeProxy.control(source.server, 1).prime(x) }
        };
    }
    
    scale { | otherPos |
        ^if (axis.isNil or: { innerCone.isOmni }) {
            this.scaleOmni
        } {
            var axisAngle = axis.angle(otherPos sub_v: position);
            if (outerCone.isOmni) {
                this.scaleDirectional(axisAngle)
            } {
                var x = this.scaleFullyDirectional(axisAngle);
                if (x == 1) {
                    [\scaleFullyDirectional, otherPos, axisAngle].postln;
                };
                ^x
            }
        }
    }
    
    scaleOmni {
        ^innerCone.scale
    }
    
    scaleDirectional { | axisAngle |
        var inInner = innerCone.containsAngle(axisAngle).binaryValue;
        var inOuter = 1 - inInner;
        ^  (inInner * innerCone.scale)
         + (inOuter * outerCone.scale)
    }
    
    scaleFullyDirectional { | axisAngle |
        var inInner = innerCone.containsAngle(axisAngle).binaryValue;
        var inOuter = 1 - inInner;
        
        ^  (inInner * innerCone.scale)
         + (inOuter * axisAngle.linlin(innerCone.angle, outerCone.angle,
                                       innerCone.level, outerCone.level).dbamp);
    }
}

MVRenderer {
}

/*MVBinAmbiRenderer : MVRenderer {
    var <kemarPath, <scheme;
    var <encoded, <bin, <out, <revIn;
    
    *new { | irPath, scheme='1_4_7_4' |
        ^super.new.initMVBinAmbRenderer(irPath, scheme)
    }
    initMVBinAmbRenderer { | inKemarPath, inScheme |
        kemarPath = inKemarPath;
        scheme = inScheme;
    }
    
    init {
        // initialise the rendering
        BinAmbi3O.kemarPath = kemarPath;
    
        // Note: different schemes may be passed to the bin NodeProxy, see source / help
        BinAmbi3O.init(scheme, doneAction: {
            // initialise the rendering chain when buffers are ready
            revIn = NodeProxy.audio(numChannels: 2);
            encoded = NodeProxy.audio(numChannels: 16);
            bin = NodeProxy.audio(numChannels: 2);
            bin.prime({ BinAmbi3O.ar( encoded.ar ) }); // only prime them, dont start them yet.
            revIn.prime({ bin.ar });    // not ideal... 
                                    // - should be diffuse-field EQed, 
                                    // and decorrelated sum of 'encoded'. 
                                    // hmm. or maybe better: as is, but softened down (OnePole)
                                
            out = NodeProxy.audio(numChannels: 2);
            out.prime({ arg room = 0.25, revGain = 0.1, hfDamping = 0.6;
                    bin.ar + (FreeVerb2.ar( revIn.ar[0], revIn.ar[1], 
                        mix: 1, room: room, damp: hfDamping) * revGain) 
            });
            // Set the room size for FreeVerb
            out.set(\room, roomModel.freeVerbRoomSize);     
        });
    }
    
    play {
        out.play;
    }
    stop {
        out.stop;
    }
    end { | fadeTime |
        out.end(fadeTime);
        fork { 
            fadeTime.wait; 
            bin.end;
            revIn.end;
            encoded.end;
        };      
    }
}*/

/*  NOTE: 
    the coordinate system is given according to the listener's head:
    x-axis (nose), y-axis (left-ear) and z-axis (vertex)
    
    * left-deep-corner            *
               x                  |
               |                  |
              / \                 depth
        y<---| z |            |
             ----                 |
                              |
    *---------- width ------------* (origin) 

    Rooms are defined by the origin and width/depth/height
*/

/*  Class: VirtualEnvironment
    Provides a virtual room with sources, listener and binaural rendering
    
    TODO:
      * factor out rendering method to allow for binaural or ambisonics (of different orders)
      * use different sets of impulse responses (IRCAM listen, AK)
*/
MVRoom {
    
    // the path to Kemar
    classvar <>kemarPath = "KemarHRTF/";
    
    // The node proxies ...
    var <encoded, <bin, <out, <revIn;
    // ... and buses
    var <refGain;

    // maximum of sources allowed (exclusive reflections) 
    classvar <>maxSources = 10;
    
    // Room model
    var <roomModel;
        
    // the listener as NodeProxy.kr [ x, y, z, orientation]
    var <listener;
    
    // the list of sources for each instance
    var <sources;
        
    /*  Class method: *new
        create an instance and initialise the instance's list of sources
    */
    *new { | roomModel, renderer |
        ^super.new.initMVRoom(roomModel, renderer)
    }

    initMVRoom { | inRoomModel, renderer |
        roomModel = inRoomModel;
        sources = IdentityDictionary.new;
    }
    
    /*  Method: init
        init the binaural rendering engine
    */
    init { | function |

        // initialise the rendering
        BinAmbi3O.kemarPath = kemarPath;
        
        // Note: different schemes may be passed to the bin NodeProxy, see source / help
        BinAmbi3O.init('1_4_7_4', doneAction: {
            // initialise the rendering chain when buffers are ready
            revIn = NodeProxy.audio(numChannels: 2);
            encoded = NodeProxy.audio(numChannels: 16);
            bin = NodeProxy.audio(numChannels: 2);
            bin.prime({ BinAmbi3O.ar( encoded.ar ) }); // only prime them, dont start them yet.
            revIn.prime({ bin.ar });    // not ideal... 
                                    // - should be diffuse-field EQed, 
                                    // and decorrelated sum of 'encoded'. 
                                    // hmm. or maybe better: as is, but softened down (OnePole)
                                    
            out = NodeProxy.audio(numChannels: 2);
            out.prime({ arg room = 0.25, revGain = 0.1, hfDamping = 0.6;
                    bin.ar + (FreeVerb2.ar( revIn.ar[0], revIn.ar[1], 
                        mix: 1, room: room, damp: hfDamping) * revGain) 
            });
            // Set the room size for FreeVerb
            out.set(
                \room,      roomModel.reverbRoomSize,
                \revGain,   roomModel.revGain,
                \hfDamping, roomModel.hfDamping
            );
            
            listener = NodeProxy.control(numChannels: 4);
            listener.prime({ |x=0, y=0, z=0, o=0| [ x, y, z, o] });
            
            refGain = NodeProxy.control(numChannels: 1);
            refGain.prime({ | refGain=1 | refGain });
            refGain.set(
                \refGain, roomModel.refGain
            );
            
            function.value;
        });
    }

    bbox {
        ^roomModel.size
    }
    
    /*  method: play
        play the output node proxy
    */
    play { out.play }

    /*  method: stop
        stops the output node proxy
    */  
    stop { out.stop }

    /*  method: end
        ends the virtual room, removes all node proxies
        Parameter:
            fadeTime A time to fade out
    */
    end { |fadeTime=0.1| 
        out.end(fadeTime);
        fork { 
            fadeTime.wait; 
            sources.do(_.end);
            bin.end;
            revIn.end;
            encoded.end;
            listener.end;
        };
    }

    /*  method: addSource
        add a source to the virtual room 
        Parameter:
            source A mono sound source as NodeProxy.audio
            key A string to identify the source
            x, y, z the position of the source
    */
    addSource { | source |
        if (sources.includesKey(source).not) {
            this.prAddSource(false, source, source);
        };
    }

    /*  method: addSourceLight
        add a source to the virtual room, a lighter version
        Parameter:
            source A mono sound source as NodeProxy.audio
            key A string to identify the source
            x, y, z the position of the source
    */
    addSourceLight { | source |
        ^this.prAddSource(true, source, source) 
    }

    /*  method: removeSource
        remove a source from the virtual room 
        Parameter:
            key A string to identify the source
    */
    removeSource { | key |
        this.prAddSource(false, key, nil);
        sources.removeAt(key);
    }

    /*  method: prAddSource
        private function that takes care of adding the source
        Parameter:
            light Boolean to denote if light version or not
            source A mono sound source as NodeProxy.audio
            key A string to identify the source
            x, y, z the position of the source
    */
    prAddSource { | light, key, source |
        var synthFunc, myProxy;
        
        if (source.notNil) {
            if (light) {
                synthFunc = this.lightSourceFunc(source.source)
            } {
                synthFunc = this.fullSourceFunc(source)
            };
        };
        
        // make the audio proxy if needed
        if (sources[key].isNil) { 
            sources.put(key, 
                NodeProxy.audio(numChannels: encoded.numChannels)
                .bus_(encoded.bus)  // now writes to encoded bus directly!
            );
        } { 
            // "VirtualRoom: reusing existing proxy.".postln;
        };
        
        sources[key]
            .map(\refGain, refGain)
            .set(\xpos, source.position.x,
                 \ypos, source.position.y,
                 \zpos, source.position.z);
        
        sources[key].source = synthFunc; 
    }

    /* Alberto:
        funcs could be optimised as SynthDefs, 
        with an arg for listenBusIndex, 
        room could be a KeyBus (synced bus and lang values)
         ... later ... 
    */
        
    /*  method: lightSourceFunc
        create the encoded source function
        Parameter:
            source A mono sound source as NodeProxy.audio
    */
    lightSourceFunc { arg source;
    
        ^{ arg refGain = 0, xpos, ypos, zpos;
            var sourcePositions;
            var distances, gains, phis, thetas, delTimes, gainSource;
            var lx, ly, lz, lo;
            
            #lx, ly, lz, lo = listener.kr(4);

            // direct source + 4 reflections        
            sourcePositions = [
                xpos, ypos, zpos, 
                this.room[0] + (2 * this.room[3]) - xpos, ypos, zpos, 
                this.room[0] - xpos, ypos, zpos, 
                xpos, this.room[1] + (2 * this.room[4]) - ypos, zpos, 
                xpos, this.room[1] -ypos, zpos
            ];
            
            #phis, thetas, distances = sourcePositions.clump(3).collect({ |pos|
                var planeDist;
                planeDist = hypot(pos[1]-ly, pos[0]-lx);
                [atan2(ly-pos[1], pos[0]-lx) + lo, atan2(pos[2]-lz, planeDist), hypot(planeDist, lz - pos[2])];
            }).flop;        

            delTimes = ( distances / 340 );

            //phis.first.poll(1, "phi0");
            //thetas.first.poll(1, "theta0");
            //distances.first.poll(1, "distance0");
            
            gains = (distances + 1).reciprocal.squared; 

            (1..4).do({ | i | gains[i] = gains[i] * refGain });
        
            // sum up the encoded channels of all sources (original + reflections) 
            // DelayL replacement with BufRead....
            DelayL.ar( source.ar, 2, delTimes, gains).collect( { |ch, i| 
                PanAmbi3O.ar(ch, phis[i], thetas[i]); }).sum;
        }
    }

    /*  method: fullSourceFunc
        create the encoded source function
        Parameter:
            source A mono sound source as NodeProxy.audio
    */  
    fullSourceFunc { arg source;
    
        ^{
            var distances, gains, phis, thetas, delTimes, gainSource;
            var sourcePos, listenerPos, phi, theta, planeDist, roomDist; 
            var sourcesPos, sourcesDist; 
            var lx, ly, lz, lo;
            var refGain1 = refGain.kr;
            var refGain2 = refGain1.squared;

            #lx, ly, lz, lo = listener.kr(4);
                                        
            // Source position and listener position
            sourcePos   = source.position.performUnaryOp(\kr);
            listenerPos = Vector3(lx, ly, lz);

            // Calculate the room model:
            //   Source/listener distance and mirror source/listener distances
            sourcesPos  = [sourcePos] ++ roomModel.mirrorSourcePos(sourcePos, listenerPos);
            sourcesDist = roomModel.class.mirrorSourceDist(sourcesPos, listenerPos);
                // [MVSourceDistance(sourcePos, listenerPos)] ++ 
                // roomModel.mirrorSourceDist(sourcePos, listenerPos);

            // Extract angles and distances
            #phis, thetas, distances = (sourcesDist.collect(_.asArray) +.1 [lo.neg, 0, 0]).flop;
            
            // Compute (mirror) source delay time based on distance
            delTimes = roomModel.delayTime(distances)
                       // .poll(1, "delTimes")
                     ;

            //distances.poll(1, "dists");
            // Compute (mirror) source gains (room reflection and air absorption)
            gains = ([1] ++ roomModel.refCoefs)        // Room reflection absorption
                  * (distances + 1).reciprocal.squared // Air absorption
                  //.poll(1, "refGain")
                  * sourcesPos.collect { |s| source.scale(s) }
                  ;
            
            (1..10).do({ | i | gains[i] = gains[i] * if (i.inclusivelyBetween(5,8), refGain1, refGain2) });

            // Sum up the encoded channels of all sources (original + reflections) 
            // DelayL replacement with BufRead....
            DelayL.ar(source.source.ar, roomModel.maxDelayTime, delTimes, gains).collect( { |ch, i|
                PanAmbi3O.ar(ch, phis[i], thetas[i]); }).sum;
        }
    }
}
