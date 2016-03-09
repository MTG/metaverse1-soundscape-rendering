# SC code
# // convert function definition
# var convertLogLat = { |l1,lg1,l2,lg2|
# var rr= 6371009; // this is the earth radius. Fixed values
# var dist = (rr * sqrt(( (l1* pi / 180) - (l2*pi/180))**2) + (((lg1*pi/180) - (lg2*pi/180))**2));
# ("Latitude and longitud converted to a two-point distance:" + dist).postln; };

# // example values for longitud and latitude for two points
# var refLat= 41.3494;
# var refLog= 1.70029;
# var l2= 41.348989;
# var lg2= 1.700006;

# convertLogLat.value(refLat,refLog,l2,lg2);


# Python code
import numpy as np
import scipy


def distanceLogLat(l1,lg1,l2,lg2):
  # http://en.wikipedia.org/wiki/Geographical_distance
  # input data: latitude1, longitud1, latitude2 , longitude2
  rr = 6371009 # this is the earth radius. Fixed value
  dist = rr * np.sqrt( ((l1*np.pi / 180) - (l2*np.pi/180))**2  + ((lg1*np.pi/180) - (lg2*np.pi/180))**2 )  
  return dist

def getRefPoint(pointList):
  # returns the lower-left reference point in latitude/longitude 
  refpoint = [[scipy.inf,scipy.inf],[-scipy.inf,-scipy.inf]] # lower-left, upper-right
  # using numpy array, it could be optrimized  (to be ported to SC)
  for pp in pointList:
    refpoint[0][0] = min(refpoint[0][0], pp[0])
    refpoint[0][1] = min(refpoint[0][1], pp[1])
    refpoint[1][0] = max(refpoint[1][0], pp[0])
    refpoint[1][1] = max(refpoint[1][1], pp[1])     
  
  return refpoint

def computeBoundingBox(refpoint):
  # return size in meters of the bounding box (width,height)
  # given two reference points (lower-left, upperright) expressed in latitude/longitude  
  distx = distanceLogLat(refpoint[1][0],refpoint[0][1],refpoint[1][0],refpoint[1][1]) #long difference  
  disty = distanceLogLat(refpoint[0][0],refpoint[1][1],refpoint[1][0],refpoint[1][1]) #lat difference
  boxWH = [distx, disty] # box width and height
  return boxWH
  
def convertPoints(pointList):
  convPoints = []
  refpoints = getRefPoint(pointList)
  box = computeBoundingBox(refpoints)
  
  lowleft = refpoints[0]  
  for pp in latlogPoints:
    distx = distanceLogLat(pp[0],lowleft[1],pp[0],pp[1])
    disty = distanceLogLat(lowleft[0],pp[1],pp[0],pp[1])
    convPoints.append([distx,disty])
  return convPoints, box
  
  
# EXAMPLE TEST
# It computes a reference point form a list of placemarks expressed in degrees (lat,long) 
# and convets all points to a list of points expressed in meters, referenced to a lower-left bound.
latlogPoints = [[41.40412, 2.19497], [41.40331, 2.19389],[41.40245, 2.19498],[41.40326, 2.19608]] # block around Tanger/Roc Boronat

[meterPoints, box] = convertPoints(latlogPoints)
print "placemarks in meters"
for p in  meterPoints: print p
print "bounding box"
print box

