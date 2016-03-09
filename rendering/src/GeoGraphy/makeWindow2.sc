/*
 // A window server that can be referred top and used later

// work with GUI
GUI.current ;

s.makeWindow2 ; // default

// set a step
s.makeWindow2(step: 40) ;

// width and height are independent from step
s.makeWindow2(step: 15, width:350) ;
s.makeWindow2(step: 22, width:350, height:200) ;

// adding something
GUI.textView.new(s.window, Rect(4, s.window.bounds.height*0.5, s.window.bounds.width-8, s.window.bounds.height*0.5-4))


*/



+ Server {
	makeWindow2 { arg step = 25, left = 10, top = 10, width, height, bound = 4, label = "localhost server" ;
		var active, booter, killer, makeDefault, running, booting, stopped, bundling;
		var recorder, scoper;
		var countsViews, ctlr;
		var dumping=false, gui;
		
		var otherStep = ((step*16-(7*bound))/6).asInteger ;

		var w ;
		
		if (width.isNil, { width = 16*step+(bound*6) }) ;
		if (height.isNil, { height = step+(bound*4)+(step*0.6*2) }) ;
		
		window = nil ; // reset Server.window var
		
		gui = GUI.current;
		
			w = window = gui.window.new( label,
						Rect(left, top, width, height),
						resizable: true );
						
		// This messes all up...so keep out				
		//	w.view.decorator = FlowLayout(w.view.bounds);
		
		// We use local (also: no internal in swing)
		if(isLocal,{
			booter = gui.button.new(w, Rect(bound, bound, step*3, step));
			booter.states = [["Boot", Color.black, Color.clear],
						   ["Quit", Color.black, Color.clear]];
			
			booter.action = { arg view; 
				if(view.value == 1, {
					booting.value;
					this.boot;
				});
				if(view.value == 0,{
					this.quit;
				});
			};
			booter.setProperty(\value,serverRunning.binaryValue);
			
			// hard-coded positions: but using a  module (here: 25)
			killer = gui.button.new(w, Rect(step*3+(bound*2), bound, step*2, step));
			killer.states = [["K", Color.black, Color.clear]];
			
			killer.action = { Server.killAll };	
		});
		
		active = gui.staticText.new(w, Rect(step*5+(bound*3),bound, step*5, step));
		active.string = this.name.asString;
		active.align = \center;
		active.font = gui.font.new( gui.font.defaultSansFace, 16 ).boldVariant;
		active.background = Color.black;
		if(serverRunning,running,stopped);		

		makeDefault = gui.button.new(w, Rect(step*10+(bound*4),bound, step*3, step));
		makeDefault.states = [["default", Color.black, Color.clear]];
		makeDefault.action = {
			thisProcess.interpreter.s = this;
			Server.default = this;
		};

		//w.view.decorator.nextLine;
		
		recorder = gui.button.new(w, Rect(step*13+(bound*5), bound, step*3, step));
		recorder.states = [
			["prepare rec", Color.black, Color.clear],
			["record >", Color.red, Color.gray(0.1)],
			["stop []", Color.black, Color.red]
		];
		recorder.action = {
			if (recorder.value == 1) {
				this.prepareForRecord;
			}{
				if (recorder.value == 2) { this.record } { this.stopRecording };
			};
		};
		recorder.enabled = false;
/*		
	// Maybe we need bindings for other stuff in the window: 
	// e.g. textField: so let's comment out 
	 
		w.view.keyDownAction = { arg ascii, char;
			var startDump, stopDump, stillRunning;
			
			case 
			{char === $n} { this.queryAllNodes(false) }
			{char === $N} { this.queryAllNodes(true) }
			{char === $ } { if(serverRunning.not) { this.boot } }
			{char === $s and: { gui.stethoscope.isValidServer( this )}} { GUI.use( gui, { this.scope })}
			{char == $d} {
				if(this.isLocal or: { this.inProcess }) {
					stillRunning = {
						SystemClock.sched(0.2, { this.stopAliveThread });
					};
					startDump = { 
						this.dumpOSC(1);
						this.stopAliveThread;
						dumping = true;
						w.name = "dumping osc: " ++ name.asString;
						CmdPeriod.add(stillRunning);
					};
					stopDump = {
						this.dumpOSC(0);
						this.startAliveThread;
						dumping = false;
						w.name = label;
						CmdPeriod.remove(stillRunning);
					};
					if(dumping, stopDump, startDump)
				} {
					"cannot dump a remote server's messages".inform
				}
			
			};
		};
*/		
		if (isLocal, {
			
			running = {
				active.stringColor_(Color.red);
				booter.setProperty(\value,1);
				recorder.enabled = true;
			};
			stopped = {
				active.stringColor_(Color.grey(0.3));
				booter.setProperty(\value,0);
				recorder.setProperty(\value,0);
				recorder.enabled = false;

			};
			booting = {
				active.stringColor_(Color.yellow(0.9));
				//booter.setProperty(\value,0);
			};
			bundling = {
				active.stringColor_(Color.new255(237, 157, 196));
				booter.setProperty(\value,1);
				recorder.enabled = false;
			};
			
			w.onClose = {
				window = nil;
				ctlr.remove;
			};
		},{	
			running = {
				active.stringColor = Color.red;
				active.background = Color.red;
				recorder.enabled = true;
			};
			stopped = {
				active.stringColor = Color.red;
				active.background = Color.black;
				recorder.setProperty(\value,0);
				recorder.enabled = false;

			};
			booting = {
				active.stringColor = Color.red;
				active.background = Color.yellow;
			};
			
			bundling = {
				active.stringColor = Color.new255(237, 157, 196);
				active.background = Color.red(0.5);
				booter.setProperty(\value,1);
				recorder.enabled = false;
			};
			
			w.onClose = {
				// but do not remove other responders
				this.stopAliveThread;
				ctlr.remove;
			};
		});
		if(serverRunning,running,stopped);
			
		//w.view.decorator.nextLine;


		countsViews = 
		#[
			"Avg CPU :", "Peak CPU :", 
			"UGens :", "Synths :", "Groups :", "SynthDefs :"
		].collect({ arg name, i;
			var label,numView, pctView;
			// skip % signs and use a horizontal layout
			label = gui.staticText.new(w, Rect(i*(otherStep+bound), bound*2+step, otherStep, step*0.6));
			label.string = name;
			label.align = \right;
		
				numView = gui.staticText.new(w,  Rect(i*(otherStep+bound), bound*3+step+(step*0.6), otherStep, step*0.6));
				numView.string = "?";
				numView.align = \right;
			
			
			numView
		});
		
		w.front;

		ctlr = SimpleController(this)
			.put(\serverRunning, {	if(serverRunning,running,stopped) })
			.put(\counts,{
				countsViews.at(0).string = avgCPU.round(0.1);
				countsViews.at(1).string = peakCPU.round(0.1);
				countsViews.at(2).string = numUGens;
				countsViews.at(3).string = numSynths;
				countsViews.at(4).string = numGroups;
				countsViews.at(5).string = numSynthDefs;
			})
			.put(\cmdPeriod,{
				recorder.setProperty(\value,0);
			})
			.put(\bundling, bundling);	
			
		this.startAliveThread;
	}
}

