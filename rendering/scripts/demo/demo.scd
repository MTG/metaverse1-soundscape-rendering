/*RUN*/

s.options.blockSize_(128);			// sets kr blocksize; default = 64
s.options.memSize_(8192 * 16);		// more memory for delay lines
s.reboot;							// dont forget to reboot the server after this

p = ProxySpace.push;

s.doWhenBooted {

// synthdefs
SynthDef(\mv_sample, { | out=0, gate=1, bufnum(-1) |
    var env = EnvGen.kr(Env.asr(0.05, 1, 0.05), gate: gate, doneAction: 2);
    var z   = PlayBuf.ar(1, bufnum, rate: BufRateScale.kr(bufnum)) * env;
    Out.ar(out, z);
}).store;

SynthDef(\mv_sample_loop, { | out=0, bufnum |
    var z = PlayBuf.ar(1, bufnum, loop: 1, rate: BufRateScale.kr(bufnum));
    Out.ar(out, z);
}).store;

v = VirtualRoom.new;
d = MVDemo("/Volumes/data/metaverse1", s, v, Vector3(15, 0, 10));
d.init;
s.sync;

v.room = [0, 0, 0, 20, 10, 6];
v.revGain = 0.1;
v.hfDamping = 0.5;
v.refGain = 0.8;
v.play;

//v.listener.source = { [ MouseY.kr(5,0), MouseX.kr(10,0), 1.8, 0] };
v.listener.source = { |x=0, y=0, z=0, o=0| [ x, y, z, o] };

OSCresponder(nil, "/position", { | time, resp, msg, addr |
	//msg.postln;
	d.position_(Vector3(msg[1], msg[2], msg[3]));
	if (d.position.x < 10) {
//		\in_carpet.postln;
		~in_carpet.set(\value, 1);
	} {
//		\out_carpet.postln;
		~in_carpet.set(\value, 0);
	};
}).add;
OSCresponder(nil, "/orientation", { | time, resp, msg, addr |
	//msg.postln;
	d.orientation_(Vector3(msg[1], msg[2], msg[3]));
}).add;
OSCresponder(nil, "/velocity", { | time, resp, msg, addr |
	var carpet = nil;
	var speed = msg[1];
	if (speed < 0.05) {
		~steps_amp.set(\value, 0);
	} {
		if (true) {
			~steps_amp.set(\value, -6.dbamp);
		};
	};
}).add;

// adding sources to the scene
~traffic.ar(1);
~traffic = \mv_sample_loop;
0.5.wait;
~traffic.set(\bufnum, d.buffers["traffic_LPfilt1khz_si_29_01-1"].bufnum);
// ~traffic.play;
// ~traffic.stop;

//~clock.ar(1);
//~clock = \mv_sample_loop;
//~clock.set(\bufnum, d.buffers["_NR_LP_amp_seg_44101__daveincamas__Pendulum_MONO"].bufnum);
// ~clock.play;

//~bird.ar(1);
//~bird.source = 
//	Pbind(
//		\instrument, \mv_sample,
//		\buffer, Pshuf(d.buffers.select { |v,k| k.contains("Canary") }.values, inf),
//		\bufnum, Pkey(\buffer).collect(_.bufnum),
//		\dur, Pkey(\buffer).collect(_.duration) + Pfunc { exprand(8,16) }
//	);
// ~bird.play;

// computer
~computer.ar(1);
~computer = \mv_sample_loop;
0.5.wait;
~computer.set(\bufnum, d.buffers["52196__sentryx86__Computer_Noise"].bufnum);
~computer2 = ~computer * 3.dbamp;
// ~computer.play;
// ~computer.stop;

// insect
~insect.ar(1);
~insect = \mv_sample_loop;
0.5.wait;
~insect.set(\bufnum, d.buffers["21891__dobroide__20060804.marshes.cricket02"].bufnum);
// ~insect.play;
// ~insect.stop;

// steps
~steps_hard.clear;
~steps_hard.ar(1);
~steps_hard = \mv_sample_loop;
0.5.wait;
~steps_hard.set(\bufnum, d.buffers["footsteps_male_hard_floor_10steps_si_17_45-1"].bufnum);

~steps_soft.ar(1);
~steps_soft = \mv_sample_loop;
0.5.wait;
~steps_soft.set(\bufnum, d.buffers["footsteps_male_hard_floor_carpet_10steps_si_17_45-1"].bufnum);

~in_carpet.kr(1);
~in_carpet = { | value=0 | value };

~steps_amp.kr(1);
~steps_amp = { | value=0| value };

~steps.ar(1);
~steps = {
	var a = ~in_carpet.kr, b = ~steps_amp.kr;
	((a * ~steps_soft.ar) + ((1-a) * ~steps_hard.ar)) * b
};

1.wait;

v.addSourceLight( ~traffic,    \traffic,    d.mapPosition(Vector3(4,1.5,0)).asArray        );
//v.addSourceLight( ~clock,      \clock,      d.mapPosition(Vector3(0,2,5)).asArray          );
//v.addSourceLight( ~bird,       \bird,       d.mapPosition(Vector3(4.75,0.75,9.75)).asArray );
v.addSourceLight( ~computer2,  \computer,   d.mapPosition(Vector3(1.75,1,12)).asArray          );
v.addSourceLight( ~insect,     \insect,     d.mapPosition(Vector3(8,0.75,6.75)).asArray );
v.addSourceLight( ~steps,      \steps,      d.mapPosition(Vector3(0,0,0)).asArray          );

}; // doWhenBooted