//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.

KMLSoundscape {
	
	var <>soundscapeDict;  //dictionary of generated soundscapes, key: soundscapeName -> [SoundWorld, kmlPath]
	
	*new { 
			^super.new.initKMLSoundscape();
	}
	
	
	initKMLSoundscape {
		
		soundscapeDict = IdentityDictionary.new	
	}

	
	//SOUNDSCAPE KML PARSING
	//API, SC just need to now the kml path

	parse { arg aPath, aSoundscapeDatabasePath;
		var kmlPath;
		var kml;
		var folders;
		var isSoundscape, soundscapeFolder;
		var segmentedDatabase = false;
		var annotatedDatabase = false; //if annotatedDatabase == true, MtgModel creation considering annotated datase
		var zones;
		var world;
		var c,r, mtgmodel;

		{
		kmlPath = PathName(aPath);
		
		kml = DOMDocument.new(kmlPath.fullPath);
		//kml.getNodeValue.postln; //return all the xml structure
		
		 
		//init soundscape
		folders = kml.getElementsByTagName("Folder");
		isSoundscape = nil; //if we don't find any soundscape this variable stay nil
		folders.do({arg fold; var soundscape, soundscapeName, soundscapeW, soundscapeH, anExtendedData, url, id, schemaData, simpleData, value;
			
			
				fold.getChildNodes(false).do({arg aNode; 
					if (aNode.getNodeName == "ExtendedData", {anExtendedData = aNode})
					});
				
				if (anExtendedData != nil, {
				
					schemaData = anExtendedData.getElementsByTagName("SchemaData");
					url = schemaData[0].getAttributeNode("schemaUrl");
					id = url.getNodeValue;
					if (id == "#soundscape_id",{
						
						simpleData = anExtendedData.getElementsByTagName("SimpleData");
						simpleData.do({arg data; var simpleDataName;
										
										simpleDataName = data.getAttribute("name");
										value = data.getText;
								
									case
										{simpleDataName == "name"}
												{ 
												soundscapeName = value;
												}
										
										{simpleDataName == "width"}
												{
												//float
												 soundscapeW = value.asFloat;
												 if (soundscapeW < 0, {"error: incorrect soundscape width value".postln});
												}
										
										{simpleDataName == "height"}
												{
												//float
												soundscapeH = value.asFloat;
												if (soundscapeH < 0, {"error: incorrect soundscape height value".postln});
												}
										
																						{simpleDataName == "type"}
																						
												{
												//bool
												//The paths annotatedDatabase must  be provided in the "zone" Extended Shema "annotatedDatabase" SimpleField 
												if ( value == "annotatedDatabase", {annotatedDatabase = true});
												if ( value == "segmentedDatabase", {segmentedDatabase = true});//
												}
												
								
							});
			
						soundscapeFolder = fold;
						isSoundscape = true;
						
						//MtgSystem Soundscape Init
						world = SoundWorld.new(scapeWidth:soundscapeW, scapeHeight:soundscapeH, name:soundscapeName, kml:true);
						soundscapeDict.add(soundscapeName.asSymbol -> [world, aPath]);
						
						});
					
					if (isSoundscape == true, {
						//all the parsing operation inside this if, else error message if isSoundscape == nil
				
				
						//zone creation
						zones = List.new;
						
						//soundscape tags
						soundscapeFolder.getChildNodes(false).do({arg aNode; var isZone; //each folder conteinded by the soundscape folder is a zone
								isZone = aNode.getNodeName;
								if (isZone == "Folder", {zones.add(aNode)});
							});
							
						
						zones.do({arg zone; 
							var a, b, c, zoneName, placeMarks, zoneExtededData = 0, isZonePlaceMark = false, isClone = false, conceptName;
						
							zoneName = zone.getElementsByTagName("name");
							//"zone name:".postln;
							zoneName = zoneName[0].getText;
							
							//create Zone
							c = Condition.new(false);
							r = OSCresponder(nil, '/synced',
								{|time, resp, msg| 
									msg.post;
									"_server osc respose".postln;
									c.test = true;
									c.signal;} ).add;
							
							mtgmodel = world.createZone(zoneName);
							//create a MtgModel object, it boots the server and performs asyncronous commands. 
							c.wait;
							c.test = false;
							r.remove;				
							
						
							//concept creation
							placeMarks = zone.getElementsByTagName("PlaceMark");
							
							placeMarks.do({arg placeMark; var placeMarkName, point, polygon, rect,extendedData;  
									 var tmpString, polyCoordinates, copy = "", polyPos; //polygon parsing variables		
								placeMarkName = placeMark.getElementsByTagName("name");
								placeMarkName = placeMarkName[0].getText;
								//"concept name: ".postln;
								//placeMarkName.postln;
								
								//process exteded Data
								extendedData = placeMark.getElementsByTagName("ExtendedData");
								
								//this placemark refer to zone or concept?
								extendedData.do({arg anExtendedData; var schemaData, url, id, simpleData;
									
									schemaData = anExtendedData.getElementsByTagName("SchemaData");
									url = schemaData[0].getAttributeNode("schemaUrl");
									id = url.getNodeValue;
									simpleData = anExtendedData.getElementsByTagName("SimpleData");
									
									if (id == "#zone_id", {isZonePlaceMark = true; zoneExtededData = zoneExtededData + 1});
									
									
									if (id == "#concept_id", {
										isZonePlaceMark = false;
										//searching for clone data
										//clone SoundConcept object is created by the father concept, this ExtendedData needs to be processed earlier than others. 
										//clones are placeMark with the same name af a previously defined concept, but with different parameters. clone takes a new pos as arg
										simpleData.do({arg data;
											var dataName, formattedValue, cloneName, father;
											dataName = data.getAttribute("name"); 
											
											if (dataName == "clone", {
											
											//position array, could be 2D or 4D, must be the same type of the father. The father is the concept with  the same name
											//all the parameters and events are copied from the father, just the position changes. 
											
											formattedValue = this.parseArray(data.getText);
											
											if((formattedValue.size == 2)or:(formattedValue.size == 4), {
												//the father concept already exist
												father = world.soundZonesDict[zoneName.asSymbol].getConcept(placeMarkName);
												cloneName = father.clone(formattedValue); 
												//the method return [cloneName,clone]
												cloneName = cloneName[0];

												}, {("error: kml incorrect clone value"+data.getText+"per cancept:"+placeMarkName).postln;
												});
											isClone = true;
											conceptName = cloneName;
											}, {isClone = false;});
										});
									});
								});

								  
								if (isZonePlaceMark == false, { //concept Placemark
									
									
									if ((annotatedDatabase == false)&&(isClone==false), { //"segmentedDatabase"
										
										//clone SoundConcept object is created by the father concept
										conceptName = placeMarkName;
										world.soundZonesDict[zoneName.asSymbol].createConcept(conceptName);
										
										
										//clone cannot specify different ExtendedData from the father
										//process conceptExtentedData
										extendedData.do({arg anExtendedData;
											this.conceptExtendedDataParse(anExtendedData, zoneName, conceptName, world);
										});		
										
										},{
											//"annotatedDatabase" creates concepts. 
										}
										
										);

									},{  //"zone Placemark".postln; //manage zone
										
										extendedData.do({arg anExtendedData;
											this.zoneExtendedDataParse(anExtendedData, zoneName, world);
										});

									}); 
									
								



/*//all this to process Point and Polygon PlaceMark tag, but as they are in longitude and latidute, the system uses the extended data geometry with meter relative reference system																				point = placeMark.getElementsByTagName("Point");
								//"concept coordinates: ".postln;
								if (point[0] != nil, {
										b = point[0].getElementsByTagName("coordinates");
										b.do({arg coordinates;
											var i, t;
											coordinates.getChildNodes(false).do({arg coordinate; 
												i = coordinate.getText; //String
												i = i.split($,);
												("x ="+i[0]+"y ="+i[1]+"z ="+i[2]).postln;
											})
										});
									},{
										polygon = placeMark.getElementsByTagName("Polygon");
										
										//the audio sysnthesis model support just rect
										if (polygon[0] != nil, {
											
											polyCoordinates = placeMark.getElementsByTagName("coordinates");
											tmpString = polyCoordinates[0].getText; //String
												tmpString.do({arg ch; 
													if(ch.ascii == 32, {copy = copy ++ "&"},{ 
														if(ch.ascii == 10,
															 {copy = copy ++ "/"; "some Line Feed symbol".postln;}, //LF new line, line feed
															 {copy = copy ++ ch;}
													 		);
														});
													});
											
											polyPos = copy.split($&);
											polyPos.do({arg i; i = i.split($,);("x = "++i[0]+"y = "++i[1]+"z = "++i[2]).postln;});
											
											//rect = ???; [x,y,W,H] x,y upper left corner
											
											case
											{isZonePlaceMark == true}
												{world.soundZonesDict[zoneName.asSymbol].setGeometry(rect)}
																							{isRanGenPlaceMark == true}
												{world.soundZonesDict[zoneName.asSymbol].getConcept(placeMarkName).setRandomPosition(rect);}											
											{isRanGenPlaceMark == nil&&(isZonePlaceMark == nil)}
												{world.soundZonesDict[zoneName.asSymbol].getConcept(placeMarkName).setnonPointSource(rect)};
											
											}, {("no coordinates information for concept"+placeMarkName).postln;}
										);
						
									
									});
//all this to process Point and Polygon PlaceMark tag, but as they are in longitude and latidute, the system uses the extended data geometry with meter relative reference system										
*/

 								});
								//concept creation_end
								if (zoneExtededData > 1, {"error: it can exist just one zone extented data per folder".postln;});
							});
							//zone creation_end
							//soundscape creation_end
							isSoundscape = false;
			
						}, { /*this is not a soundscape folder, shold be a zone folder*/ });
					},{ /*this folder doesn't contain any extensionData*/ });
			});
		//if any soundscape forlder was found
		
		world.annotationRead();  
		
		if (isSoundscape  == nil, {"this KML document don't have soundscape information in a correct format"});

		1.wait;
		
		if (aSoundscapeDatabasePath == nil, {world.loadDatabase();},{
			
			world.loadDatabase(aSoundscapeDatabasePath.asString);
		});
			
			
		0.5.wait;
		world.generateGraphs;
		0.5.wait;
		world.startAllDephasingClones;
		0.5.wait;
		world.soundscapeController = SoundscapeController.new(world);
		

		}.fork
		^world;		
				
	}
	
	
	zoneExtendedDataParse { arg anExtendedData, aZoneName, aWorld; 
		var url, id, schemaData, value, simpleData, simpleDataName;
		var c, r, mtgmodel; //condition, oscresponder
		var world, zone;
		
		schemaData = anExtendedData.getElementsByTagName("SchemaData");
		url = schemaData[0].getAttributeNode("schemaUrl");
		id = url.getNodeValue;
		world = aWorld;
		zone = world.soundZonesDict[aZoneName.asSymbol];
		
		case
		{ id == "#zone_id"}
	 				{ 
		 				//process zone extension data
						simpleData = anExtendedData.getElementsByTagName("SimpleData");
						simpleData.do({arg data;
								var formattedValue;
								
								simpleDataName = data.getAttribute("name");
								value = data.getText;
								
								case
								{simpleDataName == "zoneGeometry"}
										{
										//manage geometry in meter reference system
										
											formattedValue = this.parseArray(value);
											if (formattedValue.size == 4,{
												zone.setGeometry(formattedValue)},{("error: kml incorrect"+simpleDataName+"value per:"+aZoneName+"").postln;}
											 );
											
										
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;

										}
								
								{simpleDataName == "closedAmbient"}
										{
										//boolean,  1 = true
										
										if (value.asFloat == 1, {zone.setCloseAmbient(zone.getGeometry);})
										
										}
								
								{simpleDataName == "gain"}
										{
										//float, [from 0 to 32]
										//float. 0.125 = -18dB, 0.25 = -12dB, 0.5 = -6dB, 1 = 0db, 2 = +6dB
										
										//more than 32 start to be dangerous for ears. 
										if ((value.asFloat > 0)&&(value.asFloat <=32),
										
											{zone.setGain(value.asFloat);},
											
											{("error: kml incorrect"+simpleDataName+"value per:"+aZoneName+"").postln;}
											);
										
										
										}
										
								{simpleDataName == "virtualUnitMeterRatio"}
										{
										//float 0.25 = 1 virtual unit = 4 meter,
										// also allow to amplify or de-amplify the attenuation by distance listener-sources.
										//Each zone could have different scale = different virtualUnitMeterRatio
										
										
																						if ((value.asFloat > 0),
										
											{zone.setVirtualUnitMeterRatio(value.asFloat);},
											
											{("error: kml incorrect"+simpleDataName+"value per:"+aZoneName+"").postln;}
											);
										
										
										}	
								
								{simpleDataName == "annotatedDatabase"}
										{
										//string The URL of the zone database annotated using the MTG composition practices
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
																						c = Condition.new(false);
										r = OSCresponder(nil, '/synced',
											{|time, resp, msg| 
												msg.post;
												"_server osc respose".postln;
												c.test = true;
												c.signal;} ).add;
												
											//value is it a correct path
											//2 asynchronous commads
											zone.loadAnnotatedDatabase(value);
											c.wait;
											c.test = false;
											("MtgSystem.loadAnnotatedDatabase of zone: '"++aZoneName++"' Analysis Recordings Loaded").postln;
											c.wait;
											c.test = false;
											("MtgSystem.loadAnnotatedDatabase of zone: '"++aZoneName++"' server Buffers loaded from annotation").postln; 
											r.remove;										
										}														
							});					
					 } 	 

		^simpleDataName;
		}

	
	
	
	conceptExtendedDataParse { arg anExtendedData, aZoneName, aConceptName, aWorld; 
		var url, id, schemaData, value, simpleData, simpleDataName;
		var c, r, mtgmodel; //condition, oscresponder
		var world, concept;
		
		schemaData = anExtendedData.getElementsByTagName("SchemaData");
		url = schemaData[0].getAttributeNode("schemaUrl");
		id = url.getNodeValue;
		world = aWorld;
		
		concept = world.soundZonesDict[aZoneName.asSymbol].getConcept(aConceptName);
		
		if (concept == nil, {"error: KMLsoundscape.conceptExtendedDataParse concept = nil".postln});
		
		case 	 
		 { id == "#concept_id"}
	 				{ 
		 				simpleData = anExtendedData.getElementsByTagName("SimpleData");
		 				//aConceptName.postln;
						simpleData.do({arg data;
								var formattedValue; 
								
								simpleDataName = data.getAttribute("name");
								value = data.getText;
								
								case
								{simpleDataName == "conceptGeometry"}
										{
										//trasform kml list <value>entry,entry,entry</value> in
										//Point source 2D Array [x,y] 
										//Area source 4D Array [x,y,Width, Height] 										
										
										formattedValue = this.parseArray(value);

										if(formattedValue.size == 2, {
											
											concept.setPosition(formattedValue);
											
											}, {
											if(formattedValue.size == 4,{
												concept.setnonPointSource(formattedValue);
												}, {("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;});
											
											});
										
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										}
								
								
								{simpleDataName == "gain"}
										{
										//float. 0.125 = -18dB, 0.25 = -12dB, 0.5 = -6dB, 1 = 0db, 2 = +6dB
										//[0,32] 
										if ((value.asFloat > 0)&&(value.asFloat <=32),
										
											{concept.setGain(value.asFloat);},
											
											{("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;}
											);
										
										
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										}
										
								{simpleDataName == "psRandomGeneration"}
										{
										//Array <value>entry,entry,entry,entry</value> could be
										//Area source 4D Array x,y,Width, Height
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
										formattedValue = this.parseArray(value);
												

										if(formattedValue.size == 4, {
											
											concept.setRandomPosition(formattedValue);
											
											}, {
												("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;}
										);
											
										
										}
								
								{simpleDataName == "continuous"}
										{
										//1 = true, 0 = false means concept is Impulsive
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										if (value.asInteger == 1, {
											
												concept.setContinuous;
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										
										}
										
								{simpleDataName == "multipleGenerativePath"}
										{
										//int [1, 50]
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
										formattedValue = value.asInteger;
										
										if ((formattedValue > 1)&&(formattedValue < 50), {
											
												concept.setMultiplePath(formattedValue);
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										}	
										
																				{simpleDataName == "ar"}
										{
										//int
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
										formattedValue = value.asInteger;
										
										if (formattedValue > 0, {
											
												concept.setAr(formattedValue);
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										}
										
										
																				{simpleDataName == "listenedArea"}
										{
										//float
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
										formattedValue = value.asFloat;
										
										if (formattedValue > 0, {
											
												concept.setListenedArea(formattedValue);
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										}
								
								{simpleDataName == "probability"}
										{
										//float [0,1]
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
										
										formattedValue = value.asFloat;
																						if ((formattedValue >= 0)&&(formattedValue <= 1), {
											
												concept.setProb(formattedValue, asFreq:true);
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										}			
								
																				{simpleDataName == "recordingDistance"}
										{
										//float
										//("Extended Data found. Simple Data"+simpleDataName+"has value"+value).postln;
																						formattedValue = value.asFloat;
																						if (formattedValue > 0, {
											
												concept.setEventsRecordingDistance(formattedValue);
											
											},{
											("error: kml incorrect"+simpleDataName+"value"+value+"per cancept:"+aConceptName+"").postln;
											});
										
										
										}														

								
												
							});	
												
					 };

		}
	
	
	
	parseArray {arg aValue;
		
		var list, array, nospaceValue;
		
		nospaceValue = aValue.split($,);
			
		list = List.new; 
		
		nospaceValue.do({arg entry; 
			var cleanvalue;
			cleanvalue = entry.split($ ).reject({arg i ; i=="" ; }); //skip spaces, just in case...
			list.add(cleanvalue[0].asFloat);
			});
		
		
		array = list.asArray;
		^array;
		}
	
}

