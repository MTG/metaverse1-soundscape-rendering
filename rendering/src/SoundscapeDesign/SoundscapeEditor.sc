MV1_Editor{
	var models, gui;
	classvar <kMp3Player = "cvlc %";
	classvar <kHtmlBrowser = "open %";
	classvar <instance;
	var <>latestKmlPath;

	*new{
		if (instance.isNil) {instance = super.new.init}
	  	^instance;
	}

	init{
	  latestKmlPath = PathName(this.class.filenameSymbol.asString).pathOnly;
	  gui = MV1_EditorGUI.new;	
	  Server.default.waitForBoot({
	  	SynthDef(\mv1_player_1,{|b,t|
			Out.ar(0,Pan2.ar(EnvGen.ar(Env.new([1, 0], t,\step),doneAction:2)*DiskIn.ar(1, b)))
		}).send(Server.default);

	  	SynthDef(\mv1_player_2,{|b,t,n|
			Out.ar(0,EnvGen.ar(Env.new([1, 0], t,\step),doneAction:2)*DiskIn.ar(2, b))
			//Out.ar(0,DiskIn.ar(2, b))
		}).send(Server.default)


	  });
	}
}

MV1_EditorController{
	var <>view;
	var conceptEditor;
	var <>currentFreesounds;
	var cmd, lastPid, <>synth;
	var <currentConcept, currentEvent;
	var <databasePath;
	var <kmlPath, <xmlPath;
	
	*new{|view|
	   ^super.newCopyArgs(view);
	}
	
	initDatabase{
		databasePath.makeDir;
		view.soundscape.concepts.do({|c|
			(databasePath++"/"++c.name).makeDir;
		});
		
	}
	
	
	loadKML{|x|
		var model;
		
		kmlPath = x.path.fullPath++"/"++x.filename;	
		MV1_Editor.instance.latestKmlPath =  x.path.fullPath;
		xmlPath = kmlPath[0..kmlPath.size-5]++".xml";
		databasePath = kmlPath[0..kmlPath.size-5];
		File.exists(xmlPath).not.if({xmlPath=nil});
		view.soundscape.load(kmlPath,xmlPath);
		File.exists(databasePath).not.if({this.initDatabase});
		this.selectConcept(nil);
	}
	editConcept{|v|
		view.concepts.value.postln;
		view.soundscape.concepts[view.concepts.value].postln;
		conceptEditor = MV1_ConceptEditor.new(view.soundscape.concepts[view.concepts.value]);
	}

	selectConcept{|v|
		currentConcept =view.soundscape.concepts[view.concepts.value];
		view.searchBox.string_(currentConcept.name);
		view.events.items = 
			currentConcept.concept.events.collectAs(
			{|e| e.url.split($/).last}, Array);		
	}

	selectEvent{|v|
		currentEvent = view.events.items[view.events.value];
		this.displaySelectedEvent;
	}

	editSound{
		var evt, rate;
		evt = currentConcept.concept.events.as(Array)[view.events.value];
		rate = view.soundView.soundfile.sampleRate;
		evt.start=view.soundView.selections[0][0]/rate.asFloat;
		evt.end=(view.soundView.selections[0][0]+view.soundView.selections[0][1])/rate.asFloat;
	}

	soundscapeChanged{|model|
		model.concepts.do({|c| c.extendedData.postln});
		view.concepts.items_(model.concepts.collect({|item|item.name}));
	}
	freesoundSearch{|w|
		var result, filenames;
		this.currentFreesounds = FS2Sound.search((q:w))[\sounds]
			.select({|s|".wav$".matchRegexp(s[\original_filename])});		
		filenames = currentFreesounds.collect({|s| s[\original_filename]});
		view.freeSounds.items_(filenames);
	}
	previewMp3{|v|				
		var pipe;
		if (lastPid.notNil){	
			var result = "kill -9 %".format(lastPid).systemCmd;
			"-pid-".postln;
			lastPid.postln;
			"-result-".postln;
			result.postln;
			};
		this.currentFreesounds[v.value]['preview-lq-mp3'].postln;
		cmd = MV1_Editor.kMp3Player.format(this.currentFreesounds[v.value]['preview-lq-mp3']);		
		cmd.unixCmd;//{lastPid = nil};
		pipe = Pipe.new(
			"ps ax | grep '%' | grep -v grep |grep -v 'sh -c' |cut -f 2 -d ' '".format(cmd),"r");	
		lastPid = pipe.getLine;
		pipe.close;
	}
	previewFreeSound{|v|
		var cmd = MV1_Editor.kHtmlBrowser.format(this.currentFreesounds[view.freeSounds.value][\url]);
		cmd.unixCmd;

	}
	importSound{|v|
		var snd,dirName,sndFile;
		
		snd = currentFreesounds[view.freeSounds.value].as(FS2Sound);
		dirName = databasePath++"/"++currentConcept.name;
		view.status.string_("downloading from freesound");
		snd.retrieve(dirName,{
			var evt,end;
			var fileName = dirName ++"/"++snd[\original_filename];
			"sox -c 1 -r 44100 '%' /tmp/tmp.wav".format(fileName).systemCmd;
			"mv /tmp/tmp.wav '%'".format(fileName).systemCmd;
			view.status.string_("loading");
			snd[\duration].postln;
			snd[\samplerate].postln;
			end = snd[\duration]*44100;
			evt = MV1_Event.new(snd[\original_filename],0,end,44100,0,1);
			currentConcept.concept.addEvent(evt);
			view.events.items = 
			currentConcept.concept.events.collectAs(
			{|e| e.url.split($/).last}, Array);
			
			//view.events.items=view.events.items.add(snd[\original_filename]);
			//view.events.value = view.events.items.size -1;
			this.selectEvent(nil);
		});
	}
	loadSelection{
		var evt = currentConcept.concept.events.as(Array)[view.events.value];		
		view.soundView.setSelectionStart(0,evt.start*view.soundView.soundfile.sampleRate);
		view.soundView.setSelectionSize(0,(evt.end-evt.start)*view.soundView.soundfile.sampleRate);
		
	}
	displaySelectedEvent{		
		var conceptName, eventName, fileName, sndFile;
		conceptName = view.soundscape.concepts[view.concepts.value].name;
		eventName = view.events.items[view.events.value];
		fileName=this.databasePath++"/"++conceptName++"/"++eventName;
		fileName.postln;
		sndFile =  SoundFile.new;
		sndFile.openRead(fileName);
		view.soundView.soundfile = sndFile;
		view.soundView.readWithTask(0, sndFile.numFrames,doneAction:this.loadSelection);
		view.status.string_("");
		//view.soundView.readFileWithTask(sndFile,0,-1, showProgress:false);
		
	}
	playSelection{|v|
		var def,snd,dur,sel,buf;
		if(v.value==1){
			var sel,dur,snd,buf,def;
			sel = view.soundView.selection(0);		
			dur = (sel[1]-sel[0]) / view.soundView.soundfile.sampleRate;
			snd = view.soundView.soundfile;
			buf = Buffer.cueSoundFile(Server.default, snd.path,sel[0],snd.numChannels);
			def = ("mv1_player_"++snd.numChannels).asSymbol;
			this.synth = Synth(def,[\b,buf.bufnum,\t,dur,\n,snd.numChannels]).play(Server.default);
		}
		{
			"stopping".postln;
			this.synth.free;
		}

	}
	save{
		xmlPath.isNil.if({
			xmlPath = kmlPath[0..kmlPath.size-5]++".xml";
		});
		view.soundscape.save(this.kmlPath,this.xmlPath);
	}
}

MV1_ConceptEditor{
	var concept;
	var window, title, gain, geom, rand, multiple, prob, ar, dist, area, clone;

	
	*new{|placemark|
	   ^super.newCopyArgs(placemark.concept).init;
	}

	makeCloneGUI{	
		title = StaticText( window, Rect( 120, 3, 80, 50 )).string_("(cloned)");
		//StaticText.new(window,Rect(20, 80, 100, 25)).string_("Geometry");
		//NumberBox.new(window,Rect(20, 110, 50, 25)).value_(concept.clone.left).action_{|v| concept.clone.left = v.value};
		//NumberBox.new(window,Rect(80, 110, 50, 25)).value_(concept.clone.top).action_{|v| concept.clone.top=v.value};
		//NumberBox.new(window,Rect(140, 110, 50, 25)).value_(concept.clone.width).action_{|v| concept.clone.width=v.value};
		//NumberBox.new(window,Rect(200, 110, 50, 25)).value_(concept.clone.height).action_{|v| concept.clone.height=v.value};

	}
	makeGUI{
		var geom;
		geom = concept.conceptGeometry?concept.psRandomGeneration;	
		geom.postln;		
		StaticText.new(window,Rect(20, 50, 100, 25)).string_("Gain");
		NumberBox.new(window,Rect(150, 50, 46, 25)).value_(concept.gain).action_{|v| v.value.postln;concept.gain=v.value};
		//StaticText.new(window,Rect(20, 80, 100, 25)).string_("Geometry");		
		//PopUpMenu.new(window,Rect(150, 80, 100, 25))
		//	.items_(["Static","Random"])
		//	.value_(concept.psRandomGeneration.isNil.if({0},{1}))
		//	.action_{|v| if(v.value==0)
		//		{concept.conceptGeometry=geom;concept.psRandomGeneration=nil}
		//		{concept.conceptGeometry=nil;concept.psRandomGeneration=geom}
		//	 };
		//NumberBox.new(window,Rect(20, 110, 50, 25)).value_(geom.left).action_{|v| geom.left = v.value};
		//NumberBox.new(window,Rect(80, 110, 50, 25)).value_(geom.top).action_{|v| geom.top=v.value};
		//NumberBox.new(window,Rect(140, 110, 50, 25)).value_(geom.width).action_{|v| geom.width=v.value};
		//NumberBox.new(window,Rect(200, 110, 50, 25)).value_(geom.height).action_{|v| geom.height=v.value};
		// temporarily remove 70 pixels of commented fields from y 
		Button.new(window,Rect(20, 150-70, 100, 25))
			.states_([ [ "Continuous", Color(0.0, 0.0, 0.0, 1.0), Color(1.0, 0.0, 0.0, 1.0) ], [
				      "Periodic", Color(1.0, 1.0, 1.0, 1.0), Color(0.0, 0.0, 1.0, 1.0) ] ])
			.value_(concept.continuous.if({0},{1}))
			.action_{|v| concept.continuous = v.value ==0 };
		StaticText.new(window,Rect(20, 195-70, 100, 25))
			.string_("Mul. Gen. Path");
		NumberBox.new(window,Rect(150, 195-70, 100, 25))
			.value_(concept.multipleGenerativePath)
			.action_{|v| concept.multipleGenerativePath=v.value };
		StaticText.new(window,Rect(20, 225-70, 100, 25))
			.string_("Frequency");
		NumberBox.new(window,Rect(150, 225-70, 100, 25))
			.value_(concept.probability)
			.action_{|v| concept.probability=v.value};
		StaticText.new(window,Rect(20, 255-70, 100, 25))
			.string_("Arhythmic");
		NumberBox.new(window,Rect(150, 255-70, 100, 25))
			.value_(concept.ar)
			.action_{|v| concept.ar=v.value};
		StaticText.new(window,Rect(20, 285-70, 100, 25))
			.string_("Recording Dist.");
		//NumberBox.new(window,Rect(150, 285-70, 100, 25))
		//	.value_(concept.recordingDistance.clip(0.0001,1.0))
		//	.action_{|v| concept.recordingDistance=v.value};
		StaticText.new(window,Rect(20, 315-70, 100, 25))
			.string_("Listened area");
		NumberBox.new(window,Rect(150, 315-70, 100, 25))
			.value_(concept.listenedArea.clip(0.0001,1.0))
			.action_{|v| concept.listenedArea=v.value};
	}

	init{	
		window =  Window.new("",Rect(389, 389, 270, 450-70)).front;
		title = StaticText( window, Rect( 20, 3, 200, 50 )).string_( concept.name );
		title.font = Font( "SansSerif", 20 );
		concept.clone.isNil.if({this.makeGUI},{this.makeCloneGUI});
	}
}


MV1_EditorGUI{
	var <window,title,<status, <freeSounds, <soundView;
	var <>soundscape, <>concepts, <events;
	var loadButton, importButton, openButton, editConceptButton, previewButton, saveButton,playButton;	
	var controller, simpleController;
	var <searchBox, searchButton;
	var fgColor,bgColor;

	*new{
	   ^super.new.init;
	}

	init{
		fgColor = Color.black;
		bgColor = Color(0.5,0.5,0.7);
		Freesound2.api_key_("048ca11bd6d343de9377d232ac67ee83");
		soundscape = MV1_Soundscape.new;
		controller = MV1_EditorController.new(this);
		simpleController = SimpleController.new(soundscape);
		simpleController.put(\changed,{|...args| controller.soundscapeChanged(*args)});				
		this.makeGUI;		
	}

	loadKML{
		var window, view;
		window = Window("Load KML", Rect(this.window.bounds.left+20, 400, 400, 245)).front;
		PathName(this.class.filenameSymbol.asString).pathOnly.postln;
		view = FileListView(window,Rect(0,0,230,180), MV1_Editor.instance.latestKmlPath,
			dblClickAction:{|v| controller.loadKML(v);});
	}
	makeGUI{
		GUI.swing;	
	    window = Window.new("",Rect(389, 389, 713, 538)).front;
	    title = StaticText( window, Rect( 10, 3, 340, 50 )).string_( "Soundscape Editor" );
	    title.font = Font( "Lucida Grande", 20 );
	    loadButton = Button.new(window,Rect(250, 15, 75, 30));
	    loadButton.states_([["Load KML", fgColor, bgColor ]]);
	    loadButton.action = {this.loadKML};
	    concepts = ListView.new(window,Rect(10, 60, 120, 370)).action_{|v| controller.selectConcept(v)};
		editConceptButton = Button.new(window,Rect(10, 440, 75, 30));
		editConceptButton.states = ([["edit",fgColor, bgColor]]);
		editConceptButton.action = {|v| controller.editConcept};
	     soundView = SoundFileView.new(window,Rect(140, 60, 465, 120)).action_{|v| };
		soundView.gridOn = false;
		soundView.waveColors = [ bgColor,bgColor];
		soundView.mouseUpAction_({controller.editSound});
		playButton = Button.new(window,Rect(140, 200, 75, 30));
		playButton.states_([["play",fgColor, bgColor ], ["stop",fgColor, bgColor ]]);
		playButton.action_({|v| controller.playSelection(v)});
	    events = ListView.new(window,Rect(140, 240, 220, 190)).action_{|v| controller.selectEvent(v) };
	    freeSounds = ListView.new(window,Rect(400, 240, 220, 180)).action_{|v| controller.previewMp3(v)};
	    importButton = Button.new(window,Rect(365, 280, 30, 30));
	    importButton.states_([["<< ", fgColor, bgColor]]);
		importButton.action_{|v| controller.importSound(v)};
		searchBox = TextField.new(window,Rect(400,200,100, 30));
		searchButton = Button.new(window,Rect(540, 200, 75, 30));
		searchButton.states = ([["search",fgColor, bgColor ]]);
		searchButton.action = {controller.freesoundSearch(searchBox.value)};

		previewButton = Button.new(window,Rect(400, 440, 75, 30));
		previewButton.states_([["Preview", fgColor, bgColor ]]);
		previewButton.action_{controller.previewFreeSound};
		saveButton=Button.new(window,Rect(540, 440, 75, 30));
		saveButton.states_([["Save", fgColor, bgColor ]]);
		saveButton.action_({controller.save});
		status = StaticText( window, Rect( 10, 480, 340, 50 )).string_( "" );
	}
}
