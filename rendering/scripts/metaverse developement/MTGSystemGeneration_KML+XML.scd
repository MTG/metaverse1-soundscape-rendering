//NOTE: this soundscape is just a test, have no sound design meaning, also the events are randomlly segmented


(
//REPLACE THE PATH pass the kml annotation path and the dabase folder path
~kmlAnnotation = "/Users/mattia/git_workspace/metaverse/supercollider/scripts/metaverse developement/Canary Island Virtual Travel/virtualTravel_example.kml";
~soundscapeDatabase = "/Users/mattia/git_workspace/metaverse/supercollider/scripts/metaverse developement/Canary Island Virtual Travel";
k = KMLSoundscape.new;

k.parse(~kmlAnnotation, ~soundscapeDatabase);

)

//then you can send OSC to interact with the soundscape created
//first, in order to ear the soundscape you just need to create a listener, using the OSC message '$soundscapeName$/add/entity/listener' -> arg id 
//like this:
m = NetAddr("127.0.0.1", 57120); //local host address

m.sendMsg('Canary Island Virtual Travel/add/entity/listener', 0);

//other useful interaction messages
m.sendMsg('Canary Island Virtual Travel/remove/entity/listener', 0);

m.sendMsg('Canary Island Virtual Travel/spatdif/core/listener/0/position',10,100);

m.sendMsg('Canary Island Virtual Travel/spatdif/core/listener/0/direction', 3.14);

m.sendMsg('Canary Island Virtual Travel/spatdif/core/listener/0/getPosition');

m.sendMsg('Canary Island Virtual Travel/square/gain', 1);

m.sendMsg('Canary Island Virtual Travel/square/children/gain', 0);

m.sendMsg('Canary Island Virtual Travel/square/children/prob', 1);




//Some info about the generation just performed

/*
~world = k.soundscapeDict["Canary Island Virtual Travel".asSymbol][0];
~world.soundscapeName;
~world.scapeWidth;
~world.scapeWidth;

//the zones names
~world.soundZones.do({arg zone; zone.name.postln});

//replace the symbol \square with the name of your zone.
~world.soundZonesDict[\square].conceptDict.do({arg conc; conc.getName.postln;});
~world.soundZonesDict[\square].conceptDict.do({arg conc; conc.getParameters.postln;});
(
~world.soundZonesDict[\square].conceptDict.do({arg conc; 
	"///////////////////////".postln;
	conc.getName.postln;
	conc.events.do({arg event;
		event.info;
		
		});});
)
*/
