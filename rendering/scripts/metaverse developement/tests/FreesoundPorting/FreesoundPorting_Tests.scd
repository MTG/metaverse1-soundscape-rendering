//after UPF changes proxy set up you need to set the global variable from your terminal:
// export http_proxy="http://proxy.upf.edu:8080" . anyway now works curl -o from terminal
// but not from sc. So just specify each time the proxy with curl -x

/*search:

try notation f=tag:!bass tag:drum  does it works????
combined filter -> f=tag:bass+tag:drum //search sound that have tag both bass and drum
q=bass -drum // search for sound bass (name tag etc) that are not drum

+ is for combined filter
f=tag_dog&f=is_remix_false&f=was_remixed_false is not combined but added
*/


/*Interesting is the issue of how to search on the database in order to retrieve pure events. For exemple dogs we obtains a lot of potential unrelated materials like dogs hunting, and atmpsphere with just one dog bank...  */

/*
USE THE FILTER TO IMPROVE THE SEARCH:
duration f=duration:[0.1 TO 3]
sort more donwloaded s downloads_desc   -> WARNING tabasco doesn't have the n of downloaded copy because huge table difficult to tranfer
//seems brams will upload the tabasco copy of freesound database and also include this feature...hopefully

s rating_desc or avg_rating:[3 TO *]
f=is_remix:false
f=was_remixed:false

search on tag instead of query

f=tag:"dog"

also provide in the GUI the definition of concept and nuances:

concept = tag nuance = description
f=tag:bass description:"really heavy"
*/
/*
Interesting aspect is to define the concept the user could insert and expand this concept (adding similar/related terms like dog -> barks ) in order to assure good query resoults. This is a huge work.
*/
/*
open a url on computer browser from terminal
mac: open url
linux: firefox url
windows: firefox.exe url 
*/
//querying Freesound


PathName.tmp //return tmp folder cross platform !!!!! cool!!!!
~cache = "/Users/mattia/Library/Caches/SuperCollider/soundscapeComposition/"; //define your cache
~proxy = "http://proxy.upf.edu:8080";
(
{
~visualize = false; //open all the retrieved sound on web browser. 

//the first event search
//http://tabasco.upf.edu/api/sounds/search?q=dog&p=3&s=downloads_desc
//good: 4 
//unrelated one between each other: 18
//bad: 4

//~query = "p=3&q=dogs&s=downloads_desc"; //the same of first
//~query = "p=1&q=dogs&s=downloads_desc"; // s=downloads_desc don't do nothing in tabasco right now
//~query = "q=dog"; <num_results>603</num_results>
//~query = "f=tag:dog"; //<num_results>281</num_results>
//~query = "f=tag:dog+is_remix:false+was_remixed:false"; //<num_results>268</num_results>
~query = "f=tag:dog+is_remix:false+was_remixed:false+duration:[0 TO 60]"; //




//WARNING: without page return just hte first page
~queryName = ~query.tr($&, $_).tr($:, $_);

("curl -o "++~cache++~queryName++" 'http://tabasco.upf.edu/api/sounds/search?"++~query++"&api_key=ed72f88446254f92a66946c7d67ebcda&format=xml' -x "++~proxy).unixCmd;
("curl -o "++~cache++~queryName++" 'http://tabasco.upf.edu/api/sounds/search?"++~query++"&api_key=ed72f88446254f92a66946c7d67ebcda&format=xml' -x "++~proxy).postln;

//analysing query

//this was the first query: 
//a = DOMDocument("/Users/mattia/Desktop/SuperColliderWorkShop/Metaverse/FreesoundPorting/search_q=dog&api_key=ed72f88446254f92a66946c7d67ebcda");
2.wait;
a = DOMDocument(""++~cache++""++~queryName); //the first query


a.format.postln;

/*
a.getNodeName;
b = a.getDocumentElement;
b.getNodeName;
c = b.getChildNodes(false);
*/

//c.do({arg element; element.getNodeName.postln;});

/*
num_results
sounds
num_pages
next
*/


/*s = a.getElementsByTagName("sounds");
~ss = s[0].getChildNodes(false);
r = List.new; //list of serve, URL to download file
d = IdentityDictionary.new;

//store all the serve (sample download API) URLs in r 
~ss.do({arg sample; var serve, type, url; 

	type = sample.getElementsByTagName("type");
	if (type[0].getText == "mp3", {}, //discard mp3
		{
		serve = sample.getElementsByTagName("serve");
		r.add([serve[0].getText, type[0].getText]);
		if( ~visualize == true, {
				url = sample.getElementsByTagName("url");
				url = url[0].getText;
				("open "++url).unixCmd;
				url.postln;
			});
		
		}
	);
});
*/

}.fork;
);


//analysis process option

~ss.do({arg element; element.getNodeName.postln;}); //the sounds!!!!!!!

//print name
~ss.do({arg sample; var original_filename; 
	original_filename = sample.getElementsByTagName("original_filename");
	original_filename[0].getText.postln;	
});


~visualize = true; //open all the retrieved sound on web browser. 

//store all the serve (sample download API) URLs in r 
~ss.do({arg sample; var serve, type, url; 

	type = sample.getElementsByTagName("type");
	if (type[0].getText == "mp3", {}, //discard mp3
		{
		serve = sample.getElementsByTagName("serve");
		r.add([serve[0].getText, type[0].getText]);
		if( ~visualize == true, {
				url = sample.getElementsByTagName("url");
				url = url[0].getText;
				("open "++url).unixCmd;
				url.postln;
			});
		
		}
	);
});



//store all the serve (sample download API) URLs in d using the file name as key
~ss.do({arg sample; var serve, type, original_filename; 
	
	original_filename = sample.getElementsByTagName("original_filename");

	type = sample.getElementsByTagName("type");
	if (type[0].getText == "mp3", {}, //discard mp3
		{
			serve = sample.getElementsByTagName("serve");
		d.add(original_filename[0].getText.asSymbol -> serve[0].getText)}
	);
});


i = 0;
//download sound and name it as concept number of instance
r.do({arg arrayServeFormat; //arrayServeFormat[0] = serve arrayServeFormat[1] = format
("curl -o "++~cache++"dog"++i++"."++arrayServeFormat[1]++" '"++arrayServeFormat[0]++"?api_key=ed72f88446254f92a66946c7d67ebcda' -x "++~proxy).unixCmd;
i = (i + 1); 
});



//curl RESULT = 7 means unable to conncet with host, seems a proxy setting issues. 

//download sound and name it as the original file name -> the system don't want space!
d.do({arg serve;
("curl -o "++~cache++""++d.findKeyForValue(serve)++" '"++serve++"?api_key=ed72f88446254f92a66946c7d67ebcda' -x "++~proxy).unixCmd;
});




r.do({arg serve;
serve.postln;
});



~ss.do({arg element; var ix;
ix = element.getChildNodes(false);
r.add(ix);
});

r.do({arg i; i.do({arg element; element.getNodeName.postln;});})

"curl -O 'http://tabasco.upf.edu/api/sounds/82814/serve?api_key=ed72f88446254f92a66946c7d67ebcda'".unixCmd;

//YES it works how can you tell curl to always use this proxy? NO
("curl -o /Users/mattia/Library/Caches/SuperCollider/soundscapeComposition/dog1 'http://tabasco.upf.edu/api/sounds/83156/serve?api_key=ed72f88446254f92a66946c7d67ebcda' -x "++~proxy).unixCmd;


"ls".unixCmd;



//HOW CREATE A TMP FOLDER FOR SOUNDS?????

//curl -o outfilename
//curl -o dog1

("cd "++Platform.userExtensionDir).unixCmd;
("cd "++Platform.systemExtensionDir).postln;

"sudo cd /home/recerca/share/SuperCollider/Extensions".unixCmd;


"cd tmp/".unixCmd
"mkdir gnu".unixCmd;

"ls".unixCmd;
"mkdir "
q = "dog";


//"mkdir tmp".unixCmd;
//"cd tmp".unixCmd;
//("curl -o "++serve++" '"++serve++"?api_key=ed72f88446254f92a66946c7d67ebcda'").unixCmd;
//("curl -O '"++serve++"?api_key=ed72f88446254f92a66946c7d67ebcda'").postln;










//QUERY BY TAG



(
~cache = "/Users/mattia/Library/Caches/SuperCollider/soundscapeComposition/";
~proxy = 'http://proxy.upf.edu:8080';

"curl -o /Users/mattia/Library/Caches/SuperCollider/soundscapeComposition/ 'http://tabasco.upf.edu/api/sounds/search?p=3&q=dogs&s=downloads_desc&api_key=ed72f88446254f92a66946c7d67ebcda'".unixCmd


a = DOMDocument("/Users/mattia/Desktop/SuperColliderWorkShop/Metaverse/FreesoundPorting/search_q=dog&api_key=ed72f88446254f92a66946c7d67ebcda&format=xml");

s = a.getElementsByTagName("sounds");

~ss = s[0].getChildNodes(false);
r = List.new; //list of serve, URL to download file
d = IdentityDictionary.new;
)
