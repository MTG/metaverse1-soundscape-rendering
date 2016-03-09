Freesound2{

	classvar <base_uri 		  		  = "http://www.freesound.org/api";
	classvar <uri_sounds              = "/sounds/";
	classvar <uri_sounds_search       = "/sounds/search/";
	classvar <uri_sound               = "/sounds/%";
	classvar <uri_sound_similar       = "/sounds/%/similar/";
	classvar <uri_users               = "/people/";
	classvar <uri_user                = "/people/%";
	classvar <uri_user_sounds         = "/people/%/sounds/";
	classvar <uri_user_packs          = "/people/%/packs/";
	classvar <uri_packs               = "/packs/";
	classvar <uri_pack                = "/packs/%/";
	classvar <uri_pack_sounds         = "/packs/%/sounds/";
	classvar <>api_key;

	*uri{|uri,args|
		^(Freesound2.base_uri++uri.format(args));
	}
	
	*toIdentityDict{|node|	
		var newNode;
		node.class.switch(
			Event,{
				var dict = IdentityDictionary.new;
				node.keysValuesDo({|k,v|					
					dict.put(k.asSymbol,[Event,Array].includes(v.class).if(
						{Freesound2.toIdentityDict(v)},{v})
					)
				});
				newNode = dict;
			},
			Array,{
				newNode = node.collect({|item| 
					[Event,Array].includes(item.class).if(
						{Freesound2.toIdentityDict(item)},{item}
					)
				})
			}
		);
		^newNode;
	}
	*parseJSON{|jsonStr|
		var parsed = jsonStr;	
		var a,x;
		jsonStr.do({|char,pos|
			var inString = false;
			char.switch( 				
				$",{(jsonStr[pos-1]==$\ && inString).not.if({inString = inString.not})}, 
				${,{ if(inString.not){parsed[pos] = $(} },
				$},{ if(inString.not){parsed[pos] = $)} }				
			)
		});
		^Freesound2.toIdentityDict(parsed.interpret);
	}
}

FS2Req{
	*get{|uri,params|
		var paramsArray,paramsString,cmd, result,response;		
		if (params.isNil,{params = IdentityDictionary.new});
		params.put(\api_key,Freesound2.api_key);
		paramsArray=params.keys(Array).collect({|k|k.asString++"="++params[k].asString.urlEncode});
		paramsString=paramsArray.join("&");		
		cmd = "curl '"++uri++"?"++paramsString+"'";
		result = cmd.unixCmdGetStdOut.replace("\n","");
		response = Freesound2.parseJSON(result);
		^response;		
	}
	*retrieve{|uri,path,doneAction| //assuming no params for retrieve uris
		var cmd;
		uri = uri++"?api_key="++Freesound2.api_key;
		cmd = "curl %>'%'".format(uri,path);
		cmd.unixCmd(doneAction);		
	}
}

FS2Obj : Dictionary{}

FS2Sound : FS2Obj{
	*get_sound{|soundId| ^FS2Sound.newFrom(FS2Req.get(Freesound2.uri(Freesound2.uri_sound,soundId)))}
	*search{|... params| ^FS2Req.get(Freesound2.uri(Freesound2.uri_sounds_search),*params)}
	retrieve{|path, doneAction|
		FS2Req.retrieve(this[\serve],path++"/"++this[\original_filename],doneAction);
	}
}

+String{
	urlEncode{
		^this.collect({|c| if(c.isAlphaNum){c}{"%"++c.ascii.asHexString(2)}})
	}
}
