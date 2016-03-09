//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.


MtgModel {
	var <>server, <> runner, <>ns, <>sampleplaying;

	var <>bufDict, <>samplesPath, <>conceptDict, <>timeAnalysisRec, <> bufFree, <>segmentDict;
	var <>offsetVertexStandardListenedArea;
	var <>gain, <>controllerVolume, <>dy, <>conceptGain;
	var <>m;	// ratio VirtualUnit/meter. 
	var aDef;
	var <>name;
	var <>threshold = 0.000001;
	var c ;
	var <>geometry, <>closeAmbient, <>soloAmp;
	var <>world, <>soundscapeGroup; // a SoundWorld instance, soundscapeGroup is a Synth Node Group
	var <>activeSO; //active sound objects, an entry is in the form [Synth, bufnum, bus], soGroups collects all the execution groups (so Synth, listener_1 Synth,...,listener_n Synths) note that the listener Synths are created in MtgListener

	
	*new { arg runner, aServer, name ; 
		^super.new.initMtgModel(runner, aServer, name) ; 
	}
	
	
	initMtgModel { arg aRunner, theServer, aName;
		var serveroptions;
		
		"MtgModelInit".postln;
		runner = aRunner;
		runner.addDependant(this);
		
		
		//the user could ask to work with internal server
		if (theServer == nil, {
			server = Server.local ;
			//Server.default = Server.local; //crash...
			},{server = theServer; Server.default = theServer}
		);
		
		serveroptions = server.options;
		serveroptions.numAudioBusChannels = 3000;
		
		server.boot;
		
		server.doWhenBooted({
			this.sendDef ;
		});
		
			
		sampleplaying = IdentityDictionary.new; //to trace active synth and sample property
		
		conceptDict = IdentityDictionary.new; //Dict of the Soundscape Zone concept
		
		timeAnalysisRec = 0.0 ;		//Counter for the global audio material, the computing of the general probability is based on this 
		
		ns = 0;
		name = aName;
	}
	
	//more here is : //[\actant, vID,[x, y, vDur, vLabel, [offsetVertexListenedArea]],
	// eID, [end, eDur, eID, eOpts], aID, weight, offsetWeight, count]
	update { arg theChanged, theChanger, more;
		if (more[0] == \actant,

			 { 	
				this.play( more[1..] );
				}	
		) ; 
	}
	
	
			
	initAudio { arg aSamplesPath, nameList, ext = "wav" ;			
			activeSO = List.new;
			
			offsetVertexStandardListenedArea = 60; //in meter
			gain = 1;
			controllerVolume = 1; // this variable belong to the controller
			m = 1;
			// in Innovalia 1 virtualUnit = 3 m /
			// In balon use-case 1200 virtual unit = 105 meters
			
			samplesPath = aSamplesPath ;
			segmentDict = IdentityDictionary.new ; //allow to have a list of label/path for sample that aren't SoundConcept instance
										// In order to use the SynthDef with DiskIn -> automatic Buffer Allocator Synth for active samples
										
			bufDict = IdentityDictionary.new ;
			bufFree = List.new(0) ;
			c = Condition.new;
			Routine.run {
				server.doWhenBooted({
						soundscapeGroup = Group.new(server);
						
						//The first implementation to load up sample in the system, need a path and a list of name/uid (unique id) for the event
						//deprecated
/*						nameList.do({ arg name ;
							var path = (samplesPath++name++"."++ext);
							this.addSegment(name, path, this.getSampleDur(path));
								});*/
							server.sync(c);
							c.test = true;
							c.signal; 
							
						}) ;
			
				//all the init actions must come before the condition wait thus that complete method wait until the async commands are finished before return
				c.wait;
				};
			
			}
			
			//TODO create a new Synth def for \atm with Balance2 rather tha pan. When load a samples collection do in with f = SoundFile.new(path) ->
			// if f.numChannels == 2, allocated seperatelty left and right channel. Then in method play call \atm SynthDef and in mtgListener.pan calcolate the balance level
			// you add the active sinth to the synthDict, in teory in modif play you haven't to change nothing if you name the balance parameter of the \atm synthDef pan 
	
	
	//this send def requires CPU but very Low RAM 		
	sendDef {						
					
			aDef = SynthDef(\sequencing, {arg bufnum, bus, fadetime, sustime, dy  = 1, gain = 1, conceptGain = 1, controllerVolume = 1 ;
				Out.ar(bus, 
					(
					EnvGen.kr(Env.new([0,1,1,0], [fadetime, sustime, fadetime]), doneAction:14) //free the enclosing group and all nodes within it (including this synth)
										//see this.play where fadetime and sustime are compute
										//doneaction will free the synth at the end of EnvGen.
										// When the synth is free a NodeWatcher and
										// a SimpleController in this.play will free the associated buffer 
					*
					controllerVolume * (conceptGain * ( gain * (dy * DiskIn.ar(1, bufnum, 0)))))
					);
				}
			);
			aDef.send(server) ;
	}
	
	
	setGain { arg value;
		
		//more than 32 start to be dangerous for ears. 
		
		if ((value >= 0)&&(value <=32),{
			gain = value;},{("error: incorrect setGain value in zone:"+this.name+"").postln;});
	}
	
	getGain {^this.gain}
	
	
	setControllerVolume { arg value;
		
		controllerVolume = value;
		this.soundscapeGroup.set(\controllerVolume, controllerVolume);
	
	}
	
	getControllerVolume{^controllerVolume}

	readGain {
		^gain;
	}
	
	
	setGeometry{ arg anArray; ////[x,y,W,H] x,y the upper left corner
		
			if (anArray.class == Array&&(anArray.size == 4), {
					this.geometry = anArray;
					if (this.closeAmbient == "waiting", { this.closeAmbient = this.geometry; //"stop wait;".postln;
						 });
					},
				{ "MtgModel.setGeometry doesn't receive a correct geometry as arg".postln; 
			});
		
		}
	
	
	getGeometry{
			var correct;
			if (this.geometry == nil, {
					correct = false;});
					
			if (correct == false, 
				{ ("warning: using getGeometry when zone:"+this.name+"geometry = nil").postln; 
			});
			^this.geometry
		}
	
	
	//x,y are the coordinates of the upper left corner
	setCloseAmbient { arg anArea, theSoloAmp; //[x,y,W,H]
		closeAmbient = anArea;
		
		if (closeAmbient == nil, {
			if (this.geometry == nil, {closeAmbient = "waiting";}, {this.closeAmbient = geometry});
			});		
		

		soloAmp = 1;		
/*		closed ambient variable solo Amp not implemented, you can interact with zone gain. 
		if (theSoloAmp == nil, {soloAmp = 1}, {
			if ( theSoloAmp>0and:theSoloAmp<1, {soloAmp = theSoloAmp}, {
				"the solo amp value not correct of zone:"++name.postln;
				 soloAmp = 0;
			});
		});*/
	}
	
	
	setVirtualUnitMeterRatio { arg ratio;
		
		if (ratio > 0, 
			{m = ratio;},{("error: using an uncorrect value with setVirtualUnitMeterRatio in zone:"+this.name+"").postln
		});
	}
	
	
	
	createConcept { arg conceptName;
		var concept, id;
		
		//the id is this zone name + concept name
		id = this.name++conceptName;
		
		concept = SoundConcept.new(id,parentZone:this); 
				
		conceptDict.add(id.asSymbol -> concept); //create the pointer to the concept
		
		^id;
		
	}
	
	createConceptId { arg conceptName;
		var id;
		id = this.name++conceptName;
		^id
	}
	
	
	//---------------------------------------------------------------------------------------------
	// Create concept and Instances starting from a Database providing a folder with the name of the zone,
	// subfolders with the name of concepts. Each subfolder contains its events.  
	//---------------------------------------------------------------------------------------------

	loadConceptFoldersDatabase { arg aPath, eventNameIsFileName; //path as String, eventNameIsFileName as boolean
		//no async commands

		var zonePath = PathName.new(aPath);
		var zoneName = zonePath.fileName;
		var conceptFolders = zonePath.folders;
		
		
		conceptFolders.do({ arg folder; var files, concept, conceptName;
			
			files = folder.entries;
			
			conceptName = this.name++files[0].folderName;
			//("concept"+conceptName+"has the instances"+files).postln;
			
			
			
			if (conceptDict[conceptName.asSymbol] == nil,{ //new concept
			
				//"newConcept".postln;
				
				concept = SoundConcept.new(conceptName,parentZone:this); //create a new concept 
				//the method add in SoundConcept creates a SoundEvent instance, assign to it a  valid & unique id and return the id
				conceptDict.add(conceptName.asSymbol -> concept); //create the pointer to the concept

				});


			files.do({ arg element; var start, dur, eventName, gaverClass, pos, dy, rolloff, probE, eventArray, tmpFile;
					var wrongLabel = false;
					
					tmpFile = SoundFile.new;
					
					if(tmpFile.openRead(element.fullPath) != false, { //.openRead just read the header
						
						dur = (tmpFile.numFrames/tmpFile.sampleRate);
						start = 0.0; //segmented events
						
						timeAnalysisRec = timeAnalysisRec + dur; //to compute the probabilities

						tmpFile.close; //close the temp
						},{ ("the file "+element.fullPath+" is not a sound File").postln;
					});
					
					if (eventNameIsFileName == true, {eventName = this.name++element.fileNameWithoutExtension}); 
					//the name for event also takes the information about the zone they refers to. 

									
					//if event name exist:
					
					if (eventName != nil, { 
					
						conceptDict[conceptName.asSymbol].eventNames.do({ arg label;
							if (label == eventName, {
							("Annotation file"+element.fullPath+"has a Event name"+eventName+" that was already assigned").postln;
							wrongLabel = true;
							});
						});
					});

					if (wrongLabel == false, {
					
						//if not false eventName is a unique id for a new event of this concept, add it 
						//if eventName is nil the concept.add method will assign a unique label.
						conceptDict[conceptName.asSymbol].add(eventName, gaverClass, pos, dy, rolloff, probE, dur, element.fullPath, start, preserveEventName:eventNameIsFileName);
						//if eventNameIsFileName is true also preserveEventName is true

						}
					);
					
			});			
		});	

		//return the conceptDict to be used for graph generation
		^conceptDict;	
	
	}
	
	
	//return a conceptDict
	loadAnnotatedDatabase { arg aPath; //as String
	
		var analysisRec = IdentityDictionary.new;
		var annotationExt = "csv";
		var c = Condition.new; //for server.sync because asynchronous command

		var path = PathName.new(aPath);
		var l = path.entries;
		var arrayToEraseSpace;
		
		
		
		//load annotated audio files
		Routine.run{
			l.do({ arg element; var tmpFile = SoundFile.new;
						if(tmpFile.openRead(element.fullPath) != false, { //.openRead just read the header
							analysisRec.add( element.fileNameWithoutExtension.asSymbol -> tmpFile);
							timeAnalysisRec = timeAnalysisRec + (tmpFile.numFrames/tmpFile.sampleRate); //seems not very perfect...10s imprecision in an hour!?
							tmpFile.close; //close the temp
						},{ if(element.extension != annotationExt, {("the file "+element.fullPath+" is not a sound File").postln;
								},{
								}) 
							});
			
			});	
			server.sync(c);
			c.wait;
			"Analysis Recordings Header Loaded".postln;
	
	//---------------------------------------------------------------------------------------------
	// Read annotation and initialise abstract sound concept class and its instances as sound events
	//---------------------------------------------------------------------------------------------
			
			// an annotation is in the form [start,,duration,conceptname,eventopts]
			// eventopts = [name, class, pos, dy, rolloff, prob]
			
			Routine.run{
				l.do({ arg element; var score, a, tmpFrames, tmpRate; 
						if(element.extension == annotationExt, {
							score = CSVFileReader.read(element.fullPath, true); //skip empty line
							score.do({arg annotation; var start, dur, conceptName, concept, eventName, gaverClass, pos, dy, rolloff, probE, eventArray;
									var wrongLabel = false;
									start = annotation[0].asFloat; 
									dur = annotation[2].asFloat;
									conceptName = annotation[3];
									eventArray = annotation[4];
									
									//erase all the space from concept annotation field
							arrayToEraseSpace = conceptName.split($ ).reject({arg i ; i=="" ; });
							conceptName = arrayToEraseSpace[0];
							conceptName = this.name++conceptName; //the name store also the zone name because concept with the same name but from different zone could exists. 
							
							//eventArary is managed below
									
									if (eventArray != nil, {eventArray = eventArray.split($ ).reject({arg i ; i=="" ; }) ;
										if (eventArray == [], {("WARNING, annotation of events seems wrong in file"+element.fullPath).postln},{
											//in the csv annotation, use the string nil to declare nil a value										
											//WARNING if somebody doesn't insert a correct type, raise an error 
											
											if (eventArray[0] != nil, { if (eventArray[0] == "nil" , {}, {eventName = eventArray[0].asString})}); //event
											if (eventArray[1] != nil, { if (eventArray[1] == "nil" , {}, {gaverClass = eventArray[1].asString})}); //event
											if (eventArray[2] != nil, { if (eventArray[2] == "nil" , {}, {pos = eventArray[2]})}); //TODO as an Array, or a differential from concept position 
											if (eventArray[3] != nil, { if (eventArray[3] == "nil" , {}, {dy = eventArray[3].asFloat})}); //event
											if (eventArray[4] != nil, { if (eventArray[4] == "nil" , {}, {rolloff = eventArray[4].asFloat})}); //event
											if (eventArray[5] != nil, { if (eventArray[5] == "nil" , {}, {probE = eventArray[5].asFloat})}); //event
											});
										
										});
										
									//("arter CONTROL: "+eventName+gaverClass+pos+dy+rolloff+probE).postln;
									
									
									a = analysisRec[element.fileNameWithoutExtension.asSymbol];
									
									if (a == nil,
										{("Annotation file"+element.fileNameWithoutExtension+"has a name that doen't exist in any SoundFile").postln;
										"annotation incompleted".postln;
										tmpRate = nil;},{
										tmpRate = a.sampleRate;
										});
									
									
									if (conceptDict[conceptName.asSymbol] == nil,{ //new concept
									
										//"newConcept".postln;
										
										concept = SoundConcept.new(conceptName,parentZone:this); //create a new concept 
										
										//the method add in SoundConcept creates a SoundEvent instance, assign to it a  valid & unique id and return the id
										concept.add(eventName, gaverClass, pos, dy, rolloff, probE, dur, a.path, tmpRate*start);

																						conceptDict.add(conceptName.asSymbol -> concept); //create the pointer to the concept
										
										
										
										
										},{ //not nil, concept already exist
										//event name exist?
										
										if (eventName != nil, { 
										
											conceptDict[conceptName.asSymbol].eventNames.do({ arg label;
												if (label == eventName, {
												("Annotation file"+element.fullPath+"has a Event name"+eventName+" that was already assigned").postln;
												wrongLabel = true;
												});
											});
										});
	
										if (wrongLabel == false, {
										
											//if not false eventName is a unique id for a new event of this concept, add it 
											//if eventName is nil the concept.add method will assign a unique label.
										
											conceptDict[conceptName.asSymbol].add(eventName, gaverClass, pos, dy, rolloff, probE, dur, a.path, tmpRate*start);
											
											},{}
										);
	
										
									});
	
							});
							
						});
				});		
				server.sync(c);
				c.wait;
				"Annotated Region Loaded in Buffer".postln;
				
				analysisRec.do({arg f; f.close;}) //close all the SoundFiles readed
			};
			
			
		};
	
		//return the conceptDict to be used for graph generation
		^conceptDict;	
	
	}
	
	
	
	getConcept { arg pureConceptName; //without the zone name
		var conc;
		conc	= this.conceptDict[(this.name++pureConceptName).asSymbol];
		^conc;
	}
	
	
	
	generateConceptProbabilities {	 
		// the method does it just for general concept, anyway the designer could set the probabilities to any preferred value.
		
		"before conceptdict".postln; // debug jjaner
		conceptDict.do({ arg concept; 
			var opts, prob, ar, nevent, duredge, tmpEvent, tmpKey, position;
			
			if (concept.general == true && concept.prob == nil,
				{
				nevent = concept.events.size;
				if (nevent == 0,{
					if (concept.cloneNumber == nil, {("error: zone"+this.name+"generateGraphs on concept"+concept.getName+"has events list empty").postln;})
					}, {
					
					concept.prob = nevent/(this.getRecTimeInHour); 
					});
				},{("I didn't create prob for concept"+concept.name).postln;}
			);
			
		});

		
	}
	
	
	addEventToTimeAnalysisRec { arg aDur;
	
		timeAnalysisRec = timeAnalysisRec + aDur;
		
	}
	
	getRecTimeInHour { var t, hour = 3600.0; //sec
		t = timeAnalysisRec/hour;
		^t;
	}
	
	
/*	deprecated
	//segmentDict is used to play sample that are not organise in SoundConcept. the form is label -> [path, dur]
	addSegment { arg label, path, dur;
		 var exist;

		if (this.segmentDict.size == 0, {this.segmentDict.add(label -> [path, dur]);},
			{	
			exist = false;
			this.segmentDict.do ({arg item; var buffer, uid;
				buffer = item[0];
				uid = this.segmentDict.findKeyForValue(item);
				
				if (uid == label, {exist = true;})
			
			});
			
			if (exist == true, {"The label"+label+"already exists in segmentDict"},
				{this.segmentDict.add(label -> [path, dur]);}
			);
			}
		);
	}*/
	
	getSampleDur { arg path; 	//For sound object that coincide whit a whole sound file, doesn't work for regions. 
			var dur, sample = SoundFile.new;
			
			if (sample.openRead(path) == true,{
				dur = sample.duration;
				},{
				dur = 0; 
				//("The file"+path+"is not a sound file").postln;
				}
			);
			sample.close;
			
			^dur;
	}
	
	
	fromRecDistanceToNormalisationAmp { arg recordingDistance;
			var a2deg = [-0.00114526,  0.1509869 ,  0.17773213]; // coefficient of a 2 degree equation approximation. 
			var x, y;				
			
			x = recordingDistance;
			y = (a2deg[0]*x*x) + (a2deg[1]*x) + a2deg[2] ;			
			^y;
	}
	
	
	//manage graph dict on graph.sc
	
	//set the listened area annotation in graph vertex options vector 
	setListenedArea { arg vID, val, aGraph ;
			aGraph.setvopts(vID, 0, val); //setvopts args are: vID, annotationOrder, val
		}
	
	//set the atm annotation in graph vertex options vector 
	setAtm { arg vID, val, aGraph;	
							// if val = [x,y,W,H] vertex is an Atm. if graphDict[vID][4][1] = nil, vertex is a normal sound event.
			var control = [1,1,1,1]; //[x,y,W,H]
	
			if (val.class == Array && val.size==control.size, {
			
			aGraph.setvopts(vID, 1, val);
	
			},
			{("atm of vID"+vID+"isn't in the correct form [x,y,W,H]").postln}
			);
	}
		
	//set concept name annotation in graph vertex options vector 
	setConcept { arg vID, aConcept, aGraph;
		
			aGraph.setvopts(vID, 2, aConcept.asSymbol);
		
	}
	
	//area position with x,y upper left corner
	moveConcept { arg conceptName, x, y;
		
		var wrong = false;
		var conc = this.getConcept(conceptName.asSymbol);
		var concId = conc.name;
		//the name attribute of the SoundConcept object is the id in the form zone name + concept name. 
		
		if (x < 0, {wrong = true}) ;
		if (y < 0, {wrong = true}) ;
		if (x > world.scapeWidth, {wrong = true}) ;
		if (y > world.scapeHeight, {wrong = true}) ;
		
		if (wrong == false, {
					conc.move(x,y);
					this.changed(this, [\conceptMoved, concId]) ;
			
					},{"error: trying to move a concept with an incorrect position".postln;}
				) ;

	}
	
	
	
	//set random position in graph vertex options vector 
	setRandomPosition { arg vID, area, aGraph; 
		
		var control = [1,1]; //area is an array [width, height]
	
		if (area.class == Array && area.size==control.size, {
		
		aGraph.setvopts(vID, 3, area);},
		{("randomPosition area of vID"+vID+"isn't in the correct form [Width,Height]").postln}
		);
		
	}
	
	
	trasformConceptRandomPosition{ arg conceptName, w, h;
		
		var wrong = false;
		var conc = this.getConcept(conceptName.asSymbol);
		var concId = conc.name;
		//the name attribute of the SoundConcept object is the id in the form zone name + concept name. 
		
		if (w < 0, {wrong = true}) ;
		if (h < 0, {wrong = true}) ;
		if (w > world.scapeWidth, {wrong = true}) ;
		if (h > world.scapeHeight, {wrong = true}) ;
		
		if (wrong == false, {
					conc.trasformRandomPosition(w,h);
					},{"error: trying to trasform a concept random position with incorrect values".postln;}
				) ;

	
	}
	
	// set automatic normalisation parameter to correct the distance of recordings if different from 5m
	setDy { arg vID, val, aGraph ;
			aGraph.setvopts(vID, 4, val); //setvopts args are: vID, annotationOrder, val
		}
	

	generateGraphs { arg aGraph;
		var a;	
		a = aGraph;
		"before concept dict".postln; // debug jjaner
		this.generateConceptProbabilities; //WARNING: the method needs the probability for each concept. 

		this.conceptDict.do({ arg conc; 
						var prob, probConsiderigMultiplePath, events, nevents, hour = 3600.0, pointer, furtherCycle;
						var actantVertexId, ar, position, multiplePath;
						// varibles for edge dur calculation for impulsive sounds
						var edgedur, step = 2, x = 0, resoults = List.new, d = 0, actual = 0, previous = 0, noSenseTriggeringTime = 0; 
			
			//if the conc.general == true, do the automatic generation, if is specif the system expect a manual graph 
			// TODO store the graph in a format
						
			
			if (conc.general == true,{ 
						
				
				ar = conc.ar;
				events = conc.events;
				nevents = events.size;
				position = conc.pos; 
				pointer = 0;
				multiplePath = conc.multiplePath; //number of contemporaries path on the structure generating events. It affects the prob, prob = prob*multiplePath thus:

				//clones need to copy events and prob form father.
				if ((conc.cloneNumber != nil)&&(events[0] == nil),
					 {conc.father.cloneEvent(conc);
						events = conc.events;
						nevents = events.size; 
				});
				
				prob = conc.prob;	//prob of a concept to be percived in the soundscape
				if ((conc.prob == 0)or:(conc.prob == nil), {("warning: zone"+this.name+"generateGraphs on a concept with prob = 0 or nil").postln}, {});
				
				probConsiderigMultiplePath = (prob / multiplePath); //each further actant doubles the probability 
				// probConsiderigMultiplePath is used to compute the edge_dur in impulsive sounds, 
				// the same prob with different value of multiplePath affects the sequencing structure:
				// if multiplePath grows, edge_dur lessen.
				
//				"concept: ".post;
//				this.conceptDict.findKeyForValue(conc).postln;
//				"ar: ".post;
//				ar.postln;
//				"probability: ".post;
//				(prob).postln;
//				if (conc.cont == true, {"Continuous sound".postln;},{"Impulsive sound"});
				
				if (events[0] == nil,{("error: zone"+this.name+"generateGraphs has events list empty").postln;}, {
				//create a vertex for each event and assign position
					events.do({ arg event; var x, y, dur, buffer ;
		
							if (event.pos == nil, {("I set x = 0, y = 0 for"+conc.name+", ").post;
								event.viD = a.addVertex(0,0,0,event.name);
								("POSITION OF VERTEX"+event.viD+"TO BE UPDATED!").postln;
								this.setConcept(event.viD, conc.name, a);},
								
								{
								x = event.pos[0];
								y = event.pos[1];
								dur = event.sampleDur;
								
								event.viD = a.addVertex(x,y,dur,event.name);
								
								
								if ((event.pos.size) > 2,{ //is an Atm
									this.setAtm(event.viD, event.pos, a);
											
								});	
								
								if (conc.randomPosition != nil, {this.setRandomPosition(event.viD, conc.randomPosition, a)}); 
								//store the area of random generation in the graph opts of vertex, this is used in soundscape.play method
								
								this.setConcept(event.viD, conc.name, a);
								
								});
							}) ;
				});
				
				//DO AUTOMATIC GRAPH GENERATION
				
				if (conc.cont == true,{
						//Graph generation for Contiunous (ATM-KEYNOTE) sounds_START
						//The edge duration depends on the duration of the starting vertex, so that the sequencing streams never ends. 
						//It uses vertex dur * 0.8 to guarantee croos fade, for long SO (where dur * 0.8 > 6 sec) uses just 6 sec cross fade. 
						
						//create graphs
						
						furtherCycle = 0;
						
						ar.do({ arg times;
							
							var arBiggerThanEvents;
							
							//in case ar is higer than the number of events. 
							if (times > (events.size - 1), { 
							
								arBiggerThanEvents = (times/events.size).asInteger;
								
								arBiggerThanEvents.do({arg no; times = times - events.size; })
								});
							
							pointer = 1 + (times) - (furtherCycle*events.size);
							
							//point to event_i+1 the first time of edge creation, to event_i+2 the second time, to event_i+3 the etc...
							
							
							//create edge from event_i to event_i+1
							events.do({ arg event; var iDstart,iDstop, tmpEdgeDur, buffer;
								
								iDstart = event.viD;
								
								if (pointer == (events.size), {pointer = 0;}); //edge from last event to first event								
								if (events.size == 1, {iDstop = iDstart; actantVertexId = iDstart;},{ 
								
								iDstop = events[pointer].viD;
								
								if (pointer == 0,
									{actantVertexId = iDstop};);
								
								});
								
//								"edge: ".post;
//								(iDstart+"dur:"+a.graphDict[iDstart][2]).post;
//								" -> ".post;
//								iDstop.postln;	
								
								//find CONTINUOUS SOUNDS SPECIFIC edgedurs values for each event, ar is the number of edge connection in-out from each vertex
								
								
								tmpEdgeDur = (event.sampleDur)*0.8;  // 0.8 in order to have 20 % of superposed time between consequent event
													
								
								if ((event.sampleDur - (event.sampleDur*0.8)) > 6, //cause you put a control in synth def that if fadetime(0.1*sampleDur) > 3 = 3
									{tmpEdgeDur = (event.sampleDur - 6);}
								);
								
								
								
								//("EdgeDur"+tmpEdgeDur).postln;
								
								a.addEdge(iDstart,iDstop,tmpEdgeDur);
								
								pointer = pointer + 1;
					
								//edgedur.postln;
							});
						
						});
					
						
					//Contiunous sounds_STOP
					},{
					
					//Graph generation for impulsive sounds_start
					
					//EDGE DURATION FOR GENERAL IMPULSIVE SOUNDS CALCULATION MODEL:
					
/*	edgedur calculation variables:

 if AR = 1. edgedur is the duration cosidering just 1 edge starting from each vertex and just one edge arriving in each vertex (1edge_in/out) 
 this just depends on prob and initial actants -> edgedur = hour/(prob*multiplePath)
	
	// this is true per AR = 1, per AR > 1 each edge has 1/AR probability to be choosen and each vertex has AR number of edge_in/out
	// assuming the case of AR = 3, and t,s,r the dur of the 3 edge per vertex, as the probability of each edge to be choosen is 1/AR = 1/3 and assuming t = step*s, s = step*r
	// in order to have different generational rhythm per actant, we calcule the durations as: edge1_prob*t + edge2_prob*s + edge3_prob*r = durOfEdge_WhenAR=1(That as prob 1);
	// as he prob of each edge is kostant 1/AR, and assuming step = 3
	// 1/3*t + 1/3*s + 1/3*r = edgedur where  t = 3s, and, s = 3r -> edgeDur_i = 3*edgeDur_i+1

	ar = arrhythmic generation, it's a concept parameter:  it  creates several sequencing path on the graph using, for each futher out edge, a different durations.
	edgedur_AR=1 = edgedur -> duration of all the edges if each vertex has just 1 in/out edge
	x = edgedur_i per AR > 1
	resouls; // list of edgedur result of x_i per i = 1,..ar;
	d = sum of factor for edge calculation
	events.size //nevent
	step	is a parameter that describe the difference between each new further vertex's edge, actually is set to 2 but this could also became a dynamic parameter. 
	
	Algorithm
	// sum per i = 1,...,AR of (1/AR)*(x_i * (step^i)) = edgedur 
	// where step represent the differential beetween two concecutive x (edgedur_i per AR > 1). this means:
	// x_i = step*x_(i+1) 
	// d = List[step^i] with i from 0 to AR
	// x_i = (edgedur_AR=1)*AR / d_i
*/	
					
					
					edgedur = (hour/probConsiderigMultiplePath);
						
					//find edgedurs values
					ar.do({ arg i; var tmp;
						tmp = step**(i); //step^(i);
						d = d + tmp;
						//"tmp = ".post;
						//tmp.postln;
						//"d = ".post;
						//d.postln;
						//"i*i = ".post;
						//(i*i).postln;
						//">>>>>>>>>>>>>".postln;
					});
					
					x = (edgedur*ar)/d;
				
					ar.do({ arg i;	//from 1 to ar. The edges number depend on ar. If ar = n, n edge connection in-out per vertex and resoults.size = n
						if (i == 0, 
						{
						actual = x;
						resoults.add(actual);},
						{
						previous = actual;
						actual = previous * step;
						resoults.add(actual);
						if (actual < 0.001, {noSenseTriggeringTime = noSenseTriggeringTime +1;});
						}
						);
						
					});
					
					
					if (noSenseTriggeringTime != 0, {
					("WARNING: in concept"+conc.name+(noSenseTriggeringTime*events.size)+"edges duration are below 0.001 sec, cause to prob and ar settings").postln;
					});
					
				
					
					
					//find edgedurs values_end
					
					
					//create graphs
					furtherCycle = 0;
					
					ar.do({ arg times;
						
						pointer = 1 + (times) - (furtherCycle*events.size); //point to event_i+1 the first time of edge creation, to event_i+2 the second time, to event_i+3 the etc...
						//("pointer"+pointer+"= 1 + times - furterCycle*events.size"+times+furtherCycle+events.size).postln;
						
						//create edge from event_i to event_i+1
						events.do({ arg event; var iDstart,iDstop, tmpEdgeDur;
							
							//find id_start from event label
							iDstart = event.viD;
							
							if (pointer == (events.size), {pointer = 0;}); //edge from last event to first event
							
								
							if (events.size == 1, {iDstop = iDstart; actantVertexId = iDstart;},{ 
								
								//find id_stop from event label
								iDstop = events[pointer].viD;
								//("pointer"+pointer).postln;
								
								//and store the iD of the first event of the concept for actant Setup
								if (pointer == 0,
										{actantVertexId = iDstop}; 
								);
							});
							
							tmpEdgeDur = resoults[times];
							
//							"edge: ".post;
//							iDstart.post;
//							" -> ".post;
//							iDstop.postln;
//							tmpEdgeDur.postln;
							
							a.addEdge(iDstart,iDstop,tmpEdgeDur); //a is graph
							
							pointer = pointer + 1;
					
						
						});
						
						if (pointer == (events.size - 1), {furtherCycle = (furtherCycle + 1);});
					
					});
					
							
					//Graph generation for impulsive sounds_stop
				});
				
				
				//Finally, setup actant per concept
				
				if (multiplePath == 1, {
					this.conceptDict[(conc.name).asSymbol].actantIdDict.add(actantVertexId -> runner.na); //actantId on runner object
					runner.addAndSetup(actantVertexId);
					this.conceptDict[(conc.name).asSymbol].addActant(actantVertexId);
//					"actantId: ".post;
//					runner.na.postln;
//					"actantVertexId: ".post;
//					actantVertexId.postln;
					},{
						
						multiplePath.do({ arg times; var newActant, cycle;

							
							cycle = (times / nevents).asInteger;
							
							newActant = (actantVertexId + (times - (cycle*nevents)));
							this.conceptDict[(conc.name).asSymbol].actantIdDict.add(newActant -> runner.na); //actantId on runner object
							runner.addAndSetup(newActant);
							this.conceptDict[(conc.name).asSymbol].addActant(newActant);
							
		//					"actantId: ".post;
		//					runner.na.postln;
		//					"actantVertexId: ".post;
		//					actantVertexId.postln;
								
							
						});
					}
				);
				
				
				
				conc.graph = "auto";
			},{
				("concept"+conc.name+"is specific, a manual graph creation process was needed").postln;
				
				//anyway the parameters in graph opts must be updated also for all the events of this concept
				
								//create a vertex for each event and assign position
				conc.events.do({ arg event; var id, dur ;
				
							dur = event.sampleDur;
							
							id = a.getvID(event.name);
							event.viD = id;
							
							
							//update vertex dur
							a.graphDict[id][2] = dur;
							
							
							if ((event.pos.size) > 2,{ //is an Atm
								this.setAtm(event.viD, event.pos, a);
										
							});	
							
							if (conc.randomPosition != nil, {this.setRandomPosition(event.viD, conc.randomPosition, a)}); 
							//store the area of random generation in the graph opts of vertex, this is used in soundscape.play method
							
							this.setConcept(event.viD, conc.name, a);
							
							if ((event.pos.size) > 2,{ //is an Atm
								this.setAtm(event.viD, event.pos, a);
										
							});	
							
							//store the area of random generation in the graph opts of vertex, this is used in soundscape.play method
							if (conc.randomPosition != nil, {this.setRandomPosition(event.viD, conc.randomPosition, a)}); 
							
							this.setConcept(event.viD, conc.name, a);
							
				}) ;
				
				
			
			});	
			
			
		});//do all this for each concept
	
	}
	
/*
//AMPLITUDE BY DISTANCE FORMULA
// loudness in real life approximately changes -6db each times the distance doubles: if at 10m the loudness of source s is 50db, at d = 5m the s Loudness = 44db.
// in supercollider, each times the amp double this simulates a change of +6 db, for example evaluate this: [0.125, 0.25, 0.5, 1, 2].ampdb; 
// offsetListenedArea (maybe better rol) is a roll off to increase the perception of a vertex depending on the same distance. 
// dy a parameter to scale a sound depending on the distance it was recorded.
// The standard Rec_distance is 5m where dy = 1, if Rec_distance = 2.5m dy = 0.5, if Rec_distance = 10, dy = 2

// the Final amp formula would be
//	amp = dy/(dy + (offsetListenedArea *(d - dy))
// a = dy/(dy + (rol * (d - dy))
*/


	
	play { arg message ;	//more here is : [vID,[x, y, vDur, vLabel, [offsetVertexListenedArea, Atm, conceptName, randomPosition]],
						// eID, [end, eDur, eID, eOpts], aID, weight, offsetWeight, count]
			var label, weight, offsetWeight, offsetVertexListenedArea, dur, fadetime, sustime, atm;
			var al, bl;
			var result;
			var xv, yv, synthplaying, r, name;
			var randomPosition;
			var concept,event, eId, concGain;
			var buffer, bufn, b; //b = bus
			var mute = Boolean;
			var grouptmp;
			var dy, eventNormalisation; //dy is the distance of recording if different from 5m

			
			mute = false;
			
			
			label = message[1][3] ;
			xv = message[1][0]; //vertex position
			yv = message[1][1];	
			dur = message[1][2] ; 
				

			offsetVertexListenedArea = message[1][4][0];
			atm = message[1][4][1]; //atm dimension
			randomPosition = message[1][4][3];
			
			if (randomPosition != nil, { //(xv,yv) is the point of the upper left corner of the area in which a random position in generated. 
				xv = xv + randomPosition[0].rand; //W.rand
				yv = yv + randomPosition[1].rand; //H.rand
//				("randomPostion x,y = ("+xv+","+yv+")").postln;
			}); 
			
	 		
	 		
	 		// Don't play desactivated concept. 	 
	 		
	 		
	 		// and
	 		// Ignore activation messages for other zone: if the runner is sending the activation message for vertex that are not in this zone, do nothing.
	 		// and 
	 		// allow old version without conceptDict to play.
	 		if (message[1][4][2] == nil, {
					//play this vertex
					},{
					
					concept = message[1][4][2].asSymbol;
									
					//allow multiple cartoon model instances to play together (each instace is a zone of a bigger soundscape)
					//because master thesys model have not concetDict
					
					if (this.conceptDict == nil, { //don't play this vertex
						mute = true;
					},{

						//because each cartoonModel instances have not the soundconcept of the other zones. 
						if (this.conceptDict[concept] == nil, { //don't play this vertex
							mute = true;
							},{
							//control to desactivate concept
							if (this.conceptDict[concept].active == true , {
								//play this vertex
								},{ 
								//don't play this vertex
								mute = true;
							
								}
								); 
	
							}
						);
							
					});	
				
				}); 
			
	 			
	 			//don't play samples if the SoundConcept is muted 
	 
				 if (mute, {
				 	//do nothing
				 },{		
									
						concept = message[1][4][2];
						label = message[1][3] ;
						
			// ugly indented if
			if ( concept != nil, {
						
						//FADETIME: 
						//a minimum of fade in and out is relevant to simulate sound object approaching instead of appearing from nowhere...
						//the fadetime is proportional to the sample-region duration because they can have very different duration and a default value will not work.
						//for instace there are few sound objects that have dur = 0.3s
						//this algoritm in order to set a tie for long files fadetime
						
						if ((dur*0.1) > 3, 
		 					{
		 					fadetime = 3;
		 					sustime = dur - (2*fadetime);
		 					},{
		 					fadetime = (dur*0.1);
		 					sustime = (dur*0.8)
		 					}
		 				);
		 				
		 				
						
						//not so clean, but right now there is no other way to access event.
						conceptDict[concept].events.do({arg e; 
												if (e.name == label,{
												event = e;
												});
											});
											
											
						concGain = conceptDict[concept].getGain;
						
						//dy = normalisation, adjust the sample amp if the recording distance if different from 5 meter. need to be passed by method call because dy is a vertex property
						dy = event.dy;
						eventNormalisation = event.normalisation;
						eId = event.viD; //the vertex id on graph corresponding to the event
						
						
						if (dy == nil, {dy = 1}, //dy = 1 means the vertex is already normalised or recorder at 5m. 
							{ dy = this.fromRecDistanceToNormalisationAmp(dy); };
						);
					
						if (eventNormalisation == nil, {}, //do nothing
							{ dy = eventNormalisation; 
								};
						);
					
					
						//cueSoundFile: numChannels = 1 could change in future, 32768 is the default buffer size
						if (bufFree.size == 0,{
							buffer = Buffer.cueSoundFile(server, event.samplePath, event.sampleStartFrame, 1, 32768);
							server.sync;
							bufDict.add(buffer.bufnum -> buffer);
							bufn = buffer.bufnum;
							
							},{
							//pop return and remove the last element of the List, a bufnum of an existing & Free Buffer instance, that have been closed but not free
							bufn = bufFree.pop;

							bufDict[bufn].cueSoundFile(event.samplePath, event.sampleStartFrame, 1, 32768);
							server.sync;

							}
						);
						b = Bus.audio(); //Server.default, 1 Channel
						
						grouptmp = Group.new(soundscapeGroup);
						
						synthplaying = Synth.new(\sequencing,
									[\bufnum,bufn,\bus,b,\fadetime, fadetime, \sustime, sustime, \gain, gain, \conceptGain, concGain,\dy, dy, \controllerVolume, controllerVolume], grouptmp);
						
						name = synthplaying.asString;
						
						sampleplaying.add(name -> [synthplaying, xv, yv, offsetVertexListenedArea, atm, b, grouptmp, this.name, concept, m, eId]); //for listener and this
						ns = ns + 1;
						
						SimpleController(synthplaying).put(\n_end, { |synth, what, moreArgs| 
												//free bus
												sampleplaying[name][5].free;
												//remove so from list of active sound obejct
												sampleplaying.removeAt(name);
												ns = ns-1;
												//close the buffer and put it on the list of free buffer
												//bufDict[bufn].closeMsg({"bufferClosed".postln;});
												bufDict[bufn].close;
												bufFree.add(bufn);
												//remove SImpleController
												synth.releaseDependants;
												//the group and the Listener's synth are free by DoneAction 14
												 });
						NodeWatcher.register(synthplaying);

			});
			//ugly if_end
		//play sample_end
		this.changed(this, [\samplePlayed].addAll(sampleplaying[name])); //[synthplaying, xv, yv, offsetVertexListenedArea, atm, bufn, b, grouptmp, this.name]; //for listenerS
		});
						
	}


	setWorld { arg aSoundWorld;
	
		this.world = aSoundWorld;
	
	}

}