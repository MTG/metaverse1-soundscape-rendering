//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.



SoundscapeController {  
		var <> runner, <> world, <>soundscapeName;
		var <> actantIdDict;
		var <> multipleActants;
		var <> modifCache;
		var <> processInterruption;
		var <> processId;
		var <> activeProcess;
		var <> controllerGUI, gui, <>networkAddress, <>zones, <>concepts;
		var <> savesPath, cache;
		var <> gainRange;
		var <> conceptProbDict; //prob in %

		*new { arg aSoundWorld, gui, guiRemoteadress, runner;
			^super.new.initSoundscapeController(aSoundWorld, gui, guiRemoteadress, runner);
		}

		initSoundscapeController{ arg aSoundWorld, isGui, guiRemoteadress, aRunner;
			var i = 0;
			world = aSoundWorld;
			if (aRunner == nil, {runner = world.runner}, {runner = aRunner});
			soundscapeName = world.getName;
			
			actantIdDict = IdentityDictionary.new; //vertexiD(eventually cloned) -> actantId
			multipleActants = IdentityDictionary.new; //store the information about vertex that has more than one actant starting from it (vertex -> n actants)
			activeProcess = IdentityDictionary.new; // parameter -> [boolean, cache], if true the parameter modifications are still under processing
			processInterruption = List.new;
			conceptProbDict = IdentityDictionary.new;
			modifCache = nil;
			processId = 0;
			
			zones = List.new;
			concepts = List.new;
			
			cache = PathName.new(Platform.userAppSupportDir);
			cache.postln;
			this.createSavesPath;	
			
			world.soundZones.do({ arg aZone; 
			
				OSCresponderNode(nil, ((soundscapeName++"/"++aZone.name++"/gain").asSymbol),
					{|time, resp, msg| 
							world.soundZonesDict[aZone.name.asSymbol].setControllerVolume(msg[1].asFloat);
							
					} ).add;
					
					zones.add(aZone);  //zone are both OSC path grammar and parameter. 
					
					aZone.conceptDict.do({ arg concept;
						if (i == 0, {
							this.gainRange = concept.gainRange;
							});
						i = 1;	
						
						concepts.add(concept.name);
						OSCresponderNode(nil, ((soundscapeName++"/"++aZone.name++"/"++concept.getName++"/prob").asSymbol),
							{|time, resp, msg| 
								this.modifyPerformance(concept.getId, msg[1], aZone);
							} ).add;
							
							
/*						OSCresponderNode(nil, ("/"++aZone.name++"/"++concept.name++"/prob_changing_graph"),
							{|time, resp, msg| 
								//"receiving osc".postln;
								
								if (aZone.conceptDict[concept.name.asSymbol].isContinuous, {},{
										if ((aZone.conceptDict[concept.name.asSymbol].getProb == -1), {}, {
											aZone.conceptDict[concept.name.asSymbol].modifyProbByEdgeDurs(msg[1]);
										});
								});
								
							} ).add;	*/
							
						
						OSCresponderNode(nil, ((soundscapeName++"/"++aZone.name++"/"++concept.getName++"/gain").asSymbol),
							{|time, resp, msg| 
								var gain;
								gain = this.mapGain(msg[1]);
								aZone.conceptDict[concept.getId.asSymbol].setGain(gain);
							} ).add;
						
					});
					
			});
			
			gui = isGui;
			networkAddress = guiRemoteadress;
			
			if (networkAddress == nil, {
					networkAddress = NetAddr("127.0.0.1", 57120); //local host address
				}, {
					this.setNetworkController(networkAddress);
				});
			
			if (gui == false,{},
				{
				controllerGUI = SoundscapeControllerGUI.new(this);
				//Soundscape controller interface networking
				
			});
		}
		
		mapGain {arg value, unmap;
			var start = gainRange[0];	
			var max = gainRange[1]; 
			var g, h, startExp = 0.7;
			var result;
			var correctValue;
			
			if (start == 0, {},{"warning need to revise soundscapeController.mapGain".postln});
			
			h = ControlSpec(0.001, 32, \exp, 0.0001);
		
			if (value == start, {result = 0},{
		
				if (unmap == true, {
					if ((value > start)&&(value <= max), {
						

								result = h.unmap(value);

							
							}, {correctValue = false; ("error: soundscapeController.mapGain unmap(incorrect arg)"+value).postln;});
					
					},{
			
			
					if ((value > 0)&&(value <= 1), {
							
								result = h.map(value)
							
							}, {correctValue = false; ("error: soundscapeController.mapGain(incorrect arg)"+value).postln;});
					
					});
			});
		^result;

		
		}
		
		savePreset { arg presetName; 
			var saveFolder, file, filePath, aDom;
			var zoneElement, conceptElement;
			
			if (presetName == nil, {"error: soundscapeController.savePreset(nil)".postln});
			if (savesPath == nil, {"error: "+savesPath.fullPath+"doesn't exist".postln;});
			if (world.soundscapeName == nil, {"error: SoundscapeController has no world name to save its presets".postln;});
			
			filePath = savesPath +/+ (world.soundscapeName++"_"++presetName++".xml");
			
			file = File.new(filePath.fullPath,"w");
			aDom = DOMDocument.new(filePath.fullPath);
			
			this.controllerGUI.getProbSliderValues;
			
			
			world.soundZones.do({arg zone;
				
				zoneElement = aDom.createElement("zone");
				aDom.appendChild(zoneElement);
				zoneElement.setAttribute("name",zone.name);
				zoneElement.setAttribute("gain",zone.getControllerVolume.asString);
				
				
				zone.conceptDict.do({arg concept;
						
						conceptElement = aDom.createElement("concept");
						zoneElement.appendChild(conceptElement);
						conceptElement.setParentNode(zoneElement);
						conceptElement.setAttribute("name",concept.getName.asString);
						conceptElement.setAttribute("gain",concept.getGain.asString);
						conceptElement.setAttribute("probability",this.conceptProbDict[concept.getId.asSymbol].asString);
					
					});
				});	
			aDom.write(file);	
			file.close;	
		}
		
		
		loadPreset {arg presetName; 
			var saveFolder, filePath, aDom;
			var zonesTag, conceptsTag;
			var i, c = Condition.new(false);

			if (presetName == nil, {"error: soundscapeController.loadPreset(nil)".postln});
			if (savesPath == nil, {"error: "+savesPath.fullPath+"doesn't exist".postln;});
			if (world.soundscapeName == nil, {"error: SoundscapeController has no world name to load its presets".postln;});
			
			filePath = savesPath +/+ (world.getName++"_"++presetName++".xml");
			
			this.controllerGUI.getProbSliderValues;
			
			i = 0;
			
			if (File.exists(filePath.fullPath), {
				aDom = DOMDocument.new(filePath.fullPath);
				
				zonesTag = aDom.getChildNodes;
				
				{
				zonesTag.do({arg zoneTag;
					var zoneName, value;
					var conceptName, concept;
					
					zoneName = zoneTag.getAttribute("name");
					
					value = zoneTag.getAttribute("gain").asFloat;
					
					
					if (value == world.soundZonesDict[zoneName.asSymbol].getControllerVolume().asFloat, {},{
						networkAddress.sendMsg((soundscapeName++"/"++zoneName++"/gain").asSymbol,value);
					
					});
					
					conceptsTag = zoneTag.getChildNodes;
						
					conceptsTag.do({arg conceptTag;
							
							conceptName = conceptTag.getAttribute("name");
							concept = world.soundZonesDict[zoneName.asSymbol].getConcept(conceptName);
							value = conceptTag.getAttribute("gain").asFloat;
							
							if (value == concept.getGain.asFloat, {},{
								
								concept.setGain(value);
								

							});
							
							value = conceptTag.getAttribute("probability").asFloat;
																			if (value == (this.conceptProbDict[concept.getId.asSymbol]).asFloat, {},{
					
								 if (value == -1, 
								 	{"incorrect probability".postln;},
								 	{networkAddress.sendMsg((soundscapeName++"/"++zoneName++"/"++conceptName++"/prob").asSymbol,value);
									 this.conceptProbDict[concept.getId.asSymbol] = value;
									 	}
								 );
							});
							
							
						
						});
					i = i + 1;
					
					if (i == zonesTag.size, {c.test = true; c.signal;})
					});	
					c.wait;
					1.wait;
					//"waited".postln; //we are sure that all the messages are send, but osc take some times to be received, wait more. 
					this.changed(this, [\loadPreset, presetName, this.conceptProbDict]) ;
					}.fork;
			
			}, {"error: "+filePath.fullPath+"doesn't exist".postln;})
			
		
		}
		
		
		
		setNetworkController { arg address;
			
			case
				{address.class == String}
					{networkAddress = NetAddr(address, 57120)} //a remote host address, predifined port
				{address.class == NetAddr}
					{networkAddress = address}
		
		
		}

	
		
		resetProcessId {
			processId = 0;
		}
	
	
		
		//the limit of actants is set at 100 for the moment (in the ControlSpec increment)
		mapping { arg percent, concept;  //arg is a percentage of the probability, 10%(0.1 from slider) the initial prob, 0 is off, 100% is initial prob * 2 * number of events
				var increment, decrement, newactants; //the percentage is transformed in the number of actant navigating the graph. 
												//The interactive probability of the concept is n of actant * initial probablity.  
				var limit, flag, actantVertexId;
				var actantVertexIds = List.new;
				var pauses = List.new;
				
				
				if (percent >= 0.1, { //defalt value in slider , 10% = initial prob
					//we use 2 Spec in order to set the initial value (0.1) to the default number of actant of the graph. 

					//the initial value is multiplePath because for concept that as multiple actant at the init, the system uses this value as init
					//increment = [concept.multiplePath, concept.events.size*2, \lin, 1].asSpec; //a ControlSpec
					increment = [concept.multiplePath, 100, \lin, 1].asSpec; //a ControlSpec
					
					newactants = increment.map(((percent-0.1)*(1/0.9))); 
					//because the range is from 0.1 to 1 (range of 0.9) and map need values from 0 to 1 (range of 1)
					
					},{
					decrement = [0,concept.multiplePath, \lin, 1].asSpec;
					newactants = decrement.map((percent*10));
					}
				);
				
				^newactants;
				
		}
		
		
		modifyPerformance { arg aSoundConceptName, value, aSoundscape, aParameter; 
			var numActants, concept;

			concept  = aSoundscape.conceptDict.[(aSoundConceptName).asSymbol];
			numActants = this.mapping(value, concept);
				
			concept.modifProbability(numActants, runner);			
		
		}
		
		
		
		
		setActivationPreset { arg presetName, zone, onConceptArray, offConceptArray; //onConceptArray =  [[Concept to be activated],[Concept to be desactivated]]
			var offArray;
			
			if (offConceptArray == nil, {
				offArray = [onConceptArray[1], onConceptArray[0]]; //the onConcept contrary
			});
		
			OSCresponderNode(nil, (soundscapeName++"/"++zone.name++"/ActivationPreset/"++presetName),
				{|time, resp, msg| 
						
							
						if( msg[1] == 0,{ //on preset
							(""+presetName+"on").postln; //visualise in button
							
							onConceptArray[0].do({ arg name; //concepts to be activated
								var index;
								index = name.asSymbol;
								
								zone.conceptDict[zone.getConcept(index).name].active = true;
							});
							
							onConceptArray[1].do({ arg name; //concepts to be desactivated
								var index;
								index = name.asSymbol;
								zone.conceptDict[zone.getConcept(index).name].active = false;
							});
								
							//put all the other preset off - switch button between each others. 
							
							},{if (msg[1] == 1,{ //off preset
								
								(""+presetName+"off").postln;
								offArray[0].do({ arg name; //concepts to be activated
									var index;
									index = name.asSymbol;
									zone.conceptDict[zone.getConcept(index).name].active = true;
								});
								
								offArray[1].do({ arg name; //concepts to be desactivated
									var index;
									index = name.asSymbol;
									zone.conceptDict[zone.getConcept(index).name].active = false;
								});
							
							});
						});
			} ).add;
			

			this.changed(this, [\ActivationPreset, presetName, zone]) ;
		
		}
		

		//TODO
		createSavesPath {
			var f, fs, tmp, name;
			var found;
			
			name = cache +/+ PathName.new("soundscapePerformancePresets");
			if (name.isFolder {
				
					savesPath = name;
				
				}, {
					("mkdir "++name.fullPath).unixCmd;
					});
		
		}
		
		
	
}

SoundscapeControllerGUI { 
	
	var controller;
	var <> zones, <>orderedZones, zonei, zoneActivationPresets ;
	var <>soundscapeName;
	var <> actors;

	var <> m ;
	var w, yStep, i;
	
	var resetBut;
	var resetEnabledcalls;
	
	var <> sliders;
	
	var x1, x2, x3;
	var columnspace = 25;
	var wHeight, wWidth;
	var textWidth = 200;
	var sliderWidth = 300;
	var rowHeight = 15;
	var borders = 10;	
	var presetButt;
	var presetWidth = 100;
	var probNumBoxWidth = 60;
	
	
	var button; //tmp variable for update /newActivationPreset
	
	var <>inisialisationCompleted; //Boolean
	
	*new { arg aController;
		^super.new.initSoundscapeControllerGUI(aController);
	}
	
	
	initSoundscapeControllerGUI { arg aCont;

		controller = aCont;
		controller.addDependant(this);
		
		zones = controller.zones; //MtgModel objects List
		
		zones.do({arg zone; zone.conceptDict.do({ arg concept;
				concept.addDependant(this);
			});
		});
		
		this.m = controller.networkAddress;
		
		soundscapeName = controller.world.getName;
		
		orderedZones = SortedList.new;

		
		zones.do({ arg zone;
			orderedZones.add(zone.name);
		});
		
		
		actors = controller.concepts; // String List, concept name
		
		yStep = rowHeight+(rowHeight/4);
		
		x1 = borders+textWidth+columnspace; //the x position of the gain control column
		x2 = x1+sliderWidth+columnspace;  //the x position of the probability control column
		x3 = probNumBoxWidth+x2+sliderWidth+columnspace;//position of preset column
		
		wHeight = (yStep*(actors.size +1 ))+80; //+1 label row
		wWidth = (x3+presetWidth+borders+borders); //in case of scrool bar visaulisation

		
		resetEnabledcalls = 0; //what it is for???
		
	//	{
		this.createGui;	
//		1.wait;
//		"waited".postln;


		//}.fork;
	}
	
	getProbSliderValues {
		this.sliders.do({arg sliderArray;
					var conceptName;
				if (sliderArray.size == 2, { //concept slider
				 		conceptName = sliders.findKeyForValue(sliderArray);
				 		controller.conceptProbDict.add(conceptName.asSymbol -> sliderArray[0].value); //concept prob in %
				});
			});
		//controller.conceptProbDict.postln;
		//controller.conceptProbDict.do({arg prob; prob.postln;});
	}
	
	createGui { arg aConceptProbDict;
		var  slider, text, numBox;
		var orderedActors;
		
		
		zonei = IdentityDictionary.new; // store the y position of zone in window
		zoneActivationPresets = IdentityDictionary.new;

		
		sliders = IdentityDictionary.new;
		
		
		i = 0;
	
		{
		
		w = Window("Soundscape controller", Rect(200, Window.screenBounds.height-200,wWidth,wHeight), scroll: true);  
		//when you add element throught user interaction you can w.bound = resize bound...do you need all the elemet previously created?
		
		//sliders column labels				
		text = StaticText(w, Rect(x1, (10 + (yStep*i)), sliderWidth, rowHeight)); 
		text.string = "Gain: 0 to 1";
		
		text = StaticText(w, Rect(x2, (10 + (yStep*i)), sliderWidth, rowHeight));
		text.string = "Probability:";   //need a probability parameter visualisation. 
		
		i = 1;
		
		//zones
		orderedZones.do({ arg zoneName;
				var zone = controller.world.soundZonesDict[zoneName.asSymbol];
				var correctProb;
				
				
				//zone gain controll
				text = StaticText(w, Rect(10, (10 + (yStep*i)), textWidth, rowHeight)).background_(Color.white); //UNDERLINE ZONE 
			
				text.string = " "+zone.name+" ";
				slider = Slider( w, Rect( textWidth+20, (10 + (yStep*i)), sliderWidth, rowHeight ));
				
				slider.value_(zone.getControllerVolume); //don't do the action
				
				// the action function is called whenever the user moves the slider
				slider.action = { arg butt;
						m.sendMsg((soundscapeName++"/"++zone.name++"/gain").asSymbol, butt.value); 
				    };
				
				i = i + 1; //vertical position in GUI
				zonei.add(zoneName -> i);
				sliders.add(zone.name.asSymbol -> slider);
				
			
				//visualise concept per alphabetic order
				orderedActors = SortedList.new;
				
				
				zone.conceptDict.do({ arg concept;
						
						orderedActors.add(concept.name);		
				});
				
				orderedActors.do({ arg conceptName;
					var conceptNameWithoutZone;
				
					//concept label
					text = StaticText(w, Rect(borders, (10 + (yStep*i)), textWidth, rowHeight)).background_(Color.grey);
					
					
					conceptNameWithoutZone = conceptName.asString[(zone.name).size..];
					
					text.string = " "+conceptNameWithoutZone+" ";
				
					//gain control
					
					slider = Slider( w, Rect( x1, (10 + (yStep*i)), sliderWidth, rowHeight ));


					slider.value_(controller.mapGain(zone.conceptDict[conceptName].getGain, unmap:true)); //don't do the action
					
					slider.action = { arg butt;
					
							m.sendMsg((soundscapeName++"/"++zone.name++"/"++conceptNameWithoutZone++"/gain").asSymbol, butt.value); 
					    };
					
					
					//probability visualiser
					
					numBox = NumberBox(w, Rect(x2, (10 + (yStep*i)), (probNumBoxWidth - 2), rowHeight));
					
					//correctProb = zone.conceptDict[conceptName].setTmpProb;
					correctProb = zone.conceptDict[conceptName].getProb;
					
					if ((correctProb != nil)or:(correctProb != -1), {numBox.value = correctProb}, 
						{numBox.value = -1; ("WRONG actants setup for concept"+conceptName).postln});
					
					numBox.stringColor=Color.black;
				
					//probability controler

					slider = Slider( w, Rect( (x2 + probNumBoxWidth), (10 + (yStep*i)), sliderWidth, rowHeight ));
					
					if(aConceptProbDict != nil, {slider.value_(aConceptProbDict[conceptName.asSymbol];)}, {slider.value_(0.1)}); //don't do the action
					
					if (correctProb == -1, {slider.enabled_ (false)});
					
					slider.action = { arg butt;
							m.sendMsg((soundscapeName++"/"++zone.name++"/"++conceptNameWithoutZone++"/prob").asSymbol, butt.value); 
					    };
					
					i = i + 1;
					sliders.add(conceptName.asSymbol -> [slider,numBox]);
				});	

		});
			
/*		resetBut = Button(w, Rect(((w.bounds.width/4)-50), (w.bounds.height - 30), 150, 20)).states_([["Reset Soundscape",Color.black,Color.new(0,0.48,0)]]).action_({arg butt;
							
							sliders.do({arg slider;	[slider,numBox]
								slider.valueAction_(0.1);
							});
							m.sendMsg((.....), butt.value); //maybe not usefull!
							});	*/		
		w.front;	
					
		inisialisationCompleted = true;	
				
		}.defer;
	
	}
	
	
	setResetEnabled {	arg boolean;
		
		case 
		{boolean == false} { 
						resetBut.enabled_ (false);
						resetEnabledcalls = resetEnabledcalls + 1;
						}
		
		{boolean == true} {
						resetEnabledcalls = resetEnabledcalls - 1;
						if (resetEnabledcalls == 0, {resetBut.enabled_ (true);},{
							if (resetEnabledcalls < 0, {"warning: resetEnabledcalls ="+resetEnabledcalls.postln});
						});
						}
	}
	
	
	update { arg theChanged, theChanger, more; 
		var button, multipleColumn_x, multipleColumn_y;
		

		case 
			{ more[0] == \ActivationPreset } 
				{
					//presetName = more[1];
					//zone = more[2];
					
					
					//move button on the right if the soundscape GUI space is finished
					if (zoneActivationPresets[more[2].name].size > controller.world.soundZonesDict[more[2].name.asSymbol].conceptDict.size, {
					//TODO, don't work
/*						"maggiore".postln;
						multipleColumn_x = presetWidth+columnspace*(zoneActivationPresets[more[2].name].size/controller.world.soundZonesDict[more[2].name.asSymbol].conceptDict.size).asInteger;
						multipleColumn_y = controller.world.soundZonesDict[more[2].name.asSymbol].conceptDict.size * (1 + multipleColumn_x);*/
						}, {	
						multipleColumn_x = 0;
						multipleColumn_y = 0;
					});
									
					button = Button.new(w, Rect( x3+multipleColumn_x, ((yStep*zonei[more[2].name])+(yStep*zoneActivationPresets[more[2].name].size) - multipleColumn_y ), presetWidth, rowHeight+2));
					button.states_([[""+more[1],Color.black,Color.green],[""+more[1],Color.black, Color.red]]);
					button.action_({arg butt;
										m.sendMsg((soundscapeName++"/"++more[2].name++"/ActivationPreset/"++more[1]).asSymbol, butt.value);
										});

					
					if (zoneActivationPresets[more[2].name] == nil, 
						{
						zoneActivationPresets.add(more[2].name -> List.new.add(button))},
						{zoneActivationPresets[more[2].name].add(button)}
					);	
					
				
				}
			{ (more[0] == \tmpProb).and(this.inisialisationCompleted == true) }
			
				{
					controller.world.soundZonesDict[theChanged.zone.asSymbol].conceptDict.do({ arg concept;
						//(""+concept.name+concept.tmpProb).postln;
						
						if (concept.name == theChanged.name, { 
						
							if ((concept.tmpProb != nil), { 
								{sliders[concept.name.asSymbol][1].value = theChanged.tmpProb}.defer;
								} 
							);
								
						});
						//sliders[concept.name][1] = numBox for prob visualisation	
					})
				}
			
			{ more[0] == \loadPreset}	
				{w.close;
				this.createGui(more[2]);}
				
				
	
	}
	
	
}
