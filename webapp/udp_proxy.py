#!/usr/bin/env python

from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor

class Echo(DatagramProtocol):
    def __init__(self,port):
        self.port = port
        self.host="10.55.0.97" #campeche
    
    def datagramReceived(self, data, (host, port)):
	#print "received %r from %s:%d" % (data, host, port)
	#print "sending it to %s:%d"%(self.host,self.port) 
        self.transport.write(data, (self.host, self.port))

reactor.listenUDP(57130, Echo(57130))
reactor.listenUDP(57131, Echo(57131))
reactor.listenUDP(57132, Echo(57132))
reactor.listenUDP(57133, Echo(57133))
reactor.listenUDP(57134, Echo(57134))
reactor.run()

