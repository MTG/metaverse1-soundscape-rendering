//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.



SoundWorld { 
		
		var <>soundZones;
		var <>soundZonesDict;
		var <>offsetVertexStandardListenedArea;
		var <>scapeWidth, <>scapeHeight;
		var <>listenerDict;
		var <>soundscapeName;
		var <>kml;
		var <>graph, <>runner;
		var <>cache, <>soundscapeDatabase, annotationRead, databaseLoaded;
		var <>soundscapeController;
		
		*new { arg zoneArray, scapeWidth, scapeHeight, name, kml ;
			^super.new.initSoundWorld(zoneArray, scapeWidth, scapeHeight, name, kml);
		}
		
		initSoundWorld { arg aSoundscape, aScapeWidth, aScapeHeight, aName, isKml ;
			
			soundZones = List.new;
			scapeWidth = aScapeWidth ;
			scapeHeight = aScapeHeight;
			
			listenerDict = IdentityDictionary.new;
			soundZonesDict = IdentityDictionary.new;
			
			if (aSoundscape != nil &&(aSoundscape.class == Array), {this.initZoneArray(aSoundscape) },
				 {if (aSoundscape != nil, {"SoundWorld.new requires an Array of MtgModels".postln;}); });
			if (aName != nil, {soundscapeName = aName});
			
			kml = isKml;
				
			//kml mode, no SC application, the soundscape is creaded inside a SoundWorld instance 
			//GeoGraphy Quark
			if (kml == true, {	
				graph = Graph.new ;
				runner = Runner.new(graph);	
			});
			
			annotationRead = false;
			databaseLoaded = false;
			
			cache = PathName.new(Platform.userAppSupportDir) +/+ PathName.new("soundscapecache");
					
			//OSC interface for Multi-Listener clients. uid is mapped to channels: uid = i; channels = [(2*i), (2*i + 1)] 
			//the first uid must be 0 so that channels out [0,1] then uid 1 channels [2,3] ...
			OSCresponderNode(nil, (soundscapeName++"/"++'add/entity/listener').asSymbol, // listen any adress in default port
				{ arg time, resp, msg; 
					var uid = msg[1];
					//msg[1].postln; uid and channel * 2
					this.addListener(uid);
	
			}).add ;
			
			
			OSCresponderNode(nil,(soundscapeName++"/"++'remove/entity/listener').asSymbol, // listen any adress in default port
				{ arg time, resp, msg; 
					var uid;
					//msg.postln;
					uid = msg[1];
					this.removeListener(uid);
	
			}).add ;
			
		}
		
		setName{ arg aSoundscapeName;
			
			if (aSoundscapeName.class == String,{soundscapeName = aSoundscapeName},{"need a string as arg".postln;});
			
		}
		
		getName {
			
			if (this.soundscapeName == nil, {"error: calling SoundWorld.getName with SoundWorld.soundscapeName == nil!!!".postln;})
			^this.soundscapeName;
			
			
		}
		
		
		setWidth { arg aW;
			
			if (aW < 0,  {this.scapeWidth = aW},{"error: SoundWorld.setWidth incorrect arg".postln;});
			
		}
	
		setHeight { arg aH;
			
			if (aH < 0,  {this.scapeHeight = aH},{"error: SoundWorld.setHeight incorrect arg".postln;});
			
		}
		
		createZone {arg zoneName;
			var mtgmodel;
			
			mtgmodel = MtgModel.new(this.runner,name:zoneName).initAudio;
			this.initZone(mtgmodel);
			
			^mtgmodel;
		}
		
		
		initZoneArray { arg zoneArray;
		
			zoneArray.do({arg z; 
				soundZones.add(z);
				soundZonesDict.add(z.name.asSymbol -> z);
				z.setWorld(this);		
				
				//implementation detail, init data a mtgModel object
				if (soundZones.size == 1, {
					offsetVertexStandardListenedArea = soundZones[0].offsetVertexStandardListenedArea;
					//set graph annotation size: the n of parameter stored in geoGraphy implementation detail
					soundZones[0].runner.graph.setAnnotationSize(4);
					 });
			});		
			
		}
		
		initZone { arg z;
				
			soundZones.add(z);
			soundZonesDict.add(z.name.asSymbol -> z);	
			z.setWorld(this);
						
			//implementation detail, init data a mtgModel object
			if (soundZones.size == 1, {
				offsetVertexStandardListenedArea = soundZones[0].offsetVertexStandardListenedArea;
				//set graph annotation size: the n of parameter stored in geoGraphy implementation detail
				soundZones[0].runner.graph.setAnnotationSize(4);
				 });
			
		}
		
		
		
		
		addListener { arg id;
			var uid;
		uid = id.asInteger;
		
		if (listenerDict[uid] != nil, {
		
			"addListener bad request".postln;
		
			}, {
			listenerDict.add(uid -> MtgListener.new(0, 0, this, uid.asInteger, uid.asInteger * 2));  //0, 0 are the Listener Initial position
			("listenerDict:"+listenerDict).postln;
			});
		
		
		}
		
		removeListener { arg id;
			
			var key;
			
			key = id.asSymbol;
			
			listenerDict[id].postln;
			
			listenerDict[id].remove;
			listenerDict.removeAt(id);
		
		
		}
		
		removeAllListener {
			
			listenerDict.do({arg listener;
					var id;
					id = listenerDict.findKeyForValue(listener);
					listenerDict[id].remove;
					listenerDict.removeAt(id);
				})
		
		}
		
		
		annotationRead {
			annotationRead = true;	
		}
		
		databaseLoaded {
			databaseLoaded = true;
		}
		
		
		
		//soundscape Database format: requires a folder path of the soundscape containing the xml annotation & a list of folder (the concept) containing a list of events in turn.
		// The concept name coincide with the folder name.
		// The user can pass SoundscapeDatabasePath as arg of the parse method, if not, the method searches for a folder named as the soundscape name in the cache. 
		
		loadDatabase { arg soundscapeDatabaseUserPath;
			//if the path is not provided as arg:
			//Inside the cache search for a folder named as the soundscape. Inside the soundscape folder search for a unique xml file. Read and parse the soundscapedatabase.xml annotation
			
			// Then for each zone, takes all the concept, search for the concept folder inside the soundscape folder.
			// If found read the <soundConcept> Tag on the xml, takes all the <event> tag, read the attributes:
			// URL = file name (stefan must conserve the file name),  start, end, recDistance, normalisation.
			
			
			//takes all the concept folder audio file entries and creates a segmented event for each annotation. 	
			var f, folderName, tmp, conceptFolders;
			var databaseAnnotationXML, files, continue;
			var soundscapeTag, conceptTagsDict;
			var tmptags;
			var found = 0;
			
			
			conceptTagsDict = IdentityDictionary.new;
			
			
			if (soundscapeDatabaseUserPath == nil, { 
			
					f = cache.folders;
					
					if ((f != nil)&&(soundscapeName!=nil), {
						//attention, in linux folder.fileNames return the container folder, while in mac return this folder. 
						f.do({arg folder; 
							
							tmp = folder.allFolders;
							folderName = tmp[(tmp.size -1)];
							
							if (folderName == this.soundscapeName, {
									soundscapeDatabase = PathName.new(folder.fullPath)
								});
							});
					});	

					},{
						
					soundscapeDatabase = PathName.new(soundscapeDatabaseUserPath.asString);
			});
									
			if(soundscapeDatabase != nil, {
				
				
			//search xmlDatabaseAnnotation
				
				files = soundscapeDatabase.files;
				files.do({arg file;
					if((file.extension == "xml")or:(file.extension == "XML"), {
						
						databaseAnnotationXML = file.fullPath; //save the current path
						found = found + 1;
						});
					
					});
				found.postln;
				case 
					{found == 0}
						{("error: SoundWorld.loadDatabase didn't find any xml annotation in soundscape folder"+soundscapeDatabase).postln;}
					{found > 1}
						{("error: SoundWorld.loadDatabase found several xml annotations in soundscape folder"+soundscapeDatabase).postln;}
					{found == 1}
						{	//parse xml
							databaseAnnotationXML = DOMDocument.new(databaseAnnotationXML); //now create the DOM doc
							
							soundscapeTag = databaseAnnotationXML.getElementsByTagName("soundscapeDatabase"); //return Array
							
							tmptags = soundscapeTag[0].getElementsByTagName("soundConcept");
							
							tmptags.do({arg concTag;
									var n;
									n = concTag.getElementsByTagName("name");
									
									conceptTagsDict.add(n[0].getText.asSymbol -> concTag);
									
								});
							
							continue = true;
						};

					
				if(continue == true, {
					//create events, store segmentation and URL on it
					
					//search for concept Folders inside soundscape database
					conceptFolders = soundscapeDatabase.folders; 
					
					soundZones.do({arg zone;
						
						zone.conceptDict.do({arg concept;
							var cName;
							cName = concept.getName;
							
							conceptFolders.do({arg samplefold; 
								var fn, ftmp;
								ftmp = samplefold.allFolders;
								fn = ftmp[(ftmp.size -1)]; //ConceptFolderName
								if (fn == cName, {
									//fn.postln;
									//samplefold.files.postln;
									
									//create event per this concept
									samplefold.files.do({arg sample;
										
										var tmpFile, url, dur, start, end, recDist, norm, segmentTags, fileName;
										var urlAsPathName, segmentName;
										
										tmpFile = SoundFile.new;
											
										if(tmpFile.openRead(sample.fullPath) != false, { //.openRead just read the header
											
											fileName = sample.fileName;
											
											//the number of events is the number of <event> tags (segmentation) on the xml annotation, not the number of file
											segmentTags = conceptTagsDict[cName.asSymbol].getElementsByTagName("event");
											segmentTags.do({arg segment;
											
url = segment.getAttribute("URL");
urlAsPathName = PathName.new(url);
segmentName = urlAsPathName.fileName;

if (segmentName.asString == fileName.asString, {
	
	start = segment.getAttribute("start");
	end = segment.getAttribute("end");
	recDist = segment.getAttribute("recDistance");
	norm = segment.getAttribute("normalisation");
	
	if (start != nil, {start = start.asFloat},{("error: not correct start value in segmentation of file"+url).postln;}); //frame
	if (end != nil, {end = end.asFloat},{("error: not correct end value in segmentation of file"+url).postln;}); //frame
	if (recDist != nil, {recDist = recDist.asFloat;});
	if (norm != nil, {norm = norm.asFloat;});
	if (end<start, {("error: not correct value in segmentation of file"+url).postln;});
	dur = ((end-start)/tmpFile.sampleRate);
	
	tmpFile.close;
	
	//each add the normalisation parameter
	zone.getConcept(cName).add(sampleDurE:dur, samplePathE:sample.fullPath, sampleStartFrameE:start, rollE:recDist, normalisationE:norm);
	zone.addEventToTimeAnalysisRec(dur);
},{});								
												
												});



											
											},{ ("the file "+sample.fullPath+" is not a sound File").postln;
											});
										
										
										});
									
									});
								
								})
							
							});
						
						});
					this.databaseLoaded();

					
					});
				
			},{ "soundscapeDatabase is nil in SoundWorld.loadDatabase".postln;
			if(soundscapeName == nil,
				{"soundscapeName is nil".postln;},
				{("the general soundscape cache:"+cache.fullPath+"doesn't exist or do not contain any soundscape folder").postln;}
				);
			
			});
				
		}
		
		
		generateGraphs {
		
			if (annotationRead == true, {
				
				if (databaseLoaded == true, {
						soundZones.do({arg zone;
								zone.generateGraphs(graph);
							});
					},{"cannot generateSoundscapeGraphs because database still not processed".postln});
				
				},{"cannot generateSoundscapeGraphs because annotation still not processed".postln});
		
		}
		
/*		
						//for each zone, take the concept, search for the associated folder and create events 
				if (soundscapeDatabase != nil, {
					

					
					});*/
		
		
		
		
		//start all considering clones, ask to wait for clone activation in order to avoid combo effect cause by
		// the same samples played at the same time in different positions.
		// 0.1 is not enought, 1 second is.
		
		
		startZone { arg zoneName;
			var zone, runner;
			
			zone = this.soundZonesDict[zoneName.asSymbol];
			runner = zone.runner;
			
			zone.conceptDict.do({arg conc;
								
								if (conc.cloneNumber == nil, {
									
									conc.actantIdDict.do({ arg actid;
											runner.start(actid);
										});
								},{
									1.wait;
									//"clone".postln;
									conc.actantIdDict.do({ arg actid;
									
										runner.start(actid);
										0.001.wait;
									
									});
							
							})
						
				
					
					})
			
			
		}
		
		startAllDephasingClones { arg insideRoutine;
			
			if (insideRoutine == true, { //don't need the fork in this case. 
				
				this.soundZones.do({ arg zone;
						var runner;
						runner = zone.runner;
						zone.conceptDict.do({arg conc;
								if (conc.cloneNumber == nil, {
									
									conc.actantIdDict.do({ arg actid;
											runner.start(actid);
										});
								},{
									1.wait;
									//"clone".postln;
									conc.actantIdDict.do({ arg actid;
									
										runner.start(actid);
										0.001.wait;
									
									});
							
							})
						
				
					
					})
				});
				
				
				},{ //else we need fork
			
				{
					
				this.soundZones.do({ arg zone;
						var runner;
						runner = zone.runner;
						zone.conceptDict.do({arg conc;
								
								if (conc.cloneNumber == nil, {
									
									conc.actantIdDict.do({ arg actid;
											runner.start(actid);
										});
								},{
									1.wait;
									//"clone".postln;
									conc.actantIdDict.do({ arg actid;
									
										runner.start(actid);
										0.001.wait;
									
									});
							
							})
						
				
					
					})
				});
				}.fork
			
			});
			
		}
		
}
