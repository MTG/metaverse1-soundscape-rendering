//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.

SoundConcept {

//the name variable of the SoundConcept object store the global id (unique in this SoundWorld) in the form zone name + concept name. 
//use the getName (return just the pure name, i.d. name without the zone name) and and getId method
	var <>name, <>pos, <>prob, <>cont, <>general, <>ar, <>events, <>eventNames, <>graph, <>active, <>actants;
	var <> randomPosition, <>multiplePath; 
	var <> actantIdDict;
	var <> multipleActants;
	var <> tmpNumberActants; //cache var for actant modification request by soundscape controller. 
	var <> tmpProb; //this is the current prob during the performance interaction, while prob represents the initial value before the graph generation. 
	var <> runner;
	var <>conceptGain, <>gainRange;
	var <>listenedArea;
	var <>zone, <>zoneObject;
	
	//multiplePath store the information about the number of actant for the design at the basic stage:
	//actants store the info of the current actants due to interaction control (more/less request of generation probability) 
	//multiplePath should be the reset value of the actants size!!!!
	
	//TODO: how to store events generative structure, graph or matrix, or structure: geography has a graph grammar when save and write a graph object to file, see Runner
	var <>clones, <>cloneNumber, <>father;
	
	*new { arg name, pos, prob, cont, general, parentZone;
		^super.new.initSoundConcept(name, pos, prob, cont, general, parentZone);
	} 


	initSoundConcept{ arg aName, aPos, aProb, isContOrImpulsive, isGenOrSpecific, aParentZone ; 
		
		// pos is an Array: [x,y] for point source, [x,y,W,H] for not point source where x,y are the coordinates of the center of the rect. use setnonPointSource
		pos = aPos;
		prob = aProb; // float
		cont = isContOrImpulsive; //boolean
		
		zoneObject = aParentZone;
		zone = aParentZone.name;
		
		name = aName.asSymbol; //Symbol
		
		conceptGain = 1;
		gainRange = [0,32];
		
		//general is a boolean, if true the graph structure is automatically computed,
		//if specific a graph strucuture is needed, could be created manually 
		if (isGenOrSpecific == nil,
		{general = true;},	
		{general = isGenOrSpecific;}
		);

		// ar = arrhythmic generation parameter [0,20] int
		//In continuous sounds, ar represents the number of In/out edge per vertex, but the edge dur doesn't depend on ar.
		//In impulsive sounds
		//ar = 1 means regular generation, ar = nevents, means more edges per vertex and several temporal pattern
		ar = 1; 
		
		events = List.new; //we cannot use Identity Dictionary because the order is meaningful for graph generation
		eventNames = List.new; //list of string to control if a new name is already assigned
		
		graph = ""; //var to store information about the graph structure of all this.events

					
		active = true; //if false the amp of all its events is set to 0 in CartoonModel.play
		
		actants = List.new; //the current value of actants due to performance interaction, while mulpitlePath represents the initial and ideal value of actant for a concept. 
		
		clones = List.new;
		
		randomPosition = nil; //random position generation of point source in a reference area. -> a parameter for the generation model.
						// arg is area, an Array [Width,Height]
						
		
		actantIdDict = IdentityDictionary.new; //vertexiD(eventually cloned) -> actantId
		multipleActants = IdentityDictionary.new; //store the information about vertex that has more than one actant starting from it (vertex -> n actants)
		
		multiplePath = 1; //is the number of actant the designer wants. If not specified is just one. 
		
		tmpNumberActants = multiplePath; //initialisation, the number of actant before using the SoundscapeController. 
	}

	
	add { arg nameE, classE, posE, dyE, rollE, probE, sampleDurE, samplePathE, sampleStartFrameE, preserveEventName, normalisationE;
		var e, id, label ;
		
		
		if (nameE == nil,
			{label = (this.events.size + 1).asString;}, //is the number of event until now
			{label = nameE;}			
		);
				
		if (preserveEventName == true, {
		
			id = label; //for manual graph creation, when the designer must provide the information of zone and concept in the name
			}, {
			id = this.name++label; //use id as zoneconceptevent; example: zone beach, concept wave, event small, id = beachwavesmall
		});
		
		
		this.eventNames.add(label);
		
		if (posE == nil,
			{posE = this.pos}
		);	
		
				
		if (normalisationE != nil, {
		
			if ((normalisationE.asFloat >= gainRange[0])&&(normalisationE.asFloat <=gainRange[1]),
			
				{},
				{	normalisationE = 1;
					("error: incorrect event normalisation value per concept:"+this.name+"and event"+id).postln;
				}
				);
			});

		
		id = id.asSymbol;
		e = SoundEvent.new(id, classE, posE, dyE, rollE, probE, sampleDurE, samplePathE, sampleStartFrameE, normalisationE);
		this.events.add(e);
		
		//("soundConcept.add(event)"++e).postln;
		//("id, classE, posE, dyE, rollE, probE"+id+classE+posE+dyE+rollE+probE).postln;
		
		
				
		^id;
		
		
	}
	
	setGeneral { arg boo;
		general = boo;
		if(boo == false, {
			this.prob = -1; //we don't know the probability for this type of concept, -1 means no use prob parameter. 
			});
	}
	
	setActive { arg boo;
		active = boo;
	}
	
	
	setAr { arg anAr;
		
		if (anAr < 1,
			{anAr = 1; ("ar must be ar > 1 in concept"+this.name).postln;},
			{});
		
		//range, in some case, for concept having a lot of events, a bigger value could be useful, but this is a general standard range	
		if (anAr > 20,
			{anAr = 20; ("ar must be ar < 20 in concept"+this.name).postln;},
			{});	
		ar = anAr;	    
		
		
/* arg value; var wrong = false;
	
		if (value < 1, {wrong = true; "multiplePath cannot be < 1".postln;});
		if (value >= this.events.size, {wrong = true; "multiplePath cannot be >= the number of concept events".postln;});
		
		if (wrong == false, {
			this.multiplePath = value.asInteger; 
			
		});*/
		
	}
	
	
	setPosition{ arg posArray;
		this.pos = posArray;		
		this.events.do({arg event;
			if (event.pos == nil, //TODO, think if store event position in term of differential from (relative to) the concept position 
			{event.pos = this.pos});
		});
	}
	
	
	getPosition{ 
		^this.pos;		
	}
	
	setnonPointSource { arg rect; //the same of position but [x,y,W,H] x,y upper left corner
		
		var top, left, w, h;
		var x, y ;//the center
		
		left = rect[0]; //x
		top = rect[1]; //y
		w = rect[2];
		h = rect[3];
		//in MtgListener position model upper left corner = (0,0)
		x = left + (w/2);
		y = top + (h/2);
		
		this.setPosition([x,y,w,h]);
		
		^[x,y]
			
	}
	
	
	//The MtgModel controls that values x,y are not corrupted.
	move { arg x, y, w, h; //this method consider all the concept event with a unique position equal to the concept position. 
		
		var graph, xx, yy, return, width, height, atm; 
				
		xx = x;
		yy = y;
		
		case 
		 {this.pos.size == 2}
		 	 {this.setPosition([x,y])} //does not affect the event
		 {this.pos.size == 4}
		 	 {	
			 	if (w == nil, {width = this.pos[2]},{width = w});
			 	if (h == nil, {height = this.pos[3]},{height = h});
			 	return = this.setnonPointSource([x,y,width,height]); //actual w and h
			 	xx = return[0]; //coordiates of the center of the rect
				yy = return[1];
				atm = [xx,yy,width,height]
			 }; 
		
		
		graph = zoneObject.runner.graph;
		
		this.events.do({arg event; 
			
			graph.changeVertexPosition(event.viD,pos[0],pos[1]);
			
			if (atm != nil, {zoneObject.setAtm(event.viD,atm,graph)});
			
			event.pos[0] = xx;
			event.pos[1] = yy;
			
			});
		
	}
	
	//note that this area is used to trigger an event in a random position, thus it just affect the MtgModel.play method, and it cannot be modified in already active events. 
	trasformRandomPosition { arg w,h;
		
		var graph, width, height; 
		
		this.randomPosition = [w,h];
		
		graph = zoneObject.runner.graph;
	
		this.events.do({arg event;
			zoneObject.setRandomPosition(event.viD,randomPosition,graph)
		});
		
	
	}
	
	
	setListenedArea { arg meter, aGraph; //aGraph for back forward compatibility
	
		var graph;
		listenedArea = meter; 
		
		if ((listenedArea > 0)&&(listenedArea < 500), {
			
			if (aGraph == nil,{
				graph = zoneObject.runner.graph;},{
				graph = aGraph;	
					});
			
			this.events.do({arg event;
				event.listenedArea = meter;
				graph.setvopts(event.viD, 0, meter); //setvopts args are: vID, annotationOrder, val
	
			});	
		}, {("error, listened area must be > 0 and < 500 meter in concept"+this.name).postln;});
	
	}
	
	
	getId {
		^name	
	}
	
	getName {
		var nameWithoutZone;
		var i = 0;
		nameWithoutZone = this.name.asString[(zone).size..];
		^nameWithoutZone
	}

	getGain {
		
	^this.conceptGain;
	
	}
	
	getProb {^this.prob} //the initial prob, before soundscapeController Interaction
	
	
	setProb { arg value, asFreq;
	
		var prob;
		var correctFreq, correctTimesHour;
		var maxGenPerHour = 3600; 
		var g, h, startExp = 0.8;
		
		g = ControlSpec(1, 100, \lin, 1, 1, "Hz");
		h = ControlSpec(101, maxGenPerHour, \exp, 1, 1, "Hz");
		
		if (value == 0,{prob = 0},{
		
			if (asFreq == true, {	//frequency value
					
				if ((value > 0)&&(value <= 1), {
					
						if (value <= startExp, {prob = g.map(value*(1/startExp))}, {prob = h.map(((value - startExp)*(1 / (1 - startExp))))});
					
					}, {correctFreq = false});
				
				
				},{ //times per hour value
				
				if ((value > 0)&&(value <= maxGenPerHour), {prob = value}, {correctTimesHour = false;})
				
				});
			});
		
		
		if (prob != nil, {this.prob = prob}, {
			
				case 
					{correctFreq == false}
						{("error, probabilit as frequence must be > 0 and < 1 in concept"+this.name).postln;}
					{correctTimesHour == false}
						{("error, probabilit as Times per Hour must be > 0 and <"+maxGenPerHour+"in concept"+this.name).postln;}
			
			});
			
			
/*mapping, Frequency [0,1] to [0,3600] times per hour. Frequency = max = 1 means 1 events generation per second for this concept. 
0.001	0.01		0.05		0.1		0.15		0.2		0.25
1		2		7		13		20		26		32	

0.3		0.35		0.4		0.45		0.5		0.55		0.6
38		44		51		57		63		69		75	

0.65		0.7		0.75		0.8		0.85		0.9		0.95
81		88		94		100		247		603		1473		

1
3600	*/
	
	}

	
	
	//each actant more than the initial n of actant double the prob,
	//each actant less than the initial n of actant divide the prob * 2
	//no actant -> prob = 0;
	//actants = multiplePath, the initial state, -> prob is the initial prob
	setTmpProb { 
		var t;
		t = (actants.size - multiplePath);
		if (t == 0,{tmpProb = prob;},{	
			if (t < 0, { 
				if (actants.size == 0, {tmpProb = 0;}, { 
					tmpProb = prob/(1 + abs(t));}
				);
			},{
			if (t > 0, {
			tmpProb = prob*(1+t);});
			}
			);
		});
		
		this.changed(this, [\tmpProb]);
		
		^tmpProb
	}
	
		
	getTmpProb { //to save the currect performance status by SoundscapeController
		var t;
		
		if (this.tmpProb == nil, {
				
			t = (actants.size - multiplePath);
			if (t == 0,{tmpProb = prob;},{	
				if (t < 0, { 
					if (actants.size == 0, {tmpProb = 0;}, { 
						tmpProb = prob/(1 + abs(t));}
					);
				},{
				if (t > 0, {
				tmpProb = prob*(1+t);});
				}
				);
			});
		});
		^this.tmpProb;
	}

	
	setContinuous {
	
		this.cont = true;
	
	}
	
	isContinuous {
		var is;
		if (this.cont == true, {is = true},{is = false});
		^is;
		}
	
	//TODO add x,y,W,H value control?
	setRandomPosition { arg aDimension;
	
	
		if (aDimension.size == 2, {this.randomPosition =  aDimension;},{
				if ( aDimension.size == 4, {
					this.randomPosition = [aDimension[2], aDimension[3]]; //W, H
					this.setPosition([aDimension[0], aDimension[1]]); //x,y upper left corner
					},{
					"SoundConcept.setRandomPosition incorrect format".postln;
					}
				);
		
			});
		
	
	}
	
	
	//The edge dur (MtgModel.generateGraph) changes if there are multiple actants: edge_dur = hour/(prob*multiplePath)
	
	setMultiplePath { arg value; var wrong = false;
	
		if (value < 1, {wrong = true; "multiplePath cannot be < 1".postln;});
		/*if (value >= this.events.size, {wrong = true; "multiplePath cannot be >= the number of concept events".postln;});*/
		
		if ((value >= this.events.size)&&(this.events.size > 0), {("WARNING: multiplePath 'SHOULD' not be > of the number of concept"+this.name+" events. nevents = "+this.events.size).postln;});
		
		if (wrong == false, {
			this.multiplePath = value.asInteger; 
			
		});
		
	}
	
	setGain { arg g;
		
		
		
		if ((g.asFloat >= gainRange[0])&&(g.asFloat <=gainRange[1]),
		
			{
				this.conceptGain = g.asFloat;

				if (zoneObject.sampleplaying.size > 0, {
					zoneObject.sampleplaying.do({ arg synthInfo;
									if(synthInfo[8] == this.name.asSymbol, {synthInfo[0].set(\conceptGain, this.conceptGain);})
								});
					});
				},
			
			{("error: incorrect gain value per concept:"+this.name+"").postln;}
			);
		
		
	}
	
	
	changeEventsPosition{ arg posArray; //useful in clone to change the position of events of a new clone
		
		this.pos = posArray;
		this.events.do({arg event;
			event.pos = this.pos;
		});
	}
	
	setEventsRecordingDistance { arg aRecDistance;
	
		if ((aRecDistance.asFloat > 0)&&(aRecDistance.asFloat < 50),
		
			{this.events.do({arg event;
			event.dy = aRecDistance.asFloat;
			});
			},
			
			{("error: incorrect recording distance value per concept:"+this.name+"").postln;}
			);
	
	
	}
	
	
	addActant { arg aiD ;

		actants.add(aiD);
	
	}
	
	
	//warning: tested just with generic concept.  
	modifyProbByEdgeDurs { arg newProb, aGraph;
			var tmpedgedur;
			var j = 0;
			var step = 2;
			var tmp;
			var x = 0;
			var newDursArray = List.new, actual = 0, previous = 0;
			var graph;
								
			tmpedgedur = (3600.0/newProb);
				
			//find edgedurs values
			this.ar.do({ arg i; var tmp; //from 0 to events.size-1, calcule j
				tmp = step**(i); //step*(i);
				j = j + tmp;
			});
			
			x = (tmpedgedur*ar)/j;
		
			this.ar.do({ arg i;	//from 1 to ar. The edges number depend on ar. If ar = n, n edge connection in-out per vertex and resoults.size = n
				if (i == 0, 
				{
				actual = x;
				newDursArray.add(actual);},
				{
				previous = actual;
				actual = previous * step;
				newDursArray.add(actual);}
				);
				
			});
			
			//newDursArray.do.postln;
			
			graph = zoneObject.runner.graph;
			this.events.do({arg event; 
				var viD, edges, i;
				viD = event.viD;
				edges = graph.graphDict[viD][5..];
				i = 0;
				edges.do({arg edge; edge[1] = newDursArray[i]; i = i +1;});
				//"changed:-------------".postln;
				//graph.graphDict[viD].postln;
			});
			
			this.setProb(newProb);
		
	}
	
	
	//TODO	
/*	synchronizeProbs {
		
		this.modifProbability(multiplePath,synch:true); //reinit actans
		this.tmpProb = this.prob; //synch probs
		
		
	}*/
	
	
	//wait both in increasing and decreasing (in teory decreasing don't need), because in addition to increasing prob also triggers samples. 
	//Designers like this!
	//for immediate prob interaction use the method modifyProbByEdgeDurs
	modifProbability {arg numberOfactants, aRunner,  synch; //int the number of actant that the controller is asking
		var duration;
		
		if (aRunner != nil, {runner = aRunner});
		//You need a variable, tmpNumberActants, and looppy control if this variable is the same of actants.size (+- 1) and if not add one more actant.

		if (numberOfactants != nil, {this.tmpNumberActants = numberOfactants});
		
		//("actants.size"+actants.size+"tmpNumberActants"+tmpNumberActants+"concept"+this.name).postln;
		
		{
		
		if (tmpNumberActants > this.actants.size,{ //if the number of requested actants isn't change, do nothing
				
				//flag = 'increase';
				duration = this.increaseActant;
				//wait that the sample of the vertex activated by the new actant finished to play, that control if the request of actants is still pending
				
						
						//wait
						duration.wait;
						this.setTmpProb;
						
						this.modifProbability;
			
				}, {if (tmpNumberActants < this.actants.size,{
						
						duration = this.decreaseActant;
						
							
						//wait
						duration.wait;
						this.setTmpProb;
						

						this.modifProbability;

						//flag = 'decrease';
				
						}
					);
				}
			);	
		}.fork;
		//THIS IS A LOOP TO MYSELF, when tmpNumberActants == this.actants.size the LOOP ends
	 
	}
	
	
	//increase one actant, define its name (actantId) both on concept and runner, define on which vertex (actantVertexId) it start.
	// return duration of the vertex that it's activated when the actant is setUped

	increaseActant {
		var actantId , postfix, actantVertexId;
		var limit;
		var duration;
		

		//actantVertexId definition START
		
		if (this.actants.size == 0, 
			{actantVertexId = this.events[0].viD},
			{actantVertexId = (this.actants[(this.actants.size - 1)] + 1 );} //or actants[-1] will crash
		);
		
		//the limit of actant per vertex is set at 2 (in the ControlSpec increment of SoundscapeController mapping method)
		limit = ( this.events[0].viD + this.events.size - 1);
		if (actantVertexId > limit, {actantVertexId = this.actants[0];});

		//actantVertexId definitin STOP
		
		//vertex duration
		this.events.do({arg e;
				if (e.viD == actantVertexId, {duration = e.sampleDur});
		});
		
		//manage runner and concept actantIdDict
		
		if (actantIdDict[actantVertexId] == nil, {
		
		
			actantId = runner.na; 
			runner.addAndSetup(actantVertexId); //the method must return a vertexId 
			runner.start(actantId);
			actantIdDict.add(actantVertexId -> actantId);
			this.addActant(actantVertexId);
			//"I'm increasing actants".postln;
			},{
			
			//("Vertex"+actantVertexId+"has multiple actants").postln;
			
			if (multipleActants[actantVertexId] == nil, { 
				
				multipleActants.add((actantVertexId) -> 1); //the first multiple actant on a vertex

				//common part...
				actantId = runner.na; 
				runner.addAndSetup(actantVertexId); 
				runner.start(actantId);
				this.addActant(actantVertexId);
				//"I'm increasing actants".postln;
				//common part
				
				
				actantIdDict.add((""++actantVertexId++"_I").asSymbol -> actantId); //id for the first multiple actant on a vertex

				},{ //another multiple actant on a vertex
				
				multipleActants[actantVertexId] = (multipleActants[actantVertexId] + 1); //the first multiple actant on a vertex
				
				//common part...
				actantId = runner.na; 
				runner.addAndSetup(actantVertexId); 
				runner.start(actantId);
				this.addActant(actantVertexId);
				//"I'm increasing actants".postln;
				//common part
				
				postfix = "_I";
				(multipleActants[actantVertexId] - 1).do({ postfix = (postfix++"I"); }); 
				
				actantIdDict.add((""++actantVertexId++postfix).asSymbol -> actantId); //id for the first multiple actant on a vertex
				
				}
			);
			
			};
			
			
			
		);	
		^ duration;
	
	}
	

	decreaseActant {
		var actantId, lastactant, postfix, actantVertexId;
		var index;
		var duration;


		if (this.actants.size == 0, 
			{actantVertexId = this.events[0].viD},
			{ 
			index = (this.actants.size - 1);
			if (index < 0,{index = 0;},{});
			actantVertexId = this.actants[index];
			
			} //or actants[-1] will crash
		);

		if (multipleActants[actantVertexId] == nil, { //the vertex has just one actant. 
		
			actantId = actantIdDict[actantVertexId];
			actantIdDict.removeAt(actantVertexId);
			//(""+actantId+"actantid").postln;
			
			},{ //the vertex has multiple actants
			
			lastactant = multipleActants[actantVertexId]; //last added actant on the vertex
			//("lastactant"+lastactant).postln;
			multipleActants[actantVertexId] = (multipleActants[actantVertexId] - 1);
			
			if (multipleActants[actantVertexId] == 0, {multipleActants[actantVertexId] = nil;});
			//("multipleActants[actantVertexId]"+multipleActants[actantVertexId]).postln;
			
			postfix = "_";
			lastactant.do({ postfix = (postfix++"I")});
			
			actantId = actantIdDict.[(""++actantVertexId++postfix).asSymbol];
			//actantId = 
			actantIdDict.removeAt((""++actantVertexId++postfix).asSymbol);
			
			//("actantVertexId++postfix"+actantVertexId++postfix+"actantId"+actantId).postln;
			
			};
			
		);
		
		//vertex duration
		this.events.do({arg e;
				if (e.viD == actantVertexId, {duration = e.sampleDur});
		});
		
		//"I'm DECRESING actants".postln;
		runner.removeActant(actantId); //add control if this methos failed???
		//("runner.removeActant(actantId)"+actantId).postln;
		actantVertexId = this.removeActant();
		
		^duration;
	}


	removeActant { var viD;
		
		if (actants.size > 0,
			 {viD = actants.pop; //("actant removed on vertex"+viD).postln;
			 },
			 {" actant.size = 0".postln}
		);
		^viD;
	}


	//this method must be used before the zone.generateGraph method, because don't manage the graph.
	
	clone { arg pos;
		var cloneName, clone, zone, probBeforeProbGeneration; 
		
		zone = this.zoneObject;
		cloneName = (""++this.name++"_"++((this.clones.size) +1));
		
		probBeforeProbGeneration = this.prob;
		if (probBeforeProbGeneration == 0, {probBeforeProbGeneration = nil});
		
		clone = SoundConcept.new(cloneName, pos, probBeforeProbGeneration, this.cont, this.general, zone); 
		clone.ar = this.ar;
		clone.randomPosition = this.randomPosition;
		clone.graph = this.graph;			
		clone.active = this.active;
		clone.multiplePath = this.multiplePath; 
		clone.father = this;
		
		this.cloneEvent(clone);
		
		
		//add the clone to the list of this concept clones
		this.clones.add(cloneName);
		
		clone.cloneNumber = this.clones.size;
		
		zone.conceptDict.add(cloneName.asSymbol -> clone); //add the clone in the zone conceptDict
		
		^[clone.getName, clone]
	}
	
	
	cloneEvent {arg clone;
		
		//the events are the same samples, just the position changes
		this.events.do({arg event; 
			var e;
			
			e = SoundEvent.new(event.name, event.gaverClass, event.pos, event.dy, event.rolloff, event.prob, event.sampleDur, event.samplePath, event.sampleStartFrame);
			clone.events.add(e);
			
			});
		
		
		//change the position of all its events;
		clone.changeEventsPosition(pos);
		clone.eventNames = this.eventNames;
		clone.prob = this.prob; //this is useful in mtgModel.generateGraph
	
	}
	
	
	getState {
		this.events.do({arg event; 
			event.name.postln};);
		("Concept "+this.name+" has "+this.events.size+" instances").postln;
		"%%%%%%%%%%%%%%".postln;
	
	}
	
	
	getStateMemory {
				var conceptDur = 0, conceptMB;
			
		this.events.do({arg event; var dur;
			dur = event.sampleDur;
			event.name.post;
			(" dur:"+dur+"sec").postln;
			conceptDur = conceptDur + dur;
			});
		
		conceptMB = (conceptDur * 44100 * 4 )/ (1204*1204); //TODO?: In this case is ok, but SampleRate could be specific to each event! take it from buffer or analysis recordings...
			
		("Concept "+this.name+" has "+this.events.size+" instances ("++conceptDur+"sec) ="+conceptMB+"MB").postln;
		"%%%%%%%%%%%%%%".postln;
		
		^[conceptMB,conceptDur];
		
	}
	
	getParameters {
		
	^["name", name, "pos", pos, "prob", prob, "cont", cont, "general", general, "ar", ar, "active", active, "actants", actants,"randomPosition", randomPosition, "multiplePath", multiplePath, "conceptGain", conceptGain, "listenedArea", listenedArea]
	}
	
	
	
}


