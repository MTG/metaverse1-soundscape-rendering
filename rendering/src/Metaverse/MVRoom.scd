s.options.blockSize_(128);			// sets kr blocksize; default = 64
s.options.memSize_(8192 * 16);		// more memory for delay lines
s.reboot;							// dont forget to reboot the server after this
p = ProxySpace.push;

MVRoom.kemarPath = "KemarHRTF/";
m = MVRectRoomModel(Vector3(5, 5, 5), 1.dup(10), 1, 0, 0);
v = MVRoom.new(m).init;

v.play;
v.stop;

// listener is a NodeProxy.kr, set the source
v.listener.source = { [ MouseY.kr(5,0), MouseX.kr(5,0), 2.5, 0] };

// more static
v.listener.source = { |x=2.5, y=4, z=2.5, o=0| [ x, y, z, o] };

v.listener.source = { |x=1, y=4, z=2.5| [ x, y, z, LFTri.kr(0.1).range(-pi,-pi/2)] };
v.listener.source = { |x=1, y=4, z=2.5| [ x, y, z, 0] };

v.refGain.set(\refGain, 0);

// adding sources to the scene
(
~noisy = { EnvGen.kr(Env.adsr, Impulse.kr(3)) * PinkNoise.ar(1) };
~sinus = { EnvGen.kr(Env.adsr, Impulse.kr(3)) * SinOsc.ar(440,0.8) };
~dust = { Dust.ar(400) };
)

Vector3(1, 2, 2.5).angle((Vector3(1, 2, 2.5) sub_v: Vector3(0,-1,0)))

MVRadiationCone(0, pi/16).containsAngle(Vector3(1,0,0) angle: (Vector3(1,4,2.5) sub_v: Vector3(1,2,2.5)))
MVRadiationCone(0, pi/16).containsAngle())
MVRadiationCone(0, pi/16).isOmni
(Vector3(1,0,0) angle: (Vector3(1,4,2.5) sub_v: Vector3(1,2,2.5))).linlin(pi/16,pi/16,0, -inf).dbamp
                                       
v.addSource( \test1, MVSource(~noisy, Vector3(1, 2, 2.5)) );
v.addSource( \test1, MVSource(~noisy, Vector3(1,2,2.5), Vector3(1,0,0), MVRadiationCone(0, pi/16), MVRadiationCone(-30, pi/8)) );
v.addSource( ~sinus, \test2, [4, 7, 2.5]);
v.addSource( ~dust, \test3, [2, 5, 2.5]);

// change the source position
v.sources[\test1].setn(\xpos, [2.5, 3, 2.5]);		// right
v.sources[\test1].setn(\xpos, [2.5, 5, 2.5]);		// left
v.sources[\test1].setn(\xpos, [2, 4, 2.5]);		// back
v.sources[\test1].setn(\xpos, [3, 4, 2.5]);		// front

// change the listener position
v.listener.setn(\x, [3, 5, 2.5, 0])				// source to the right
v.listener.setn(\x, [3, 5, 2.5, pi/2])			// turn, source to the front


// remove the sources
v.removeSource(\test1);
v.removeSource(\test2);
v.removeSource(\test3);


// the light version of addSource is used if a lot of sources are needed
// consumes 63% CPU on iBook G4 1Ghz
(
x = (1..6).collect ({ |i|
	var test, testpos;
	test = NodeProxy.audio(s,1); 
	test.source = { EnvGen.kr(Env.adsr, Impulse.kr(3)) * SinOsc.ar(440.rand,0.6) };
	v.addSourceLight(test, i.asString, [1 , 4, 2.5] );
	test;
});
)

// remove the sources and free the node proxies
6.do ({ |i|  v.removeSource(i.asString); x[i].free; });

// adjust the room properties as you like
v.gui;

// Create a second virtual room
w = VirtualRoom.new;

// initialise rendering
w.init;

// set the room properties (reverberation time and gain,
// hf damping on reverb and early reflections gain
(
w.revTime = 0;
w.revGain = 0;
w.hfDamping = 1;
)

// same listener, same room dimensions
w.listener.source = { |x=2.5, y=4, z=2.5, o=0| [ x, y, z, o] };
w.room = [0, 0, 0, 5, 8, 5];

// play it on channels 2, 3
w.out.playN([2,3]);
~dust = { Dust.ar(400) };
w.addSource( ~dust, \test3, [3, 3, 2.5]);
w.removeSource(\test3);

// free the virtual room
v.free;
w.free;

// remove the ProxySpace
p.pop;

