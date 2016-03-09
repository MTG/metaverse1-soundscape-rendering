
//MP3 Quark don't seems to work...
//filter mp3 -> don't use samples that are mp3
//the Freesound API just provide mp3?
Quarks.gui
MP3.lamepath;

MP3.curlpath;

// To check that there's a file at the expected path

File.exists(MP3.lamepath)

File.exists(MP3.curlpath)

SoundFile


s.boot // Boot the server, if you need to 

// read a whole sound into memory
b = Buffer.read(s, "2/Users/mattia/Downloads/tmp/serve?api_key=ed72f88446254f92a66946c7d67ebcda.aiff" ); // remember to free the buffer later.


//it is an mp3 he cannot read it!!!!
b = Buffer.read(s, "/Users/mattia/Downloads/tmp.mp3"); // remember to free the buffer later.


SynthDef(\help_PlayBuf, {| out = 0, bufnum = 0 |
	Out.ar(out, 
		PlayBuf.ar(1, bufnum, BufRateScale.kr(bufnum), doneAction:2)
	)
}).play(s, [\out, 0, \bufnum, b]);

Quarks.gui



(
f = SoundFile.new;
f.openRead("/Users/mattia/Downloads/tmp/serve?api_key=ed72f88446254f92a66946c7d67ebcda");
f.headerFormat;

f.inspect;

f.close;
)

"cd /Users/mattia/Downloads/tmp".unixCmd
"curl -O 'http://tabasco.upf.edu/api/sounds/77499/serve?api_key=ed72f88446254f92a66946c7d67ebcda'".unixCmd

"curl -O 'http://tabasco.upf.edu/api/sounds/77499/serve?api_key=ed72f88446254f92a66946c7d67ebcda'".postln;


"mv /Users/mattia/Downloads/tmp/serve?api_key=ed72f88446254f92a66946c7d67ebcda /Users/mattia/Downloads/tmp/serve?api_key=ed72f88446254f92a66946c7d67ebcda.aiff".unixCmd