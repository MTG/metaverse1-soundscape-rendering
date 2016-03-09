using Bespoke.Common.Osc;
using System;
using System.Collections;
using System.Net;

namespace Osc
{
    class Client
    {
        public static IPAddress LookupHost(string hostName)
        {
            IPHostEntry hostEntry = Dns.GetHostByName(hostName);
            return hostEntry.AddressList[0];
        }

        public static IPEndPoint ParseIPEndPoint(string s)
        {
            string[] components = s.Split(':');
            if (components.Length != 2)
            {
                throw new Exception("Invalid IPEndPoint format"); 
            }
            String hostName = components[0];
            int port = Convert.ToInt32(components[1]);
            return new IPEndPoint(LookupHost(hostName), port);
        }

        public readonly IPEndPoint EndPoint;

        public Client(string address)
        {
            this.EndPoint = ParseIPEndPoint(address);
        }

        public Client(string hostName, int port)
        {
            this.EndPoint = new IPEndPoint(LookupHost(hostName), port);
        }
        
        public void Send(OscPacket packet)
        {
            packet.Send(this.EndPoint);
        }
    }
}
