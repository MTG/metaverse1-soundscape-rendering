
//compute distance in meter from 2 place mark in longitude | latitude --

//TODO: this implementation doesn't work properly...when 50 meter, return 1km....
	
/* 

Calculating the distance between geographical coordinates is based on some level of abstraction; it does not provide an exact distance, which is unattainable if one attempted to account for every irregularity in the surface of the earth. Common abstractions for the surface between two geographic points are:

    * Flat surface;
    * Spherical surface;
    * Ellipsoidal surface.

All abstractions above ignore changes in elevation.

A planar approximation for the surface of the earth may be useful over small distances. The accuracy of distance calculations using this approximation become increasingly inaccurate as:

    * The separation between the points becomes greater;
    * A point becomes closer to a geographic pole.

R = earth’s radius (mean radius = 6,371km)
Δlat = lat2− lat1
Δlong = long2− long1
a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
c = 2.atan2(√a, √(1−a))
d = R.c */

(
r = 6371009; //m earth radious

d = {arg longLat1, longLat2;  //  longLat1 = [long,lat]
	var dLat, dLong, dist, a, c, long1, long2, lat1, lat2;
	
	long1 = longLat1[0];
	long2 = longLat2[0];
	lat1 = longLat1[1];
	lat2 = longLat2[1];
	
	//dLong = (longLat1[0]  -  longLat2[0]) ;
	//dLat = (longLat1[1]  -  longLat2[1]) ;
	
	//diffLat = diffLat * diffLat; wrong you should consider sphere relation
	//diffLong = diffLong* diffLong;
	
	dLat = (lat2 - lat1) ;
	dLong = (long2 - long1);
	
	
	a = (sin(dLat/2) * sin(dLat/2)) + (cos(lat1) * cos(lat2) * sin(dLong/2) * sin(dLong/2)) ;
	c = 2 * atan((sqrt(a)/sqrt(1-a)));
	dist = r * c ;
	dist.postln;
	^dist;
	};
	
)



d.value([-122.0856552124024,37.4224281311035],[15.02468937557116,37.67395167941667]);

//jordi KML distances  http://oaxaca.upf.es/metaverse1/media/freesoundScapes/soundscapeTest.kml
//		children playing					birds	
d.value([2.195368386292103,41.40349885158449],[2.195391422909756,41.4037024062987]);
//result should be around 100 meter!!!!!

//		birds						dog barking
d.value([2.195368386292103,41.40349885158449],[2.195470541516742,41.40329219761719]);
1468.6737612299 meter

//	cambridge								//brund
d.value([0.1312370002642327,52.2025439956543],[-1.852060002690059,53.14996300304585]);

//


