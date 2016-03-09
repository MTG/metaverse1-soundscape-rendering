


//Example with a stereophonic sample balance
l = Buffer.readChannel(s, "/Users/mattia/Documents/Soundscape/MetaverseUseCase/squareAtm2.wav", channels: [0]);
r = Buffer.readChannel(s, "/Users/mattia/Documents/Soundscape/MetaverseUseCase/squareAtm2.wav", channels: [1]);

(

x = SynthDef(\balance, {arg out = 0, bufnumL, bufnumR, balance;
						Out.ar(out, 
							Balance2.ar( PlayBuf.ar(1, bufnumL, BufRateScale.kr(bufnumL)),
									 PlayBuf.ar(1, bufnumR, BufRateScale.kr(bufnumR)),
						  			balance)
						)
					}
			)
)
h = x.play(s,[ \bufnumL, l.bufnum, \bufnumR, r.bufnum, \balance, 0]);
h.set( \balance, -1);
h.set( \balance, 0);	
h.set( \balance, 1);	



s.boot;

//Example with a stereophonic sample pan -> It use just one channel, if you use two doesn't make sense. 
l = Buffer.read(s, "/Users/mattia/Documents/Soundscape/MetaverseUseCase/squareAtm2.wav");

(
x = SynthDef(\pan, {arg out = 0, bufnum, pan;
						Out.ar(out, 
							Pan2.ar( PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum)),pan
							)
						)
			})
)
h = x.play(s,[ \bufnum, l.bufnum, \pan, 0]);
h.set( \pan, -1);
h.set( \pan, 0);	
h.set( \pan, 1);	