//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.

SoundscapeGenerator {
	classvar <currentSoundscapePath; //to be used inside the .scd annotation
	var <>kmlSoundscapeDict, <>scdSoundscapeDict;  //dictionary of generated soundscapes, key: soundscapeName -> [SoundWorld, kmlPath]
	
	*new { 
			^super.new.initSoundscapeGenerator();
	}
	
	//create a soundscape starting from a .scd sound design annotation 
	initSoundscapeGenerator { 
		
		kmlSoundscapeDict = IdentityDictionary.new;
		scdSoundscapeDict = IdentityDictionary.new;
	}	
		
	//types are: ("kml", "scd")	
	addSoundscape {arg database, annotation, type;
			var newSoundscape, k, s;	
		case
			{type == "kml"}
			
				{	
					{
						k = KMLSoundscape.new;
						//the kml annotation, the path of the soundscape folder containing XMLdatabase & the concept forlders 
						newSoundscape = k.parse(annotation, database); 
						1.wait;
						k.soundscapeDict.do({arg array; newSoundscape = array[0]});

						kmlSoundscapeDict.add(newSoundscape.soundscapeName.asSymbol -> newSoundscape);
						}.fork
					}
		 	{type == "scd"}
			
				{	//WARNING: in order to store the soundscape the last evaluated line must be the soundscape = SoundWorld.sc object
					currentSoundscapePath = database;
					newSoundscape = thisProcess.interpreter.executeFile(annotation);
					("here"+newSoundscape).postln;
					scdSoundscapeDict.add(newSoundscape.soundscapeName.asSymbol -> newSoundscape);
					}
		
		//TODO store soundscape
		//soundscapeDict = IdentityDictionary.new 	
		//kmlSoundscapeDict.add(newSoundscape.name.asSymbol -> newSoundscape);
	}

	
	//SOUNDSCAPE KML PARSING
	//API, SC just need to now the kml path

	
}

