//CANARY ISLAND: LAS PALMAS. SOUNDSCAPE COMPOSITION. UPDATED at MULTIPLE LISTENER & SEQUENCING + SPATIALISATION Audio Engines


(
var soundscapeDatabase;

//Soundscape controller variables
~world = SoundWorld.new(scapeWidth:256, scapeHeight:256, name:"VirtualIsland");

~zone_beach; //those are variables that last, Environment class
~zone_square;
~zone_museum_outside;
~zone_museum_inside;

//DATABASE API 

soundscapeDatabase = SoundscapeGenerator.currentSoundscapePath;
(soundscapeDatabase+"passed by SoundscapeGenerator").postln;

~beach_database = soundscapeDatabase +/+ "beach_database" ;
~square_database = soundscapeDatabase +/+ "square_database/conceptfolder/square";
~museum_outside_database = soundscapeDatabase +/+ "museum_out_database" ;
~museum_inside_database = soundscapeDatabase +/+ "museum_in_database" ;

/*

~beach_database = "/home/metaverse/metaverse/supercollider/scripts/soundscape-streaming/sounds/virtualtravel_database/beach_database/" ;
~square_database = "/home/metaverse/metaverse/supercollider/scripts/soundscape-streaming/sounds/virtualtravel_database/square_database/conceptfolder/square/";
~museum_outside_database = "/home/metaverse/metaverse/supercollider/scripts/soundscape-streaming/sounds/virtualtravel_database/museum_out_database/" ;
~museum_inside_database = "/home/metaverse/metaverse/supercollider/scripts/soundscape-streaming/sounds/virtualtravel_database/museum_in_database/" ;

*/


//Init condition and OSCresponder to wait asynchronous threads end
c = Condition.new(false);
r  = OSCresponder(nil, '/synced',
{|time, resp, msg| 
	msg.post;
	"_server osc respose".postln;
	c.test = true;
	c.signal;} ).add;
	


//////////////////////////////////////Init Soundscape Generation System
a = Graph.new ;
b = Runner.new(a, name: \virtualIsland);
p = GraphParser(a, b); //parser for high level command to create manual graph, see below square toll and bellsmelody concept. 



{ //forked function

//%%%%%%%%%%%%%%%%%%%%%%%
//CREATE SOUND ZONES  %%%
//%%%%%%%%%%%%%%%%%%%%%%%


//square zone
//1 asynchronous commads
~zone_square = MtgModel.new(b,Server.internal,name:"square").initAudio; //a sound zone or sound layer
c.wait;
c.test = false;
//0 asynchronous commads
~square_database.postln;
~zone_square.loadConceptFoldersDatabase(~square_database, eventNameIsFileName:true); //square database as not annotation but is organised as follow:
							// a root folder (method arg) is the zone, the subfolder are the concepts,
							// each concept contains the events samples
"*_* clang said: server Buffers loaded from Square Database".postln;




//virtual_museum_outside zone
//1 asynchronous commads
~zone_museum_outside = MtgModel.new(b,Server.internal,name:"Museumoutside").initAudio; //a sound zone or sound layer
c.wait;
c.test = false;
//2 asynchronous commads
~zone_museum_outside.loadAnnotatedDatabase(~museum_outside_database);
c.wait;
c.test = false;
"*_* clang said: Museum outside Analysis Recordings Loaded".postln;
c.wait;
c.test = false;
"*_* clang said: server Buffers loaded from annotation".postln;


//virtual_museum_inside
//1 asynchronous commads
~zone_museum_inside = MtgModel.new(b,Server.internal,name:"Museuminside").initAudio; //a sound zone or sound layer
c.wait;
c.test = false;
//2 asynchronous commads
~zone_museum_inside.loadAnnotatedDatabase(~museum_inside_database);
c.wait;
c.test = false;
"*_* clang said: Museum inside Analysis Recordings Loaded".postln;
c.wait;
c.test = false;
"*_* clang said: server Buffers loaded from annotation".postln;


//beach zone 
//1 asynchronous commads
~zone_beach = MtgModel.new(b,Server.internal,name:"Beach").initAudio; //a sound zone or sound layer
c.wait;
c.test = false;
//2 asynchronous commads
~zone_beach.loadAnnotatedDatabase(~beach_database);
c.wait;
c.test = false;
"*_* clang said: Beach Analysis Recordings Loaded".postln;
c.wait;
c.test = false;
"*_* clang said: server Buffers loaded from annotation".postln;


r.remove; //asynchronous commads end, OSCListener remove, no more usefull.


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//CREATE SOUND WORLD & LISTENERS %%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

~world.initZoneArray([~zone_beach, ~zone_museum_outside, ~zone_museum_inside, ~zone_square]); 
//l = MtgListener.new(117,53, ~world);

//~listener2 = MtgListener.new(150,53, ~world, uid:2, channelOut:2);


//%%%%%%%%%%%%%%%%%%%
//MIXING & CONTROLLING ZONES GENERAL AMPS (gain) & AMPS ATTENUTENUATION BASED ON DISTANCE (m)

~zone_beach.setVirtualUnitMeterRatio(1/6); 
~zone_beach.setGain(1.8);

~zone_museum_outside.setVirtualUnitMeterRatio(1/6);
~zone_museum_outside.setGain(6);

~zone_museum_inside.setVirtualUnitMeterRatio(1/3);
~zone_museum_inside.setGain(4);
~zone_museum_inside.setCloseAmbient([108,37,18,32]);


~zone_square.setCloseAmbient([145,36,48,180]);
~zone_square.setVirtualUnitMeterRatio(1/3);
~zone_square.setGain(4);

//%%%%%%%%%%%%%%%%%%%%


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//CREATE SOUND CONCEPT & ASSIGN PARAMETERS 
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

//MUSEUM OUTSIDE

~zone_museum_outside.getConcept(("museum_park_atm").asSymbol).setPosition([102,49,100,68]);
~zone_museum_outside.getConcept(("museum_park_atm").asSymbol).setContinuous();

//SQUARE


//manual graph START_________
//bell: 2 sound concept for 1 soundscape agent

~zone_square.getConcept(\toll).setGeneral(false); //do not create the automatic graph for this concept when invoke .generateGraph();
~zone_square.getConcept(\bellsmelody).setGeneral(false);
//this command create vertex, edges and the vertex name that must contain also the information of the zone. 
p.parse("e+ squaretoll 10 squarebellsmelody 3400 squaretoll") ; // Prob = 1/7 (in real soundscape it relates with 8 and consecutive 12 tolls, 8 a.m. and noon). 
p.parse("e+ squaretoll 2 squaretoll 2 squaretoll 2 squaretoll 2 squaretoll 2 squaretoll 3500 squaretoll") ; //5 edges create a toll loop sequence, probability 5/7. an edge go to the next hour, probability = 1/7

//set position of events
a.changeVertexPosition(a.getvID("squaretoll"),154,79); 
~zone_square.getConcept(\toll).setPosition([154,79]); //the pos information need to be stored also in concept or any event where it is different from the concept one.
a.changeVertexPosition(a.getvID("squarebellsmelody"),154,79);
~zone_square.getConcept(\bellsmelody).setPosition([154,79]);
p.parse("a+ squaretoll") ;	//add actant to the bell graph
//name the event as the vertex you just created


//fountain: 1 concept, 5 events
~zone_square.getConcept(\fountain).setGeneral(false);
p.parse("e+ squarefountain 59 squarefountain") ; //normal activity loop 1/5 P
p.parse("e+ squarefountain 59 squarefountain2 6 squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ squarefountain 59 squarefountain3 3 squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ squarefountain 59 squarefountain4 3 squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ squarefountain 59 squarefountain5 7 squarefountain") ; //subject interaction event 1/5 P
p.parse("a+ squarefountain") ;	//add actant
//set position of events
a.changeVertexPosition(a.getvID("squarefountain"),179,101); 
a.changeVertexPosition(a.getvID("squarefountain2"),179,101); 
a.changeVertexPosition(a.getvID("squarefountain3"),179,101); 
a.changeVertexPosition(a.getvID("squarefountain4"),179,101); 
a.changeVertexPosition(a.getvID("squarefountain5"),179,101); 
//store concept pos information
~zone_square.getConcept(\fountain).setPosition([179,101]);

//manual graph STOP______

//general concept, parameter for automatic graph creation

~zone_square.getConcept(\pigeon).setPosition([165,117]);
~zone_square.getConcept(\pigeon).setAr(4);
~zone_square.getConcept(\pigeon).setProb(35);
~zone_square.getConcept(\pigeon).setMultiplePath(2);


~zone_square.getConcept(\children).setPosition([158,122]);
~zone_square.getConcept(\children).setAr(5);
~zone_square.getConcept(\children).setProb(80);
~zone_square.getConcept(\children).setMultiplePath(2);


~zone_square.getConcept(\steps).setPosition([170,211]);
~zone_square.getConcept(\steps).setAr(3);
~zone_square.getConcept(\steps).setProb(30);


~zone_square.getConcept(\footsteps).setPosition([184,187]);
~zone_square.getConcept(\footsteps).setAr(3);
~zone_square.getConcept(\footsteps).setProb(35);
~zone_square.getConcept(\footsteps).setMultiplePath(2);


~zone_square.getConcept(\cyclist).setPosition([165,117]);
~zone_square.getConcept(\cyclist).setAr(3);
~zone_square.getConcept(\cyclist).setProb(20);

~zone_square.getConcept(\squareAtm).setPosition([169,126,48,180]);
~zone_square.getConcept(\squareAtm).setContinuous();




//MUSEUM INSIDE

~zone_museum_inside.getConcept(("museum_inside_atm").asSymbol).setPosition([117,53,18,32]);
~zone_museum_inside.getConcept(("museum_inside_atm").asSymbol).setContinuous();

~zone_museum_inside.getConcept(\entrance).setPosition([109,56]);
~zone_museum_inside.getConcept(\entrance).setAr(2);

~zone_museum_inside.getConcept(\footsteps).setPosition([108,37]);
~zone_museum_inside.getConcept(\footsteps).setAr(3); 
~zone_museum_inside.getConcept(\footsteps).randomPosition = [18,32];
~zone_museum_inside.getConcept(\footsteps).setMultiplePath(4);

~zone_museum_inside.getConcept(\voices).setPosition([108,37]);
~zone_museum_inside.getConcept(\voices).setAr(3); 
~zone_museum_inside.getConcept(\voices).randomPosition = [18,32];
~zone_museum_inside.getConcept(\voices).setMultiplePath(3);

~zone_museum_inside.getConcept(\armchair).setPosition([113,64]);

//BEACH

~zone_beach.getConcept(("Clapping-Thongs").asSymbol).setPosition([90, 150]); 
~zone_beach.getConcept(("Clapping-Thongs").asSymbol).setProb(10);

//object
~zone_beach.getConcept(\FlappingFlag).setPosition([114,177]);
~zone_beach.getConcept(\FlappingFlag).setContinuous();
~zone_beach.getConcept(\FlappingFlag).setEventsRecordingDistance(2.9);

~zone_beach.getConcept(\FlappingFlag_I).setPosition([114,149]);
~zone_beach.getConcept(\FlappingFlag_I).setContinuous();
~zone_beach.getConcept(\FlappingFlag_I).setEventsRecordingDistance(2.9);


~zone_beach.getConcept(\FlappingFlag_II).setPosition([114,129]);
~zone_beach.getConcept(\FlappingFlag_II).setContinuous();
~zone_beach.getConcept(\FlappingFlag_II).setEventsRecordingDistance(2.9);



~zone_beach.getConcept(\Shower).setPosition([80,131]);
~zone_beach.getConcept(\Shower).setProb(30);


~zone_beach.getConcept(\BeachBar).setPosition([70,178]);
~zone_beach.getConcept(\BeachBar).setProb(180);
~zone_beach.getConcept(\BeachBar).setAr(3);
~zone_beach.getConcept(\BeachBar).setMultiplePath(4);

//people
~zone_beach.getConcept(\Bingo).setProb(10);
~zone_beach.getConcept(\Bingo).setPosition([80, 122]); 
~zone_beach.getConcept(\Bingo).setAr(4);


~zone_beach.getConcept(\BeachTennis).setProb(40);
~zone_beach.getConcept(\BeachTennis).setPosition([74, 196]);
~zone_beach.getConcept(\BeachTennis).setAr(4);


~zone_beach.getConcept(\Voices).setPosition([34, 108]);
~zone_beach.getConcept(\Voices).setAr(3);
~zone_beach.getConcept(\Voices).setProb(30);
~zone_beach.getConcept(\Voices).randomPosition = [78,110];
~zone_beach.getConcept(\Voices).setMultiplePath(4); 

~zone_beach.getConcept(\BeachFootball).setProb(20);
~zone_beach.getConcept(\BeachFootball).setPosition([87, 180]);
~zone_beach.getConcept(\BeachFootball).setAr(3);

~zone_beach.getConcept(\WaterFootsteps).setPosition([30,73]);
~zone_beach.getConcept(\WaterFootsteps).randomPosition = [14,140];
~zone_beach.getConcept(\WaterFootsteps).setMultiplePath(2);
~zone_beach.getConcept(\WaterFootsteps).setProb(10);

~zone_beach.getConcept(\BeachFootbridge).setPosition([64,143]);
~zone_beach.getConcept(\BeachFootbridge).randomPosition = [47,5];
~zone_beach.getConcept(\BeachFootbridge).setAr(3);
~zone_beach.getConcept(\BeachFootbridge).setProb(25);
~zone_beach.getConcept(\BeachFootbridge).setMultiplePath(2);

//sea

//randomPosition seems better than Atm for waves, in fact wave are point source...sea as an atm, but not waves. 
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setAr(4);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setPosition([30,73]);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).randomPosition = [14,140];
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setMultiplePath(4);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setContinuous();
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setEventsRecordingDistance(2.5); //2m this means ask the system to de-amplify the concept
//IT WORKS, a perceptible difference is noted with very low value as: distance = 1, 0.5, 0.05, m

//Design associating different waves nuances for temporal evolution (time states)
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setPosition([30,73]);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).randomPosition = [14,140];
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setAr(4);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setMultiplePath(3);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setContinuous();
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).active = false;


//TODO: Wait for concept.limit(anArea); that this.active = true;
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setPosition([56,225]);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).randomPosition = [36,22];
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setAr(4);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setMultiplePath(3);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setContinuous();
//~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).active = false;
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).conceptGain = 0.6;

//street
~zone_beach.getConcept(\Palm).setPosition([64,102]);
~zone_beach.getConcept(\Palm).setProb(10);
~zone_beach.getConcept(\Palm_I).setPosition([75,104]);
~zone_beach.getConcept(\Palm_I).setProb(10);
~zone_beach.getConcept(\Palm_II).setPosition([86,104]);
~zone_beach.getConcept(\Palm_II).setProb(10);
~zone_beach.getConcept(\Palm_III).setPosition([99,104]);
~zone_beach.getConcept(\Palm_III).setProb(10);

~zone_beach.getConcept(\Beach_steps).setPosition([114,179]);
~zone_beach.getConcept(\Beach_steps).setProb(10);

~zone_beach.getConcept(\Beach_steps_I).setPosition([114,134]); 
~zone_beach.getConcept(\Beach_steps_I).setProb(10);

~zone_beach.getConcept(\Beach_steps_II).setPosition([93,113]); 
~zone_beach.getConcept(\Beach_steps_II).setProb(10);

~zone_beach.getConcept(("Passer-by").asSymbol).setPosition([112,84]);
~zone_beach.getConcept(("Passer-by").asSymbol).setProb(60);
~zone_beach.getConcept(("Passer-by").asSymbol).setAr(4);
~zone_beach.getConcept(("Passer-by").asSymbol).randomPosition = [19,100];
~zone_beach.getConcept(("Passer-by").asSymbol).setMultiplePath(4);


~zone_beach.getConcept(("Runner-by").asSymbol).setPosition([115,155]); 
~zone_beach.getConcept(("Runner-by").asSymbol).setAr(2);
~zone_beach.getConcept(("Runner-by").asSymbol).setProb(5);

~zone_beach.getConcept(("Roller-by").asSymbol).setPosition([114,115]);
~zone_beach.getConcept(("Roller-by").asSymbol).setAr(2);
~zone_beach.getConcept(("Roller-by").asSymbol).setProb(4);

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%_END
//CREATE SOUND CONCEPT & ASSIGN PARAMETERS_END 
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%_END




//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//AUTOMATIC GRAPH GENERATION %%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

~zone_museum_inside.generateGraphs(a);
~zone_museum_outside.generateGraphs(a); //need a Graph instance
~zone_beach.generateGraphs(a);
~zone_square.generateGraphs(a);


"%%%%%%%%%%%%%%%%".postln;
"AUTOMATIC GRAPH GENERATION COMPLETE".postln;
"%%%%%%%%%%%%%%%%".postln;


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//START SOUNDSCAPE GENERATION %%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
1.wait; //Just to be sure
b.startAll;
"generation Active".postln;

//THIS WILL RETURN THE SOUNDSCAPE TO THE SoundscapeGenerator.sc that interpret this script


}.fork;

~world;
)





/*

//query the synthesis architecture status
s.queryAllNodes;



l.setPosition(118,53); //museum inside
l.setPosition(100,54); //museum outside
l.setPosition(117,70); //museum outside near beach
l.setPosition(150, 150); //square
l.setPosition(80,131); //beach
//near bingo
l.setPosition(82, 124);
//near bar 
l.setPosition(75,180);
//in street, near people, runner-by, roller-by
l.setPosition(70, 190);
//near beach footbridge
l.setPosition(64,143);



*/


