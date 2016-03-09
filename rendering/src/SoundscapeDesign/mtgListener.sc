//MTG <Music Technology group> www.mtg.upf.edu//UPF <Universitat Pompeu Fabra>//Design and development by Mattia Schirosa//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.


MtgListener {

	var <>world, <>soundscapeName;
	var <>positionResponders, <>moveposResponder;
	var <>la, <>lb, <>lorient;
	var positionCache;
	var <>offsetAngle, <>newPi, <>scapeWidth, <>scapeHeight, <>address;
	var <>perceptionArea;
	var <>counter = 0;
	var <>uid; //unique id, the OSC protocol for changing position depends on the uid, allowing Multiple Listeners
	var <>listenerSynths;
	var <>channelOut;
	var <>server;
	//var <>threshold; // allows to avoid playing imperceptible samples
	var <>zoneAmps,muteRestWorldCache;
	var <>init; //for update position the first sample activation message sent. 
	var http, proxy;
	
	var <>scoreTranscription, <>startTime; 
	
	*new { arg a = 1, b = 1, aSoundWorld, uid = 1, channelOut = 0, http, proxy = nil ; 		//a,b initial position listener in the virtual space
		^super.new.initMtgListener(a, b, aSoundWorld, uid, channelOut, http, proxy)
	}
	initMtgListener { arg aA, aB, aSoundworld, aUid, aChannelOut, ifHttp, incaseofProxy;
		
		var spatialisationDef; 
		
		la = aA;		
		lb = aB;
		uid = aUid;
		newPi = pi ;
		lorient = (newPi/2);	//pi standard value for p greco in Sc is buggy?! 
		offsetAngle = (newPi/32); 
		perceptionArea = 30; 	//in meter
		positionCache = [la,lb,lorient];
		
		if (ifHttp != nil, {http = ifHttp} , {http = false});
		proxy = incaseofProxy;
				
		if (http == false, {
		//in this mode the only method used is update				
			world = aSoundworld; //the soundscape could be formed by several sound zones or layers.
			zoneAmps = IdentityDictionary.new;
			
			world.soundZones.do({arg soundzone;
				soundzone.addDependant(this);
				zoneAmps.add((soundzone.name).asSymbol -> 1); //init all zone amps = 1
			});
			
			scapeWidth = world.scapeWidth;
			scapeHeight = world.scapeHeight;
			
			soundscapeName = world.getName;
			
			listenerSynths = IdentityDictionary.new;
			
			channelOut = aChannelOut;
			server = world.soundZones[0].server;
			
			positionResponders = List.new; // store OSCnode to release Them		
			
			//listener audio engine
			if (server.serverRunning, {
				
				spatialisationDef = SynthDef(\listenerspatialisation, {arg out = 0, in, amp = 1,  pan = 0, cutFrequency=22000, durLag = 0.1, zoneAmp = 1, durzoneLag = 1;
				
					Out.ar(out, 
						Pan2.ar(
							LPF.ar(
							(
							(In.ar(in, 1) * Ramp.kr(amp,durLag)) * Ramp.kr(zoneAmp,durzoneLag) //perform a smooth change between 2 consecutive values
							), Ramp.kr(cutFrequency,4*durLag))
						, Ramp.kr(pan,durLag)) //also pan could have discrete interaction so a Line control is needed
					);
					
						
					}
				).send(server);
				
				}, {"WARNING, server not running, Listener"+uid+"Synth Def not sended".postln;}
			);
			
			this.openOSCinterface;
	
			},{
			//http API controller, unix based, no windows. 
		
			//in this mode the only method used is update, the synthesis is on the server,
			// this listener is just a copy of the server listener, but allow to controll it from a local machine.	
			this.openOSCinterface;
		
		});
	
	
	}
	
	openOSCinterface {
		
		//OSC position controllers
		
		//METAVERSE API
		positionResponders.add(OSCresponderNode(nil, (soundscapeName++'/spatdif/core/listener/'++uid++'/position').asSymbol, 
		// listen any address in default port
		{ arg time, resp, msg; 
			var x,y, wrong;
		if (msg[1] != nil, {x = msg[1].asFloat; }, {wrong = false;}); 
		if (msg[2] != nil, {y = scapeHeight - msg[2].asFloat; }, {wrong = false;});
		
		if (wrong != false, {this.setPosition(x,y);})
		}).add) ;
		
		//METAVERSE API
		positionResponders.add(OSCresponderNode(nil, (soundscapeName++'/spatdif/core/listener/'++uid++'/direction').asSymbol,
		{ arg time, resp, msg; 
			//msg.postln;
		if (msg[1] != nil, {this.setOrient(msg[1].asFloat); });
		}).add);
		
		

		positionResponders.add(OSCresponderNode(nil, (soundscapeName++'/spatdif/core/listener/'++uid++'/move').asSymbol,
		{ arg time, resp, msg; 
			//msg.postln;
		if (msg[1] == 'gostraight', {this.gostraight()});
		if (msg[1] == 'goback', {this.goback()});
		if (msg[1] == 'turnleft', {this.turnleft()});
		if (msg[1] == 'turnright', {this.turnright()});
		}).add) ;
		
		
		positionResponders.add(OSCresponderNode(nil, (soundscapeName++'/spatdif/core/listener/'++uid++'/getPosition').asSymbol,
		{ arg time, resp, msg; 
		("x"+this.la+"y"+this.lb+"orient"+this.lorient).postln;
		}).add);
		
		//http://redmine.spatdif.org/projects/spatdif/wiki/Core_descriptors
		//http://redmine.spatdif.org/projects/spatdif/wiki/TinyAVE
		
		
	}

	
	gostraight {
	var x = cos(lorient);
	var y = sin(lorient);
	var xnosign = abs(x);
	var ynosign = abs(y);
	var signx = x/xnosign;
	var signy = y/ynosign;
	if (xnosign > 0.5, {x = 1},{x = 0});
	if (ynosign > 0.5, {y = 1},{y = 0});
	if (la < scapeWidth)		
		{if (la > 0)
			{la = la + (signx*x)}
			{la = 1}
			}
		{la = (scapeWidth - 1)};
	if (lb < scapeHeight)
		{if (lb > 0)
			{lb = lb + neg((signy*y))}	//because in SuperCollider GUI view 0,0 is the bottom left corner while this model uses 0,0 in the Upper Left corner
			{lb = 1}
			}
		{lb = (scapeHeight - 1)};
	this.update(this);
	}
	

	goback {
	var x = cos(lorient);
	var y = sin(lorient);
	var xnosign = abs(x);
	var ynosign = abs(y);
	var signx = x/xnosign;
	var signy = y/ynosign;
	if (xnosign > 0.5, {x = 1},{x = 0});
	if (ynosign > 0.5, {y = 1},{y = 0});
	if (la < scapeWidth)		
		{if (la > 0)
			{la = la + neg((signx*x))}
			{la = 1}
			}//the gostraight contrary
		{la = (scapeWidth - 1)};
	if (lb < scapeHeight)
		{if (lb > 0)
			{lb = lb + (signy*y)}
			{lb = 1}
			}
		{lb = (scapeHeight - 1)};
	this.update(this);
	}


	turnleft {
	var compare, angleLimit;
	lorient = lorient + offsetAngle;
	compare = lorient;
	angleLimit = 3*(pi/2);
	if (compare > angleLimit, {lorient = (lorient - 2pi)} );
	//lorient.postln;
	this.update(this); //no angle variation visualisation yet developed
	}

	turnright {
	var compare, limit;
	lorient = lorient - offsetAngle;
	compare = lorient;
	limit = neg((pi)/2); //-pi/2
	if (compare < limit, {lorient = (lorient + 2pi);});
	//lorient.postln;
	this.update(this);
	}
	
	//range [-¹/2,3/2¹]
	setOrient { arg angle;
		
		var wrong = false;
		
		if (angle < (neg((pi)/2)), {wrong = true}) ;
		if (angle > (3*(pi/2)), {wrong = true}) ;
		
		if (wrong == false, {
			lorient = angle;
			this.update(this);	
			},{"error: trying to set Listener orient with incorrect values".postln;}
		);

	
	}
	
	
	setPosition { arg x, y;
		
		var wrong = false;
		
		if (x < 0, {wrong = true}) ;
		if (y < 0, {wrong = true}) ;
		if (x > world.scapeWidth, {wrong = true}) ;
		if (y > world.scapeHeight, {wrong = true}) ;
		
		if (wrong == false, {
					la = x;
					lb = y;
					this.update(this);	
					},{"error: trying to set Listener position with incorrect values".postln;}
				);
		

	}	

	getPosition {
	^[la,lb,lorient];
	}

	
	calculatePanning { arg aXv, aYv, aA, aB, aOrient, anAtm;
	var xv = aXv;				
	var yv = scapeHeight - aYv; //because theoretical spatialisaton model for panning was created with a cartesian 
	var a = aA;	//space: here in Sc y go to scapeHeight (up) to 0 (down). so for a correct pan behaviour we need to change y reference system
	var b = scapeHeight - aB;
	var orient = aOrient;
	var atm = anAtm;
	
	var d = sqrt(squared(a-xv)+squared(b-yv));
	var pan;
	
	if (atm != nil,//vertex is an atmosphere type
		{pan = 0; //panning always 0 for atm	
			},
		{pan = ((sin(orient)*((xv-a)/d)) - (cos(orient)*((yv-b)/d)));
						}
					);
	//"sin".postln; sin(orient).postln; "cos".postln; cos(orient).postln; "d".postln; d.postln;
	//"(xv-a)/d".postln; ((xv-a)/d).postln; "(yv-b)/d".postln; ((yv - b)/d).postln; "pan".postln; pan.postln;

	if (d == 0, {pan = 0});
	^pan; 
	
	}
	

	
//AMPLITUDE BY DISTANCE FORMULA
// loudness in real life approximately changes -6db each times the distance doubles: if at 10m the loudness of source s is 50db, at d = 5m the s Loudness = 44db.
// in supercollider, each times the amp double this simulates a change of +6 db, for example evaluate this: [0.125, 0.25, 0.5, 1, 2].ampdb; 
// offsetListenedArea (maybe better rol) is a roll off to increase the perception of a vertex depending on the same distance. 
// dy a parameter to scale a sound depending on the distance it was recorded.
// The standard Rec_distance is 5m where dy = 1, if Rec_distance = 2.5m dy = 0.5, if Rec_distance = 10, dy = 2

// the Final amp formula would be
//	amp = dy/(dy + (offsetListenedArea *(d - dy)) //dj is managed in MtgModel.sc
// a = dy/(dy + (rol * (d - dy))



	filter { arg aXv, aYv, aA, aB, aOffsetVertexListenedArea, anAtm, aM ;
		var xv = aXv;
		var yv = aYv;
		var a = aA;
		var b = aB;
		var amp = 0; 
		var cutFrequency = 22000;
		var offsetListenedArea = aOffsetVertexListenedArea;	//could be the roll off parameter of OpenAL
		var perceptionArea = this.perceptionArea;
		var atm = anAtm;
		var atm_W, atm_H, d_atm;
		var d = sqrt(squared(a-xv)+squared(b-yv));
		
		//variables for rectangle - point distance, TODO: to be implemeted in a primitive reactangle object 
		var top,bottom, left, right, leftTopP, rightTopP, leftBottomP, rightBottomP, listenerP;
		
		var m = aM;
		
		
		if (offsetListenedArea == nil, {offsetListenedArea = world.offsetVertexStandardListenedArea}); 
		

		 if (atm != nil,
			{
			atm_W = atm[2]; //W
			atm_H = atm[3]; //H
			//define reactangle points
			
			top = yv + (atm_H/2);
			bottom = yv - (atm_H/2);
			left = xv - (atm_W/2);
			right = xv + (atm_W/2);
			
			leftTopP  = Point.new(left,top);
			rightTopP = Point.new(right,top);
			leftBottomP = Point.new(left,bottom);
			rightBottomP = Point.new(right,bottom);
			
			listenerP = Point.new(a,b);
			
			if ( ((a >= left) && (a <= right)),
			
				{if ( ((b >= bottom) && (b <= top)), 
					{d_atm = 0;	
					//("inside atm").postln;
						},
						{ //("or over the top or over the bottom").postln; //listenerP could be both over the top or over the bottom
						if (b > top,
							{d_atm = (b - top);}, //attenuation formula for atmosphere. The distance is considering as the closest point to listenerP.
							{d_atm = (bottom - b);} //so b < bottom
						);
						
						}
					);
				} 				
				,{
				if ( ((b >= bottom) && (b <= top)),
					{
					//("or over the left or over the right").postln;
					
					if (a < left,
						{d_atm = (left - a) ;},
						{d_atm = (a - right);} //else, a is > right 
					);
					},
					{//("outside atm and outside the infinite projection of atm sides").postln; // so now the closest point are the reactangle vertices
					
					if (b > top,
						{ if (a > right,
								{ d_atm = rightTopP.dist(listenerP);},
								{d_atm = leftTopP.dist(listenerP);}  // a is < left
							);
						},
						//else b is < bottom
						{ if (a > right,
								{ d_atm = rightBottomP.dist(listenerP);},
								{d_atm = leftBottomP.dist(listenerP);}  // a is < left
						);} //bottom
					);
					
					}
				);
				}
			);
			
			d = d_atm; 	
//			"d_atm: ".post;
//			d_atm.postln;

			},{
			
			//attenutaion formula for normal events (vertex)
			//point sources also have a lowpassband filter
			
			if(d > (m*perceptionArea*2),
					{
					cutFrequency = exp(10 - ( 7 *((d - (m*perceptionArea*2))/(m*offsetListenedArea + m*perceptionArea))));
					if(cutFrequency < 20, {cutFrequency = 20});			//the max filtered value with a LPB is 20hz in model created
		
						} 
			);	
			
			} 
		);
				
		//attenuation formula
		d = d*m;
		
		if (d < 1, {d = 1}); // d cannot be less then 1
		
		amp = 1/d;
			
		//"amp".post;
		//amp.postln;

		if(amp < 0, {amp = 0});
		^[amp,cutFrequency];
	}
	
	
	writeScore {
			
			scoreTranscription = true;
			startTime = thisThread.seconds;
	
	}

//code for transcription to add in playActivatedSample, but label information is missing. . . 
/*		if (scoreTranscription == true,
					{
					now = thisThread.seconds - startTime;
					("Event:"+label+" - time:"+now+" - duration:"+dur+" - d:"+((sqrt(squared(al-xv)+squared(bl-yv)))/3)+" - pan:"+pan+" - cutFrequency"+cutFrequency).postln;
					
					}
				);*/
	

	playActivatedSample { arg message; //[xv, yv, offsetVertexListenedArea, atm, bufn, b, grouptmp, this.name, m];
				var result, amp, cutFrequency, pan;
				var sy; //sy = synth
				var xv = message[0];
				var yv = message[1];
				var offsetVertexListenedArea = message[2];
				var atm = message[3];
				var bus = message[4];
				var group = message[5];
				var zoneName = message[6];
				var conceptName = message[7];
				var m = message[8];
				var vId = message[9];
				
				var name;
				var zoneAmp;
				
				zoneAmp = zoneAmps[zoneName.asSymbol];
				//("playActivatedSample zoneAmps["+zoneName+"]"+zoneAmps[zoneName.asSymbol]).postln;
				
				result = this.filter(xv, yv, la, lb, offsetVertexListenedArea, atm, m);
				amp = result[0];
	 			cutFrequency = result[1];
	 			
	 			pan = this.calculatePanning(xv, yv, la, lb, lorient, atm); 
	 			
	//("listenerspatialisation, \in"+bus+",\amp,"+amp+",\pan,"+pan+",\cutFrequency,"+cutFrequency+", \out, "+channelOut+",zoneAmp"+zoneAmp).postln;
				sy = Synth.new(\listenerspatialisation, [\in, bus,\amp,amp,\pan,pan,\cutFrequency,cutFrequency, \out, channelOut, \zoneAmp, zoneAmp], group, \addToTail);
				
				name = sy.asString;
				listenerSynths.add(name -> [sy, xv, yv, offsetVertexListenedArea, atm, zoneName, conceptName, m, vId]);
				
				
				SimpleController(sy).put(\n_end, { |synth, what, moreArgs| 
						//remove synth from list of listener active synth 
						listenerSynths.removeAt(name);
						//don't forget to erase the SimpleController
						synth.releaseDependants;
						 });
				NodeWatcher.register(sy);
				
	}
	

	
	modifplay {	
				var amp, cutFrequency, pan, xv, yv, offsetVertexListenedArea, atm, a, b, orient;
				var result, conceptName, m, zoneName, zoneAmp, closedAmbienceFound, tmpzone, recontrol;
				var vId;
				var waiting = false;
				a = la;
				b = lb;
				orient = lorient;
				closedAmbienceFound = false;
				recontrol = false;
				
				
				//closed ambience management_start
				//first control that all the closed Ambiance Area are corretly instanciated. 
				world.soundZones.do({arg zone;
						if (zone.closeAmbient == "waiting", {waiting = true;});
					
					});
				
				if (waiting == false, {

					//TODO how to manage nestled closed ambience?
					//control if closed ambience mute other sound zones
					if (muteRestWorldCache == "outsideSoundscape" or:(muteRestWorldCache == nil), {//nil just initilal value
					
						world.soundZones.do({arg zone;
						
							if (closedAmbienceFound == false, {
						
								if (zone.closeAmbient != nil, { //this soundscape belong to a close ambience
								
									if ( a > zone.closeAmbient[0] and:(a <(zone.closeAmbient[0] + zone.closeAmbient[2])), // if  x < x_listener < x + W
										{
										if ( b > zone.closeAmbient[1] and:(b <(zone.closeAmbient[1] + zone.closeAmbient[3])), // if  y < y_listener < y + H
											{	//listener is inside this close ambient zone
											if (muteRestWorldCache != zone.name,{  //muteRestOfTheWorld just the first time you enter a closedAmbience
													this.muteRestOfTheWorld(true, zone);
													closedAmbienceFound = true;
												//set the gain of other soundscape to 0
												}, {/*else the lister is still in the same zone of previous position interaction*/});
											}, 
											//or leave a closed ambience
											{if (muteRestWorldCache != "outsideSoundscape",{
												this.muteRestOfTheWorld(false, zone);});
											}); 
										},{//or leave a closed ambience
											if (muteRestWorldCache != "outsideSoundscape",{
												this.muteRestOfTheWorld(false, zone);});
										}); 
								 });
							});
						});
					
					
						},{ //else we are already inside a closed ambience
						
						tmpzone = world.soundZonesDict[muteRestWorldCache.asSymbol];																	if ( a > tmpzone.closeAmbient[0] and:(a <(tmpzone.closeAmbient[0] + tmpzone.closeAmbient[2])), // if  x < x_listener < x + W
										{
										if ( b > tmpzone.closeAmbient[1] and:(b <(tmpzone.closeAmbient[1] + tmpzone.closeAmbient[3])), // if  y < y_listener < y + H
											{	
												//listener is still inside this close ambient zone
											}, 
											//or leave a closed ambience
											{if (muteRestWorldCache != "outsideSoundscape",{ //in theory this control is not usefull
												this.muteRestOfTheWorld(false, tmpzone);});
												//now we should control is exing a zone the llistener enter in a new one
												recontrol = true;
											}); 
										},{//or leave a closed ambience
											if (muteRestWorldCache != "outsideSoundscape",{ //in theory this control is not usefull
												this.muteRestOfTheWorld(false, tmpzone);});
												//now we should control is exing a zone the llistener enter in a new one
												recontrol = true;
										});
						
						}
					);
					
					
					//recontrol cause by a listener leaving a closed ambience, maybe enter in a new closed ambience
					if (recontrol == true, {
					
						world.soundZones.do({arg zone;
						
							if (closedAmbienceFound == false, {
						
								if (zone.closeAmbient != nil, { //this soundscape belong to a close ambience
								
									if ( a > zone.closeAmbient[0] and:(a <(zone.closeAmbient[0] + zone.closeAmbient[2])), // if  x < x_listener < x + W
										{
										if ( b > zone.closeAmbient[1] and:(b <(zone.closeAmbient[1] + zone.closeAmbient[3])), // if  y < y_listener < y + H
											{	//listener is inside this close ambient zone
											if (muteRestWorldCache != zone.name,{  //muteRestOfTheWorld just the first time you enter a closedAmbience
													this.muteRestOfTheWorld(true, zone);
													closedAmbienceFound = true;
												//set the gain of other soundscape to 0
												}, {/*else the lister is still in the same zone of previous position interaction*/});
											}, 
											//or leave a closed ambience
											{if (muteRestWorldCache != "outsideSoundscape",{
												this.muteRestOfTheWorld(false, zone);});
											}); 
										},{//or leave a closed ambience
											if (muteRestWorldCache != "outsideSoundscape",{
												this.muteRestOfTheWorld(false, zone);});
										}); 
								 });
							});
						});
					});
					
					//closed ambience management_end
				});
				
				//modify all active sound events. 
				listenerSynths.do({ arg item;
						
						var vertex;
											
						offsetVertexListenedArea = item[3] ;
						
						zoneName = item[5] ;
						conceptName = item[6];
						m = item[7];
						vId = item[8];
						
						vertex = world.soundZonesDict[zoneName.asSymbol].runner.graph.graphDict[vId];
						
						//xv = item[1] ; //like this concept synthesis cannot simulate the mouvement in real time after a user request
						//yv = item[2] ;
						//atm = item[4] ; //like this concept synthesis cannot trasform in RT
						
						xv = vertex[0];
						yv = vertex[1];
						atm = vertex[4][1];

						zoneAmp = zoneAmps[zoneName.asSymbol];
						
						result = this.filter(xv,yv,a,b,offsetVertexListenedArea,atm, m);
						amp = result[0];
						cutFrequency = result[1];
						pan = this.calculatePanning(xv,yv,a,b,orient,atm);
						
						item[0].set(\amp,amp,\pan,pan,\cutFrequency,cutFrequency, \zoneAmp, zoneAmp );
				}); 

	}
	
	conceptMoved { arg movedConcept;
		
				var amp, cutFrequency, pan, xv, yv, offsetVertexListenedArea, atm;
				var result, zoneName, conceptName, m;
				var vId;
				
							
				listenerSynths.do({ arg item;
						
						var vertex;
											
						offsetVertexListenedArea = item[3] ;
						zoneName = item[5] ;
						conceptName = item[6];
						m = item[7];
						vId = item[8];
						
						if (movedConcept.asString == conceptName.asString, {
							
							vertex = world.soundZonesDict[zoneName.asSymbol].runner.graph.graphDict[vId];
							
							xv = vertex[0];
							yv = vertex[1];
							atm = atm = vertex[4][1];
							
							result = this.filter(xv,yv,la,lb,offsetVertexListenedArea,atm, m);
							amp = result[0];
							cutFrequency = result[1];
							pan = this.calculatePanning(xv,yv,la,lb,lorient,atm);
						
							item[0].set(\amp,amp,\pan,pan,\cutFrequency,cutFrequency );
						
						});
				}); 

		
	}
	
	
	
	//TODO? add argument for closed ambience, taking zone.soloAmp value
	muteRestOfTheWorld { arg boo, soundscape;
		var restOfTheWorld;
		
			
		
			restOfTheWorld = List.new;
			world.soundZones.do({arg zone;
				if (zone == soundscape, {}, {restOfTheWorld.add(zone)});
			});
	
			if (boo == true,{
					//set other soundscapes silent
				restOfTheWorld.do({arg anOutsideSoundscape; 
					zoneAmps[(anOutsideSoundscape.name).asSymbol] = 0; //soundscape.soloAmp = Attention, what happed with others closed ambiences that must be 0?
				});
				//and this loud
				zoneAmps[(soundscape.name).asSymbol] = 1;
				
				},{ //false
				restOfTheWorld.do({arg anOutsideSoundscape; 
					if (anOutsideSoundscape.closeAmbient == nil, { zoneAmps[(anOutsideSoundscape.name).asSymbol] = 1; }); //turn on this soundscape just if it is not a closed Ambient
					
				});
				zoneAmps[(soundscape.name).asSymbol] = 0;
			});
			
			if(boo, {muteRestWorldCache = soundscape.name;}, {muteRestWorldCache = "outsideSoundscape"}); 
			//if this listener is in a closedAmbience zone the cache store the zone name. If is not the cache store the "outsideSoundscape" flag.
			
			//("muteRestOfTheWorld"+soundscape.name+"cache =").post;
			//muteRestWorldCache.postln;
	}


		
	remove {
			world.soundZones.do({arg soundzone;
			soundzone.removeDependant(this);
		});
		
		positionResponders.do({arg aOSCNode; aOSCNode.remove;});
		
		listenerSynths.do({arg synthInfoArray;
			synthInfoArray[0].free; //[0] is the synth
		});
	}


	update { arg theChanged, theChanger, more;

		var offset, result, atm, message;
		
		if (theChanged.class == MtgListener or:(more == \init),
			{
				
				more = [\tobefilteredbyposition,la,lb,lorient];				
				("Listener"+uid+"x,y,angle = ").post;
				la.post;
				(", ").post;
				lb.post;
				(", ").post;
				lorient.postln;				
				
				this.changed(\position, more);
				
				if ( http == true, {
					
					if (proxy == nil, {
						("curl -G 'http://mtg110.upf.es:8080/soundscape/position?clientid="++uid++"&x="++la++"&y="++lb++"'").unixCmd;
						("curl -G 'http://mtg110.upf.es:8080/soundscape/rotation?clientid="++uid++"&a="++lorient++"'").unixCmd;
						},{ 	
						("curl -G 'http://mtg110.upf.es:8080/soundscape/position?clientid="++uid++"&x="++la++"&y="++lb++"' -x '"++proxy++"'").unixCmd;
						("curl -G 'http://mtg110.upf.es:8080/soundscape/rotation?clientid="++uid++"&a="++lorient++"' -x '"++proxy++"'").unixCmd;
						});
					
					},{
					this.modifplay();	
					});
				
				
				
				//positionCache = [la,lb,lorient];
			//});
			
			}		
		);
		
		if (theChanged.class == MtgModel,{
			if (more[0] == \samplePlayed,{
					if ( this.init == nil, {this.update(more:\init);});
					this.init = 1;
					message = more[2..];
					this.playActivatedSample(message); //erase \samplePlayed and the MtgModel synth, don't usefull here. add m
					//[xv, yv, offsetVertexListenedArea, atm, bufn, b, grouptmp, concept.name, m, vId];
				});
				
				
			if (more[0] == \conceptMoved, {
				
				this.conceptMoved(more[1]); //conceptName
				
				});	
				
			});
		
		
	}

} 



MtgListenerGUI {
		var <>mtgListener, <>address, <>uid;
		
		*new { arg mtgListener, address, uid;
			^super.new.initMtgListenerGUI(mtgListener,address,uid) 
			
		}

		initMtgListenerGUI { arg aMtgListener, anAddress, aUid;
		var w, c, ystep, xstep, g;	

		
		mtgListener = aMtgListener;
		if (anAddress != nil, {address = NetAddr(anAddress, 57120)},
			{ address = NetAddr("127.0.0.1", 57120)}
		);
		address.postln;
		
		if (aUid == nil, {uid = 1;},{ uid = aUid }); // the unique id of the listener to control
		
		mtgListener.addDependant(this);
		w = Window.new("Listener Position Controller",Rect(800, 80, 270, 120)).front;
		//w.view.background_(Color(0.9, 0.9, 0.9)) ;
		ystep = 30;
		xstep = 250*0.5;
		c = StaticText.new( w, Rect( 0, 0, xstep, ystep )).string_("<-- or a = turn left").stringColor_(Color(0, 0, 0.3)).align_(\center);
		c = StaticText.new( w, Rect( xstep, 0, xstep, ystep )).string_("--> or d = turn right").stringColor_(Color(0, 0, 0.3)).align_(\center);
		c = StaticText.new( w, Rect( 0, ystep*1, xstep, ystep )).string_("^ or w = go straight").stringColor_(Color(0, 0, 0.3)).align_(\center);
		c = StaticText.new( w, Rect( xstep, ystep*1, xstep, ystep )).string_("I or s = go back").stringColor_(Color(0, 0, 0.3)).align_(\center);
		c = StaticText.new( w, Rect( 0, ystep*2, 250, 30 )).string_("    CLICK AND FOCUS ON THIS WINDOWS &  ").stringColor_(Color(1, 0, 0.3));
		c = StaticText.new( w, Rect( 0, ystep*3, 250, 30 )).string_("    PRESS POSITION CONTROL COMMAND     ").stringColor_(Color(1, 0, 0.3));
		c = Slider( w, Rect(0, 0, 0, 0));
		w.view.keyDownAction = { arg view,char,modifiers,unicode,keycode;
				//unicode.postln;
				this.setPosition(unicode);	
				};
		
		w.onClose_({ mtgListener.removeDependant(this) }) ;		
		}

		setPosition { arg aUnicode; 
		var compare;
		//aUnicode.postln;
		compare = aUnicode;		
			//does it work's universally? (in case you can switch in use a, d, w, x instead of arrows)
			
//			TODO try this
			
/*		unicode 16rF700, 			up arrow
		unicode 16rF703, 			right arrow
		unicode 16rF701, 			down arrow
		unicode 16rF702, 			left arrow	*/
			
 			case //{ (compare == 97) }  //a 
			{ (compare == 63234  or:(compare == 97)) } // left arrow
				{
					
				address.sendMsg(("/spatdif/core/listener/"++uid++"/move"), "turnleft");
				if (mtgListener != nil, {mtgListener.turnleft;});
				} 

			//{ ( compare == 100) }	//d
			{ ( compare == 63235 or:(compare == 100)) } // right arrow	
				{
				address.sendMsg(("/spatdif/core/listener/"++uid++"/move"), "turnright"); 
				if (mtgListener != nil, {mtgListener.turnright;});
				
				}

			//{ (compare == 119 } //w
			{ (compare == 63232 or:(compare == 119)) } // up arrow

				{
				address.sendMsg(("/spatdif/core/listener/"++uid++"/move"), "gostraight"); 
				if (mtgListener != nil, {mtgListener.gostraight;});
				}

			//{ (compare == 115 ) } //s
			{ (compare == 63233 or:(compare == 115)) } //down arrow

				{
				address.sendMsg(("/spatdif/core/listener/"++uid++"/move"), "goback"); 
				if (mtgListener != nil, {mtgListener.goback;});
				}

		}

		
}
              