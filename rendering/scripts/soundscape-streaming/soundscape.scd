(
var dir = "/home/skersten/src/metaverse/supercollider/scripts/soundscape-streaming";
var density = 1;

var mkTraffic = { | name, density, dmin, dmax, step, gmin, gmax, gstep, offset=0 |
	var bufnum = trafficSounds[name].bufnum;
	Pseq([
		Event.silent(offset),
		Pseq([
			Pbind(
				\instrument, \traffic,
				\dur, Pbrown(dmin, dmax, step, density),
				\bufnum, bufnum
			),
			Pbind(
				\type, \rest,
				\dur, Pbrown(gmin, gmax, gstep, 1)
			)
		], inf);
	])
};

var mkVoice = { | buffer, offset, grainSize, dur |
	Pbind(
		\instrument, \voices,
		\dur, dur,
		\envDur, grainSize,
		\startpos, offset,
		\bufnum, buffer.bufnum
	);
};

var trafficSounds, voiceSounds, prepare = {
	SynthDef("voices", { | out=0, lvl=0, bufnum, pan=0, startpos=0, envDur=10 |
		var env = EnvGen.ar(Env.sine(envDur), doneAction: 2);
		var x = PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), startPos: startpos*SampleRate.ir);
		var z = Pan2.ar(LPF.ar(HPF.ar(x * lvl.dbamp * env, 200), 7000), pan);
		Out.ar(out, z)
	}).store;

	SynthDef("traffic", { | out=0, lvl=0, bufnum, pan=0, low_cut=10000 |
		var x = Pan2.ar(LPF.ar(PlayBuf.ar(1,bufnum,BufRateScale.kr(bufnum)) * lvl.dbamp, low_cut), pan/100);
		var env = Line.ar(1, 1, BufDur.kr(bufnum), doneAction: 2);
		Out.ar(out, x*env)
	}).store;

	trafficSounds = (
    		slow_light:  Buffer.read(s, dir +/+ "sounds/TRAFFIC/VEHICLES SLOW/LIGHT CAR2 (si_06_27-1).wav"),
    		slow_medium: Buffer.read(s, dir +/+ "sounds/TRAFFIC/VEHICLES SLOW/MEDIUM CAR (si_05_27-1).wav"),
    		slow_large:  Buffer.read(s, dir +/+ "sounds/TRAFFIC/VEHICLES SLOW/LARGE CAR3 (si_08_27-1).wav"),
    		slow_truck:  Buffer.read(s, dir +/+ "sounds/TRAFFIC/VEHICLES SLOW/TRUCK3 (si_33_88-3).wav")
    	);
    	
    	voiceSounds = ["sounds/VOICES/66606__snippet1.wav", "sounds/VOICES/69671__snippet1.wav", "sounds/VOICES/69823__snippet1.wav", "sounds/VOICES/66615_snippet1.wav"].collect { |x| Buffer.read(s, (dir +/+ x).postln ) };

    	s.sync;

	// Traffic    	
    	mkTraffic.value(\slow_light,  1, 1.0, 3.0, 0.5, 8.0, 10.0, 1.0).trace.play;
    	mkTraffic.value(\slow_medium, 1, 1.0, 3.0, 0.5, 8.0, 10.0, 1.0, 2.0).trace.play;
    	mkTraffic.value(\slow_large,  1, 1.0, 3.0, 0.5, 8.0, 10.0, 1.0, 5.0).trace.play;
    	mkTraffic.value(\slow_truck,  1, 1.0, 3.0, 0.5, 8.0, 10.0, 1.0, 4.0).trace.play;

	// Voices
	mkVoice.value(voiceSounds[0], Pfunc { rrand(0.0, 30.0) }, 10.0, 2.2).trace.play;
	mkVoice.value(voiceSounds[1], Pfunc { rrand(0.0, 30.0) }, 10.0, 2.2).trace.play;
	mkVoice.value(voiceSounds[2], Pfunc { rrand(0.0, 30.0) }, 10.0, 2.2).trace.play;
};
setenv("SC_JACK_DEFAULT_OUTPUTS", "darkice-metaverse1");
s.boot.doWhenBooted({ fork(prepare) });
)
