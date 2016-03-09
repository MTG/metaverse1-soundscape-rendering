using System;
using System.Reflection;
using GridProxy;

class ProxyMain
{
    public static void Main(string[] args)
    {
        ProxyFrame p = new ProxyFrame(args);
	    ProxyPlugin m = new Metaverse1(p);
        m.Init();
	    p.proxy.Start();
    }
}
