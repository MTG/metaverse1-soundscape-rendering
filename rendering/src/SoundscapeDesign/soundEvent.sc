//MTG <Music Technology group> www.mtg.upf.edu
//UPF <Universitat Pompeu Fabra>
//Design and development by Mattia Schirosa
//Published under GPLv3 http://www.gnu.org/licenses/gpl.html
//Documentation and resources at http://mtg.upf.edu/technologies/soundscapes
//NOTE: This application uses the GeoGraphy and XML quarks.

SoundEvent { 
//WARNING -> never use the variable "class" as parameter of a SC class because object use it for print...
//and if is nil than each time you try to print rase an error -> gaverClass is ok. 

	var <>name, <>gaverClass, <>pos, <>dy, <> rolloff, <>prob, <> viD, <>sampleDur, <>samplePath, <> sampleStartFrame, <> bufnum, <> listenedArea, <>normalisation;
	
	*new{ arg name, gaverClass, pos, dy, rolloff, prob, sampleDur, samplePath, sampleStartFrame, normalisation;
		^super.new.initSoundEvent(name, gaverClass, pos, dy, rolloff, prob, sampleDur, samplePath, sampleStartFrame, normalisation);
	}
	
	initSoundEvent{ arg  aName, aClass, aPos, aDy, aRoll, aProb, aSampleDur, aSamplePath, aSampleStartFrame, aNormalisation ;
		name = aName;
		//("name"+name).postln;
		gaverClass = aClass; //a Gaver taxonomy basic event class
		pos = aPos;
		dy = aDy;		
		rolloff = aRoll;
		prob = aProb;
		sampleDur = aSampleDur;
		//("dur"+sampleDur).postln;
		samplePath = aSamplePath;
		sampleStartFrame = aSampleStartFrame.asInteger;
		normalisation = aNormalisation;
		//("sampleStartFrame"+sampleStartFrame).postln;
		
		//("aName, aClass, aPos, aDy, aRoll, aProb"+name+gaverClass+pos+dy+rolloff+prob).postln;
	}
	
	
	info {
		("name"+name+"pos"+pos+"dy"+dy+"viD"+viD+"sampleDur"+sampleDur+"path"+samplePath+"startframe"+sampleStartFrame+"bufnum"+bufnum).postln;
	
	}

}
