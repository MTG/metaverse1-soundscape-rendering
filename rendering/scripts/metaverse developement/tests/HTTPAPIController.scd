
(
l = MtgListener.new(uid:0,http:true, proxy:"http://proxy.upf.edu:8080");
l.scapeWidth = 256;
l.scapeHeight = 256;

MtgListenerGUI.new(uid:0); // this allow to control the listener 0, thus the stream http://mtg110.upf.es:8000/island-0 
)

l.setPosition(118,203); //museum inside
l.setPosition(100, 202); //museum outside
l.setPosition(80, 131); //museum outside near beach
l.setPosition(150, 150); //square
l.setPosition(180, 156)  //funtain

l.setPosition(80,125); //beach
l.setPosition(82, 132); //near bingo
l.setPosition(75,76); //near bar 
//in street, near people, runner-by, roller-by
l.setPosition(70, 66);



({ //latency detector, try from museum to square
t = thisThread.seconds;
l.setPosition(150, 150); //square
}.fork;)
//when you heard the sound changes, evaluate this:
(thisThread.seconds - t ).postln; //latency 15 sec???



