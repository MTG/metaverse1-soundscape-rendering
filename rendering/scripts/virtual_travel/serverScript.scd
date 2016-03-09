
(

// Darkice dynamic server name creation and streaming chain connection settings

if(thisProcess.platform.name.postln==\linux){
        var jackServer = thisProcess.argv[0];
        var jackOutputs = [];
        jackServer.postln;
        o = ServerOptions.new;
        o.device=jackServer++":sc-metaverse";
        Server.internal.options=o;
        
        4.do { |i|
        		2.do { |c|
        			// Format: darkice-metaverse1-<CLIENTID>:left,darkice-metaverse1-<CLIENTID>:right
	    			jackOutputs = jackOutputs.add("darkice-metaverse1-" ++ i ++ ":" ++ ["left", "right"][c]);
        		}
        };
	   
	   setenv("SC_JACK_DEFAULT_OUTPUTS", jackOutputs.join($,));
};




n = SoundscapeGenerator.new;

/*~soundscapeDatabase = //??; 
~annotation = //??; 
~type = //??; */

~soundscapeDatabase = "/home/metaverse/metaverse/supercollider/scripts/soundscape-streaming/sounds/virtualtravel_database/"; 
~annotation = "/home/metaverse/metaverse/supercollider/scripts/virtual_travel/VirtualIsland.scd"; 
~type = "scd"; 


n.addSoundscape(~soundscapeDatabase,~annotation ,~type);
)

/*
( //works!
~kml = "/Users/mattia/Desktop/SuperColliderWorkShop/Metaverse/Seq_Spat_2audioEngines/kml/virtualTravelBeach.kml";
~soundscapeDatabase = "/Users/mattia/Library/Application Support/SuperCollider/soundscapecache/Canary Island Virtual Travel/";

n.addSoundscape(~soundscapeDatabase, ~kml,"kml");

n.addSoundscape("/Users/mattia/Documents/Soundscape/MetaverseUseCase/","/Users/mattia/Desktop/SuperColliderWorkShop/Metaverse/Seq_Spat_2audioEngines/MtgSystem_VirtualIsland.scd" ,"scd");
)

n.kmlSoundscapeDict[("Canary Island Virtual Travel").asSymbol].soundscapeName

n.kmlSoundscapeDict

*/