
(
var xRange, xoffset, yRange, yoffset;
var xList, yList;
var pause = 1;
var step;
var big, small;

//square
/*
xRange = 30;
xoffset = 150; //150 - 180
yRange = 120;
yoffset = 80; // 80 - 200
*/
//square

//beach

xRange = 40;
xoffset = 80; 
yRange = 40;
yoffset = 120;

//beach

m = NetAddr("127.0.0.1", 57120); //Local Machine


//x
xList = List.new;
xRange.do({arg item; xList.add(item + xoffset);});
//c = xList.reverse;
//c.removeAt(0);
//c.pop;
xList.addAll(xList.reverse); 

//y
yList = List.new;
yRange.do({arg item; yList.add(item + yoffset);});
yList.addAll(yList.reverse); 


if (xRange > yRange,{ 
		step = (xRange / yRange); //must be an integer please...as you can choose...
		("is it step:"+step+"a Integer?").postln;
		big = \x;
		small = \y;
		
		
		},{
		step = (yRange / xRange);
		("is it step:"+step+"a Integer?").postln;
		big = \y;
		small = \x;
		}
	);


{
var x,y,a,b,i;

x = Pseq(xList,inf).asStream;
y = Pseq(yList,inf).asStream;
a = x.next; //or first value will be nil;
b = y.next;

//("a:"+a+"b:"+b).postln;
m.sendMsg("/spatdif/core/listener/1/position", a, b); 
//l.setPosition( a,b);

i = 0;
t = Task({ { 

	i = i + 1; 
	
	if (big.asString == x,
		{
		a = x.next;
		
		if (i >= step, {
			
			b = y.next;
			i = 0;
		});
		
		},
		{
		
		b = y.next;
		
		if (i >= step, {
			
			a = x.next;
			i = 0;
		});
		
		}
	);
	
	m.sendMsg("/spatdif/core/listener/1/position", a, b); 
//	l.setPosition( a,b);
	//("a:"+a+"b:"+b).postln;
	
	pause.wait;}.loop });

pause.wait; 

t.start;

}.fork;

)

(
//t.stop;
//x.resume;
//y.resume;
)


