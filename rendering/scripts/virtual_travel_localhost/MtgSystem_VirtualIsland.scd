//CANARY ISLAND: LAS PALMAS. SOUNDSCAPE COMPOSITION

(

var visualisation = false; //if you want visualisation set this variable to true, but first care about visualisation Image API
//Soundscape controller variables

//those with ~ are variables that last, Environment class
~world = SoundWorld.new(scapeWidth:256, scapeHeight:256, name:"VirtualIsland");


~zone_beach; 
~zone_square;
~zone_museum_outside;
~zone_museum_inside;

//DATABASE API 
~beach_database = "/Users/mattia/Documents/Soundscape/MetaverseUseCase/Beach/Labeled" ;
~square_database = "/Users/mattia/Documents/Soundscape/MetaverseUseCase/Square";
~museum_outside_database = "/Users/mattia/Documents/Soundscape/MetaverseUseCase/Museum/Labeled/outside" ;
~museum_inside_database = "/Users/mattia/Documents/Soundscape/MetaverseUseCase/Museum/Labeled/inside" ;

//visualisation Image API (the image dimension must be the same of the Painter window size)
~soundscapeImage = "/Users/mattia/Documents/Soundscape/MetaverseUseCase/InnovaliaMapX2-5.jpg"; 



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
b = Runner.new(a);
p = GraphParser(a, b); //parser for high level command to create manual graph, see below square toll and bellsmelody concept. 



{ //forked function

//%%%%%%%%%%%%%%%%%%%%%%%
//CREATE SOUND ZONES  %%%
//%%%%%%%%%%%%%%%%%%%%%%%


//square zone
//1 asynchronous commads
~zone_square = MtgModel.new(b,name:"Square").initAudio; //a sound zone or sound layer
c.wait;
c.test = false;
//0 asynchronous commads
~zone_square.loadConceptFoldersDatabase(~square_database, eventNameIsFileName:true); //square database as not annotation but is organised as follow:
							// a root folder (method arg) is the zone, the subfolder are the concepts,
							// each concept contains the events samples
"*_* clang said: server Buffers loaded from Square Database".postln;




//virtual_museum_outside zone
//1 asynchronous commads
~zone_museum_outside = MtgModel.new(b,name:"Museumoutside").initAudio; //a sound zone or sound layer
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
~zone_museum_inside = MtgModel.new(b,name:"Museuminside").initAudio; //a sound zone or sound layer
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
~zone_beach = MtgModel.new(b,name:"Beach").initAudio; //a sound zone or sound layer
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


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//ADD ZONES to SOUND WORLD & create LISTENER %%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
~world.initZoneArray([~zone_beach, ~zone_museum_outside, ~zone_museum_inside, ~zone_square]);

//l = MtgListener.new(117,53, ~world);//museuminside
l = MtgListener.new(82,124, ~world); //beach

//visualisation
if (visualisation == true,{

	 //if you want GUI
	z = SwingOSC.default;
	z.waitForBoot({
		c.test = true;
		c.signal;
	});
	z.boot;
	
	c.wait; //wait for boot
	c.test = false;
	
	GUI.swing ;
	0.1.wait;
	~painter = Painter.new(a, b, l, windowWidth:640, windowHeight:640, vertices:false, edges:false, eLabels:false, vDim:[100,20], fontSize:8);
	//if you use map with the same dimention of virtual unit -> windowWidth:256, windowHeight:256,
	~painter.setBackground(~soundscapeImage); // the positions of the verteces need to be * 2.5 
	~painter.vPosScale = 2.5;
});

//%%%%%%%%%%%%%%%%%%%
//MIXING & CONTROLLING ZONES GENERAL AMPS (gain) & AMPS ATTENUTENUATION BASED ON DISTANCE (m)

~zone_beach.setVirtualUnitMeterRatio(1/6); 
~zone_beach.setGain(2);

~zone_museum_outside.setVirtualUnitMeterRatio(1/6);
~zone_museum_outside.setGain(2);
//~zone_museum_outside.setCloseAmbient([80,37,28,30]); how to manage nestled closed ambience? -> don't work

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
p.parse("e+ Squaretoll 10 Squarebellsmelody 3400 Squaretoll") ; // Prob = 1/7 (in real soundscape it relates with 8 and consecutive 12 tolls, 8 a.m. and noon). 
p.parse("e+ Squaretoll 2 Squaretoll 2 Squaretoll 2 Squaretoll 2 Squaretoll 2 Squaretoll 3500 Squaretoll") ; //5 edges create a toll loop sequence, probability 5/7. an edge go to the next hour, probability = 1/7

//set position of events
a.changeVertexPosition(a.getvID("Squaretoll"),154,79); 
~zone_square.getConcept(\toll).setPosition([154,79]); //the pos information need to be stored also in concept or any event where it is different from the concept one.
a.changeVertexPosition(a.getvID("Squarebellsmelody"),154,79);
~zone_square.getConcept(\bellsmelody).setPosition([154,79]);
p.parse("a+ Squaretoll") ;	//add actant to the bell graph
//name the event as the vertex you just created
~zone_square.getConcept(\toll).actantIdDict.add(a.getvID("Squaretoll") -> (b.na -1) ); //b.na = runner number of actants, = actant id. 
~zone_square.getConcept(\toll).actants.add(a.getvID("Squaretoll"));


//fountain: 1 concept, 5 events
~zone_square.getConcept(\fountain).setGeneral(false);
p.parse("e+ Squarefountain 59 Squarefountain") ; //normal activity loop 1/5 P
p.parse("e+ Squarefountain 59 Squarefountain2 6 Squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ Squarefountain 59 Squarefountain3 3 Squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ Squarefountain 59 Squarefountain4 3 Squarefountain") ; //subject interaction event 1/5 P
p.parse("e+ Squarefountain 59 Squarefountain5 7 Squarefountain") ; //subject interaction event 1/5 P
p.parse("a+ Squarefountain") ;	//add actant
//set position of events
a.changeVertexPosition(a.getvID("Squarefountain"),179,101); 
a.changeVertexPosition(a.getvID("Squarefountain2"),179,101); 
a.changeVertexPosition(a.getvID("Squarefountain3"),179,101); 
a.changeVertexPosition(a.getvID("Squarefountain4"),179,101); 
a.changeVertexPosition(a.getvID("Squarefountain5"),179,101); 
//store concept pos & actant information (what you manually created). 
~zone_square.getConcept(\fountain).actantIdDict.add(a.getvID("Squarefountain") -> (b.na -1)); //b.na = runner number of actants, = actant id. 
~zone_square.getConcept(\fountain).actants.add(a.getvID("Squarefountain"));
~zone_square.getConcept(\fountain).setPosition([179,101]);

//manual graph STOP______

//general concept, parameter for automatic graph creation

~zone_square.getConcept(\pigeon).setPosition([165,117]);
~zone_square.getConcept(\pigeon).setAr(8);
~zone_square.getConcept(\pigeon).setProb(35);
~zone_square.getConcept(\pigeon).setMultiplePath(2);


~zone_square.getConcept(\children).setPosition([158,122]);
~zone_square.getConcept(\children).setAr(5);
~zone_square.getConcept(\children).setProb(80);
~zone_square.getConcept(\children).setMultiplePath(2);


~zone_square.getConcept(\steps).setPosition([170,211]);
~zone_square.getConcept(\steps).setAr(2);
~zone_square.getConcept(\steps).setProb(30);


~zone_square.getConcept(\footsteps).setPosition([184,187]);
~zone_square.getConcept(\footsteps).setAr(3);
~zone_square.getConcept(\footsteps).setProb(35);
~zone_square.getConcept(\footsteps).setMultiplePath(2);


~zone_square.getConcept(\cyclist).setPosition([165,117]);
~zone_square.getConcept(\cyclist).setAr(2);
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

~zone_beach.getConcept(("Clapping-Thongs").asSymbol).setPosition([90, 150]);  //because of -
~zone_beach.getConcept(("Clapping-Thongs").asSymbol).prob = 10;

//object
~zone_beach.getConcept(\FlappingFlag).setPosition([114,177]);
~zone_beach.getConcept(\FlappingFlag).setContinuous();

~zone_beach.getConcept(\FlappingFlag).clone([114,149]);
~zone_beach.getConcept(\FlappingFlag).clone([114,129]);



~zone_beach.getConcept(\Shower).setPosition([80,131]);
~zone_beach.getConcept(\Shower).prob = 10;


~zone_beach.getConcept(\BeachBar).setPosition([70,178]);
~zone_beach.getConcept(\BeachBar).prob = 50;
~zone_beach.getConcept(\BeachBar).setAr(3);
~zone_beach.getConcept(\BeachBar).setMultiplePath(2);

//people
~zone_beach.getConcept(\Bingo).setPosition([80, 122]); 
~zone_beach.getConcept(\Bingo).setAr(4);


~zone_beach.getConcept(\BeachTennis).setPosition([74, 196]);
~zone_beach.getConcept(\BeachTennis).setAr(4);


~zone_beach.getConcept(\Voices).setPosition([34, 108]);
~zone_beach.getConcept(\Voices).setAr(3);
~zone_beach.getConcept(\Voices).prob = 30;
~zone_beach.getConcept(\Voices).randomPosition = [78,110];
~zone_beach.getConcept(\Voices).setMultiplePath(4); //always after prob ... ARG ... not clean!!!!

~zone_beach.getConcept(\BeachFootball).setPosition([87, 180]);
~zone_beach.getConcept(\BeachFootball).setAr(3);

~zone_beach.getConcept(\WaterFootsteps).setPosition([30,73]);
~zone_beach.getConcept(\WaterFootsteps).randomPosition = [14,140];
~zone_beach.getConcept(\WaterFootsteps).setMultiplePath(2);
~zone_beach.getConcept(\WaterFootsteps).prob = 10;

~zone_beach.getConcept(\BeachFootbridge).setPosition([64,143]);
~zone_beach.getConcept(\BeachFootbridge).randomPosition = [47,5];
~zone_beach.getConcept(\BeachFootbridge).setAr(3);
~zone_beach.getConcept(\BeachFootbridge).prob = 25;
~zone_beach.getConcept(\BeachFootbridge).setMultiplePath(2);

//sea

//randomPosition seems better than Atm for waves, in fact wave are point source...sea as an atm, but not waves. 
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setAr(4);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setPosition([30,73]);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).randomPosition = [14,140];
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setMultiplePath(4);
~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setContinuous();

//~zone_beach.getConcept(("Breaking-Wave-Quite").asSymbol).setEventsRecordingDistance(2); //2m this means ask the system to de-amplify the concept
//IT WORKS, a perceptible difference is noted with very low value as: distance = 1, 0.5, 0.05, m

//Design associating different waves nuances for temporal evolution (time states)
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setPosition([30,73]);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).randomPosition = [14,140];
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setAr(4);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setMultiplePath(3);
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setContinuous();
~zone_beach.getConcept(("Breaking-Wave-Calm").asSymbol).setActive(false);


//~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setPosition([56,225]);
//~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).randomPosition = [36,22];
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setPosition([30,73]);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).randomPosition = [14,140];
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setAr(4);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setMultiplePath(3);
~zone_beach.getConcept(("Waves-Rippling-Rocks").asSymbol).setContinuous();


//street
~zone_beach.getConcept(\Palm).setPosition([64,102]);
~zone_beach.getConcept(\Palm).prob = 10;
~zone_beach.getConcept(\Palm_I).setPosition([75,104]);
~zone_beach.getConcept(\Palm_I).prob = 10;
~zone_beach.getConcept(\Palm_II).setPosition([86,104]);
~zone_beach.getConcept(\Palm_II).prob = 10;
~zone_beach.getConcept(\Palm_III).setPosition([99,104]);
~zone_beach.getConcept(\Palm_III).prob = 10;

~zone_beach.getConcept(\Beach_steps).setPosition([114,179]);
~zone_beach.getConcept(\Beach_steps).prob = 10;

~zone_beach.getConcept(\Beach_steps_I).setPosition([114,134]); 
~zone_beach.getConcept(\Beach_steps_I).prob = 10;

~zone_beach.getConcept(\Beach_steps_II).setPosition([93,113]); 
~zone_beach.getConcept(\Beach_steps_II).prob = 10;

~zone_beach.getConcept(("Passer-by").asSymbol).setPosition([112,84]);
~zone_beach.getConcept(("Passer-by").asSymbol).setProb(60);
~zone_beach.getConcept(("Passer-by").asSymbol).setAr(15);
~zone_beach.getConcept(("Passer-by").asSymbol).randomPosition = [19,100];
~zone_beach.getConcept(("Passer-by").asSymbol).setMultiplePath(4);


~zone_beach.getConcept(("Runner-by").asSymbol).setPosition([115,155]); 
~zone_beach.getConcept(("Runner-by").asSymbol).setAr(2);
~zone_beach.getConcept(("Runner-by").asSymbol).prob = 5;

~zone_beach.getConcept(("Roller-by").asSymbol).setPosition([114,115]);
~zone_beach.getConcept(("Roller-by").asSymbol).setAr(2);
~zone_beach.getConcept(("Roller-by").asSymbol).prob = 4;

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


~zone_beach.getConcept(\BeachFootball).setListenedArea(100, a); // set Listened area Must be call after the graph are created. TODO Review it!

"%%%%%%%%%%%%%%%%".postln;
"AUTOMATIC GRAPH GENERATION COMPLETE".postln;
"%%%%%%%%%%%%%%%%".postln;


//visualisation 
if (visualisation == true, {
	4.wait; //for graph generation //Warning: if you have a slow computer this wait could be not enough
	~painter.vertices = true;
	~painter.drawVertices; //draw all the vertex
	4.wait; //GeoGraphy GUI is a bit slow...
});


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//START SOUNDSCAPE GENERATION %%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
1.wait; //Just to be sure
~world.startAllDephasingClones(insideRoutine:true);
//~world.startZone("Museuminside");
"generation Active".postln;


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//Soundscape Controller

~controller = SoundscapeController.new(~world, gui:true, runner:b); //need a runner instance
//in order to modify the soundscape performance, true ask for gui


}.fork;

)
MtgListenerGUI.new(); //Position GUI controller, Local Machine, listener1



~controller.loadPreset("day");
~controller.loadPreset("night");
~controller.loadPreset("people");
~controller.loadPreset("morePeople");


//near beach tennis
l.setPosition(74,196); 
//near bingo
l.setPosition(82, 124);
//near bar 
l.setPosition(75,180);
//in street, near people, runner-by, roller-by
l.setPosition(70, 190);
//near beach footbridge
l.setPosition(64,143);

l.setPosition(118,53); //museum inside
l.setPosition(100,54); //museum outside
l.setPosition(117,70); //museum outside near beach
l.setPosition(150, 150); //square


l.setPosition(114,177) //flappingFlag
l.setPosition(114,149) //flappingFlag1
l.setPosition(114,129) //flappingFlag2


//some interactions

~zone_beach.getConcept(\FlappingFlag).getName;
~zone_beach.getConcept(\FlappingFlag).getId;
~zone_beach.getConcept(\FlappingFlag_1).getName;
~zone_beach.getConcept(\FlappingFlag_1).getId;
~zone_beach.getConcept(\FlappingFlag_2).getName;
~zone_beach.getConcept(\FlappingFlag_2).getId;

~zone_beach.getConcept(\BeachTennis).getName;
~zone_beach.getConcept(\BeachTennis).getId



~zone_beach.getConcept(\FlappingFlag).getParameters
~zone_beach.getConcept(\FlappingFlag_1).getParameters
~zone_beach.getConcept(\FlappingFlag_2).getParameters

~zone_beach.getConcept(\FlappingFlag_1).events.do({arg event; event.pos.postln;})
~zone_beach.getConcept(\FlappingFlag_1).events.do({arg event; event.pos.postln;})


(
l.listenerSynths.do({arg synthArray; synthArray.postln;});


//query the synthesis architecture status
s.queryAllNodes;
)

~zone_beach.moveConcept(\FlappingFlag, 100,0);
~zone_beach.moveConcept(\FlappingFlag_1, 200,1);
~zone_beach.moveConcept("Breaking-Wave-Quite", 100,100);
~zone_museum_inside.moveConcept("museum_inside_atm", 1,1);


~zone_beach.trasformConceptRandomPosition("Breaking-Wave-Quite", 200,200);

{
100.do({arg i;
	i.postln;
~zone_beach.moveConcept("Breaking-Wave-Quite", (10 + i*2),(10 + i*2));
1.wait;
});
}.fork;



~controller.savePreset("morePeople");
~controller.savePreset("day");


//maybe here is it better a switching button
~controller.setActivationPreset("Day", ~zone_beach, [["Breaking-Wave-Quite", \FlappingFlag, \FlappingFlag_1, \FlappingFlag_2],["Breaking-Wave-Calm"]]); //onConceptArray =  [[Concept to be activated],[Concept to be deactivated]]

~controller.setActivationPreset("Night", ~zone_beach, [["Waves-Rippling-Rocks"],["Breaking-Wave-Calm", "Breaking-Wave-Quite", \FlappingFlag, \FlappingFlag_1, \FlappingFlag_2]]);


m = NetAddr("10.55.0.97", 57130);
~controller.setNetworkController(m);
~controller.setNetworkController("127.0.0.1");

