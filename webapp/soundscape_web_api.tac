from twisted.web import resource, server, static
from twisted.application import internet, service
from twisted.internet import reactor
from txosc import osc
from txosc import async
import time

MAX_CLIENTS=4
MAX_TIME=120 # sec
CLEANUP_TIME=10000 # ms
BASE_URL="http://mtg110.upf.edu:8000/soundscape-"

client = async.DatagramClientProtocol()
client_port = reactor.listenUDP(0, client)
host="10.55.0.97"
port=57130
pool = [None for i in range(MAX_CLIENTS)]



class Add(resource.Resource):
   isLeaf = True
   def render_GET(self, request):
      request.setHeader("Cache-Control", "no-cache")
      name = request.args['name'][0]
      for i in range(MAX_CLIENTS):
	if pool[i] is None:
	  	pool[i] = time.time()	
      		client.send(osc.Message("/%s/add/entity/listener"%name,i), (host, port))
      		return "{'result':1,'url':'"+BASE_URL+str(i)+"', 'clientid':'"+str(i)+"'}"
      return "{'result':0}"

class Remove(resource.Resource):
   isLeaf = True
   def render_GET(self, request):
      name = request.args['name'][0]
      clientid=request.args['clientid'][0]
      request.setHeader("Cache-Control", "no-cache")
      client.send(osc.Message("/%s/remove/entity/listener"%name,int(clientid)), (host, port))
      pool[int(clientid)]=None
      return "{'result':1}"

class Position(resource.Resource):
   isLeaf = True
   def render_GET(self, request):
        request.setHeader("Cache-Control", "no-cache")
        name = request.args['name'][0]
        print "POSITION"
	pool[int(request.args['clientid'][0])] = time.time()	
      #try:
	client.send(osc.Message("/%s/spatdif/core/listener/"%name+str(request.args['clientid'][0])+"/position",float(request.args['x'][0]),float(request.args['y'][0])), (host, port))
      	return "{'result':1}"
      #except:
      #	return "{'result':-1}"

class Rotation(resource.Resource):
   isLeaf = True
   def render_GET(self, request):
        request.setHeader("Cache-Control", "no-cache")
        name = request.args['name'][0]
	pool[int(request.args['clientid'][0])] = time.time()	
      #try:
	client.send(osc.Message("/%s/spatdif/core/listener/"%name+str(request.args['clientid'][0])+"/direction",float(request.args['a'][0])), (host, port))
      	return "{'result':1}"
      #except:
      #	return "{'result':-1}"
root = resource.Resource()
child = resource.Resource()
root.putChild('soundscape', child)
child.putChild('add', Add())
child.putChild('remove', Remove())
child.putChild('position', Position())
child.putChild('rotation', Rotation())

def cleanUp():
    for i in range(MAX_CLIENTS):
        if pool[i] is not None:
       	    if time.time()-pool[i] > MAX_TIME:
		print "cleaning up ",i
		pool[i]=None

site = server.Site(root)
serv = internet.TCPServer(8080, site, CLEANUP_TIME)
application = service.Application('soundscape')
cleanupServer = internet.TimerService(1, cleanUp)
serv.setServiceParent(application)
cleanupServer.setServiceParent(application)
