/*Ramp interpolation is better than Lag, see figure from notes. 
Then, if interaction smaller than Lagtime, is not a problem because the function
compute from the last value the interpolation is in the istante a new value is set, so
the interpolation is still continuous.  
*/

(				
SynthDef(\RampInteraction, { arg bufnum, amp = 1, out = 0, dur, pan = 0, durLag = 0.1;
			 //if cutfreq>22000 LPF will not correctly work. //Line in order to control the amp generating a line from the last amp value and the next one  
							Out.ar(out,
									Pan2.ar(
										(
										EnvGen.kr(Env.new([0,1,1,0], [dur*0.05, dur*0.9, dur*0.05]), doneAction:2) 
										//now all the samples have fad in nd fade out depending on their dur
										//doneaction will free the synth at the end of control line.kr = dur 
										*
										PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), loop: 0)
										*												Ramp.kr(amp,durLag) //perform a smooth change between 2 consecutive values of amp
										), Ramp.kr(pan,durLag)) //also pan has discrete behaviour so a Line control is needed
								,)
							}.send(s);
)


{ |amp = 1, durLag = 1 |
	Ramp.kr(amp,durLag);
}.scope;



Server.internal.boot;
(
// used to lag pitch
f = { arg int = 1;
	SinOsc.ar(
		800,	
		0,	// phase
		Ramp.kr(int,0.5) //amp
	) 
}.scope;
)


f.set(\int, 0);
f.set(\int, 1);


f = {arg x =0, y = 1; (x+y)};
f(1,1);