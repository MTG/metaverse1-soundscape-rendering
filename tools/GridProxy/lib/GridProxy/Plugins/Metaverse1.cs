/*
 * Metaverse1.cs: proxy that makes packet inspection and modifcation interactive
 *   See the README for usage instructions.
 *
 * Copyright (c) 2006 Austin Jennings
 * Modified by "qode" and "mcortez" on December 21st, 2006 to work with the new
 * pregen
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without 
 *   modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Neither the name of the openmetaverse.org nor the names 
 *   of its contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Net;
using System.Text.RegularExpressions;
using System.Reflection;
using Nwc.XmlRpc;
using OpenMetaverse;
using OpenMetaverse.Packets;
using GridProxy;
using Bespoke.Common.Osc;
using Newtonsoft.Json;

// FIXME: see below
// TODO: Cache values being sent via OSC and only send when they have changed.
// TODO: Find out how to rewrite the Stream URL of a parcel (ParcelProperties).
public class Metaverse1 : ProxyPlugin
{
    private ProxyFrame frame;
    private Proxy proxy;
    private Hashtable loggedPackets = new Hashtable();
    private string logGrep = null;
    private Dictionary<PacketType, Dictionary<BlockField, object>> modifiedPackets = new Dictionary<PacketType, Dictionary<BlockField, object>>();
    private Assembly openmvAssembly;
    private StreamWriter output;
    
    private static PacketDecoder DecodePacket = new PacketDecoder();

    private Client Self;

	class HttpParam
	{
		public readonly string Name;
		public readonly string Value;

		public HttpParam(string name, string value)
		{
			Name = name;
			Value = value;
		}

		public string ToParamString()
		{
			return "&" + Name + "=" + Value;
		}
	}

    class Client
    {
		public readonly Metaverse1 Plugin;
        public readonly UUID SessionID;
        public readonly Avatar Avatar;

        public Vector3 BodyRotation;
        public Vector3 HeadRotation;

        public static readonly string DefaultHttpServer = "http://mtg110.upf.edu:8080/soundscape";
		public static readonly string DefaultSoundscape = "VirtualIsland";
        public readonly string HttpServer;
		public readonly string HttpSoundscapeName;
        public readonly HttpParam HttpSessionID;
        public readonly System.Uri StreamingUrl;

        private Dictionary<string, Osc.Client> oscClients = new Dictionary<string, Osc.Client>();
        private Org.Mentalis.Proxy.Proxy streamingProxy;

        public Client(Metaverse1 plugin, UUID sessionID, UUID agentID, string httpServer, string soundscape)
        {
			Plugin = plugin;
            SessionID = sessionID;
            Avatar = new Avatar();
            // FIXME: why is Self.Avatar.ID zero?
            // maybe frame.AgentID gets assigned later in the process ...
            Avatar.ID = agentID;
            Avatar.LocalID = 0;
            BodyRotation = new Vector3(0, 0, 0);
            HeadRotation = new Vector3(0, 0, 0);
            
			// Use IP address instead of host name to avoid DNS lookup delays during
            // realtime position updates.
            HttpServer = DnsLookupUri(new System.Uri(httpServer)).AbsoluteUri;
            HttpSoundscapeName = soundscape;

            if (HttpServer != null)
            {
                OpenMetaverse.Logger.Log("Initializing HTTP session with " + HttpServer, Helpers.LogLevel.Info);
                
                WebResponse resp = WebRequest.Create(HttpRequestString("add")).GetResponse();
            
                Stream respStream = resp.GetResponseStream();
            	System.Text.Encoding encode = System.Text.Encoding.GetEncoding("utf-8");
                // Pipe the stream to a higher level stream reader with the required encoding format. 
        	    StreamReader readStream = new StreamReader(respStream, encode);
                Dictionary<string, string> obj = JsonConvert.DeserializeObject<Dictionary<string, string>>(readStream.ReadToEnd());

				if (obj["result"] == "0")
				{
					Plugin.SayToUser("No streaming slots available, streaming disabled.");
				}
				else
				{
	                HttpSessionID = new HttpParam("clientid", obj["clientid"]);
	                StreamingUrl  = new System.Uri(obj["url"]);
	                OpenMetaverse.Logger.Log("Added HTTP client with id=" + HttpSessionID.Value + ", url=" + StreamingUrl, Helpers.LogLevel.Info);

	                streamingProxy = Org.Mentalis.Proxy.Proxy.HttpProxy("127.0.0.1", 8000, StreamingUrl);
	                streamingProxy.Start();
				}
                
                resp.Close();
            }
        }
        
        ~Client()
        {
			if (HttpSessionID != null)
			   	OpenMetaverse.Logger.Log("Tearing down HTTP session " + HttpSessionID, Helpers.LogLevel.Info);
            if (streamingProxy != null)
                streamingProxy.Stop();
            if (HttpServer != null)
                WebRequest.Create(HttpRequestString("remove", HttpSessionID)).GetResponse();
        }
        
        public void AddOscClient(string client)
        {
            oscClients[client] = new Osc.Client(client);
        }
        public void RemoveOscClient(string client)
        {
            oscClients.Remove(client);
        }
        public void RemoveAllOscClients()
        {
            oscClients = new Dictionary<string,Osc.Client>();
        }

		// Return a modified URI with the host part replaced by the result of the DNS lookup.
		private static System.Uri DnsLookupUri(System.Uri uri)
		{
			IPHostEntry he = Dns.GetHostByName(uri.DnsSafeHost);
			IPAddress[] ip_addrs = he.AddressList;
			string resolvedHost = ip_addrs[0].ToString();
			int port = uri.Port;
			return new System.Uri(uri.Scheme + "://" + resolvedHost + (port < 0 ? "" : ":" + port.ToString()) + uri.PathAndQuery);
		} 

		private static void AsynchWebResponseCallback(IAsyncResult asynchronousResult)
		{  
			try
			{
				WebRequest webRequest = (WebRequest)asynchronousResult.AsyncState;
				WebResponse response = webRequest.EndGetResponse(asynchronousResult);
				response.Close();
			}
			catch(WebException e)
			{
				Console.WriteLine("WebException raised!");
				Console.WriteLine("\n{0}",e.Message);
				Console.WriteLine("\n{0}",e.Status);
			} 
			catch(Exception e)
			{
				Console.WriteLine("Exception raised!");
				Console.WriteLine("Source : " + e.Source);
				Console.WriteLine("Message : " + e.Message);
			}
		}

		private static void MakeAsynchWebRequest(string url)
		{
            WebRequest request = WebRequest.Create(url);
			request.BeginGetResponse(new AsyncCallback(AsynchWebResponseCallback), request);
		}

		private string HttpRequestString(string command, params HttpParam[] ps)
		{
			return HttpServer
			        + "/" + command + "?name=" + HttpSoundscapeName
			        + String.Join("", ps.Select(x => x.ToParamString()).ToArray());
		}

		public void Changed()
        {
            // Send HTTP updates
            if (HttpServer != null)
            {
                MakeAsynchWebRequest(HttpRequestString("position", HttpSessionID
                                    , new HttpParam("x", Avatar.Position.X.ToString(CultureInfo.InvariantCulture))
                                    , new HttpParam("y", Avatar.Position.Y.ToString(CultureInfo.InvariantCulture))
                                    , new HttpParam("z", Avatar.Position.Z.ToString(CultureInfo.InvariantCulture))
                                    ));
                MakeAsynchWebRequest(HttpRequestString("rotation", HttpSessionID
                                    , new HttpParam("a", BodyRotation.Z.ToString(CultureInfo.InvariantCulture))
                                    ));
            }
            
            // Send OSC updates
            if (oscClients.Count > 0)
            {
                // Position
                OscMessage pos = new OscMessage(null, "/linden/agent/position");
                pos.Append(Avatar.Position.X);
                pos.Append(Avatar.Position.Y);
                pos.Append(Avatar.Position.Z);
                // Body and head rotation
                OscMessage bodyRot = new OscMessage(null, "/linden/agent/body_rotation");
                bodyRot.Append(BodyRotation.X);
                bodyRot.Append(BodyRotation.Y);
                bodyRot.Append(BodyRotation.Z);
                OscMessage headRot = new OscMessage(null, "/linden/agent/head_rotation");
                headRot.Append(HeadRotation.X);
                headRot.Append(HeadRotation.Y);
                headRot.Append(HeadRotation.Z);
                // Assemble bundle ...
                OscBundle bdl = new OscBundle(null);
                bdl.Append(pos);
                bdl.Append(bodyRot);
                bdl.Append(headRot);
                // ... and send it.
                foreach (KeyValuePair<string,Osc.Client> pair in oscClients)
                    pair.Value.Send(bdl);
            }

            
        }
    }

    public Metaverse1(ProxyFrame frame)
    {
        this.frame = frame;
        this.proxy = frame.proxy;
        
        proxy.AddDelegate(PacketType.AgentUpdate, Direction.Incoming, new PacketDelegate(OnAgentUpdate));
        proxy.AddDelegate(PacketType.AgentUpdate, Direction.Outgoing, new PacketDelegate(OnAgentUpdate));
        proxy.AddDelegate(PacketType.ObjectUpdate, Direction.Incoming, new PacketDelegate(OnObjectUpdate));
        proxy.AddDelegate(PacketType.ObjectUpdate, Direction.Outgoing, new PacketDelegate(OnObjectUpdate));
        proxy.AddDelegate(PacketType.ImprovedTerseObjectUpdate, Direction.Incoming, new PacketDelegate(OnTerseObjectUpdate));
        proxy.AddDelegate(PacketType.ImprovedTerseObjectUpdate, Direction.Outgoing, new PacketDelegate(OnTerseObjectUpdate));
        proxy.AddDelegate(PacketType.ObjectUpdateCached, Direction.Incoming, new PacketDelegate(OnObjectUpdateCached));
        proxy.AddDelegate(PacketType.ObjectUpdateCached, Direction.Outgoing, new PacketDelegate(OnObjectUpdateCached));
        proxy.AddDelegate(PacketType.SimulatorViewerTimeMessage, Direction.Incoming, new PacketDelegate(OnTimeMessage));
        proxy.AddDelegate(PacketType.SimulatorViewerTimeMessage, Direction.Outgoing, new PacketDelegate(OnTimeMessage));

        //proxy.AddDelegate(PacketType.ObjectUpdateCompressed, Direction.Incoming, new PacketDelegate(OnObjectUpdateCompressed));
        //proxy.AddDelegate(PacketType.ObjectUpdateCompressed, Direction.Outgoing, new PacketDelegate(OnObjectUpdateCompressed));

        proxy.AddDelegate(PacketType.LogoutReply, Direction.Incoming, new PacketDelegate(OnLogoutReply));
    }

    ~Metaverse1()
    {
        if (output != null)
            output.Close();
    }

    public override void Init()
    {
        openmvAssembly = Assembly.Load("OpenMetaverse");
        if (openmvAssembly == null) throw new Exception("Assembly load exception");

        // build the table of /command delegates
        InitializeCommandDelegates();

        string httpServer = Client.DefaultHttpServer;
        string soundscape = Client.DefaultSoundscape;
        string [] oscClients = {};

        //  handle command line arguments
        foreach (string arg in frame.Args)
        {
            if (arg == "--log-all")
            {
                LogAll();
            }
            else if (arg.Contains("--log-whitelist="))
            {
                LogWhitelist(arg.Substring(arg.IndexOf('=') + 1));
            }
            else if (arg.Contains("--no-log-blacklist="))
            {
                NoLogBlacklist(arg.Substring(arg.IndexOf('=') + 1));
            }
            else if (arg.Contains("--output="))
            {
                SetOutput(arg.Substring(arg.IndexOf('=') + 1));
            }
            else if (arg.Contains("--sendOSC="))
            {
                oscClients = arg.Substring(arg.IndexOf('=') + 1).Split(',');
            }
            else if (arg.Contains("--http-server="))
            {
                httpServer = arg.Substring(arg.IndexOf('=') + 1);
            }
            else if (arg.Contains("--soundscape-name="))
            {
                soundscape = arg.Substring(arg.IndexOf('=') + 1);
            }
            else if (arg == "--no-http")
            {
                httpServer = null;
            }
        }
        
        this.Self = new Client(this, frame.SessionID, frame.AgentID, httpServer, soundscape);

        foreach (string client in oscClients)
        {
            try
            {
                Self.AddOscClient(client);
                OpenMetaverse.Logger.Log("Sending OSC updates to " + client, Helpers.LogLevel.Info);
            }
            catch (Exception e)
            {
                OpenMetaverse.Logger.Log(e, Helpers.LogLevel.Info);
            }
        }

        OpenMetaverse.Logger.Log("Metaverse1 initialized", Helpers.LogLevel.Info);
    }

    // InitializeCommandDelegates: configure Metaverse1's commands
    private void InitializeCommandDelegates()
    {
        // For the moment it's useful to keep those commands for debugging purposes
        frame.AddCommand("/log", new ProxyFrame.CommandDelegate(CmdLog));
        frame.AddCommand("/-log", new ProxyFrame.CommandDelegate(CmdNoLog));
        frame.AddCommand("/grep", new ProxyFrame.CommandDelegate(CmdGrep));

        // Commands specific to metaverse
        frame.AddCommand("/sendOSC", new ProxyFrame.CommandDelegate(CmdSendOSC));
        frame.AddCommand("/-sendOSC", new ProxyFrame.CommandDelegate(CmdNoSendOSC));
		frame.AddCommand("/streamingInfo", new ProxyFrame.CommandDelegate(CmdStreamingInfo));
    }

    private static PacketType packetTypeFromName(string name)
    {
        Type packetTypeType = typeof(PacketType);
        System.Reflection.FieldInfo f = packetTypeType.GetField(name);
        if (f == null) throw new ArgumentException("Bad packet type");
        return (PacketType)Enum.ToObject(packetTypeType, (int)f.GetValue(packetTypeType));
    }

    // CmdLog: handle a /log command
    private void CmdLog(string[] words)
    {
        if (words.Length != 2)
        {
            SayToUser("Usage: /log <packet name>");
        }
        else if (words[1] == "*")
        {
            LogAll();
            SayToUser("logging all packets");
        }
        else
        {
            try
            {
                PacketType pType = packetTypeFromName(words[1]);
                loggedPackets[pType] = null;
                proxy.AddDelegate(pType, Direction.Incoming, new PacketDelegate(LogPacketIn));
                proxy.AddDelegate(pType, Direction.Outgoing, new PacketDelegate(LogPacketOut));
                SayToUser("logging " + words[1]);
            }
            catch (ArgumentException)
            {
                SayToUser("Bad packet name: " + words[1]);
            }
        }
    }

    // CmdNoLog: handle a /-log command
    private void CmdNoLog(string[] words)
    {
        if (words.Length != 2)
        {
            SayToUser("Usage: /-log <packet name>");
        }
        else if (words[1] == "*")
        {
            NoLogAll();
            SayToUser("stopped logging all packets");
        }
        else
        {
            PacketType pType = packetTypeFromName(words[1]);
            loggedPackets.Remove(pType);

            proxy.RemoveDelegate(pType, Direction.Incoming, new PacketDelegate(LogPacketIn));
            proxy.RemoveDelegate(pType, Direction.Outgoing, new PacketDelegate(LogPacketOut));
            SayToUser("stopped logging " + words[1]);
        }
    }

    // CmdGrep: handle a /grep command
    private void CmdGrep(string[] words)
    {
        if (words.Length == 1)
        {
            logGrep = null;
            SayToUser("stopped filtering logs");
        }
        else
        {
            string[] regexArray = new string[words.Length - 1];
            Array.Copy(words, 1, regexArray, 0, words.Length - 1);
            logGrep = String.Join(" ", regexArray);
            SayToUser("filtering log with " + logGrep);
        }
    }

    // CmdSendOSC: handle a /sendOSC command
    private void CmdSendOSC(string[] words)
    {
        if (words.Length != 2)
        {
            SayToUser("Usage: /sendOSC <host>:<port>");
        }
        else
        {
            string client = words[1];
            try
            {
                Self.AddOscClient(client);
            }
            catch (Exception e)
            {
                SayToUser(e.ToString());
            }
        }
    }

    // CmdSendOSC: handle a /-sendOSC command
    private void CmdNoSendOSC(string[] words)
    {
        if (words.Length > 2)
        {
            SayToUser("Usage: /-sendOSC <host>:<port>");
        }
        else if (words.Length > 1)
        {
            string client = words[1];
            Self.RemoveOscClient(client);
            SayToUser("Stopped sending OSC updates to " + client);
        }
        else
        {
            Self.RemoveAllOscClients();
            SayToUser("Stopped sending OSC updates");
        }
    }

	private void CmdStreamingInfo(string[] words)
	{
		if (Self.HttpServer == null)
		{
			SayToUser("Streaming disabled");
		}
		else
		{
			SayToUser("Session id: " + Self.HttpSessionID + ", streaming URL: " + Self.StreamingUrl);
		}
	}
	
    private double ConvertAxisAngle(float axis, float angle)
    {
        return (2*Math.PI + axis*angle) % (2*Math.PI);
    }

    // LogPacketIn: log an incoming packet
    private Packet OnAgentUpdate(Packet inPacket, IPEndPoint endPoint)
    {
        AgentUpdatePacket packet = inPacket as AgentUpdatePacket;

        // Update client body and head rotation
        Vector3 axis;
        float angle;
        packet.AgentData.BodyRotation.GetAxisAngle(out axis, out angle);
        Self.BodyRotation.X = (float)ConvertAxisAngle(axis.X, angle);
        Self.BodyRotation.Y = (float)ConvertAxisAngle(axis.Y, angle);
        Self.BodyRotation.Z = (float)ConvertAxisAngle(axis.Z, angle);
        packet.AgentData.HeadRotation.GetAxisAngle(out axis, out angle);
        Self.HeadRotation.X = (float)ConvertAxisAngle(axis.X, angle);
        Self.HeadRotation.Y = (float)ConvertAxisAngle(axis.Y, angle);
        Self.HeadRotation.Z = (float)ConvertAxisAngle(axis.Z, angle);

        // Send external state updates
        Self.Changed();
        
        return inPacket;
    }

    private Packet OnObjectUpdate(Packet inPacket, IPEndPoint endPoint)
    {
        //LogPacket(inPacket, endPoint, Direction.Incoming);
        ObjectUpdateHandler(inPacket);
        return inPacket;
    }
    private Packet OnTerseObjectUpdate(Packet inPacket, IPEndPoint endPoint)
    {
        TerseObjectUpdateHandler(inPacket);
        return inPacket;
    }
    private Packet OnObjectUpdateCached(Packet inPacket, IPEndPoint endPoint)
    {
        //TerseObjectUpdateHandler(inPacket);
        CachedUpdateHandler(inPacket);
        return inPacket;
    }
    private Packet OnObjectUpdateCompressed(Packet inPacket, IPEndPoint endPoint)
    {
        //TerseObjectUpdateHandler(inPacket);
        return inPacket;
    }

    private Packet OnTimeMessage(Packet packet, IPEndPoint endPoint)
    {
        SimulatorViewerTimeMessagePacket time = (SimulatorViewerTimeMessagePacket)packet;

/*        if (Self.OscClients.Count > 0)
        {
            OscMessage sunPhase = new OscMessage(null, "/linden/simulator/sun/phase");
            sunPhase.Append(time.TimeInfo.SunPhase);
            OscMessage sunDirection = new OscMessage(null, "/linden/simulator/sun/direction");
            sunDirection.Append((float)time.TimeInfo.SunDirection.X);
            sunDirection.Append((float)time.TimeInfo.SunDirection.Y);
            sunDirection.Append((float)time.TimeInfo.SunDirection.Z);
            OscBundle bdl = new OscBundle(null);
            bdl.Append(sunPhase);
            bdl.Append(sunDirection);
            Osc.Client.DispatchPacket(Self.OscClients, bdl);
        }
*/        
        return packet;
    }
    
    private Packet OnLogoutReply(Packet packet, IPEndPoint endPoint)
    {
        // FIXME: This is rather crude.
        Environment.Exit(0);
        return packet;
    }
    
    /// <summary>
    /// Request object information from the sim, primarily used for stale 
    /// or missing cache entries
    /// </summary>
    /// <param name="simulator">The simulator containing the object you're 
    /// looking for</param>
    /// <param name="localID">The objects ID which is local to the simulator the object is in</param>
    //public void RequestObject(uint localID)
    //{
    //    RequestMultipleObjectsPacket request = new RequestMultipleObjectsPacket();
    //    request.AgentData.AgentID = Client.Self.AgentID;
    //    request.AgentData.SessionID = Client.Self.SessionID;
    //    request.ObjectData = new RequestMultipleObjectsPacket.ObjectDataBlock[1];
    //    request.ObjectData[0] = new RequestMultipleObjectsPacket.ObjectDataBlock();
    //    request.ObjectData[0].ID = localID;
    //    request.ObjectData[0].CacheMissType = 0;

    //    Client.Network.SendPacket(request, simulator);
    //}

    /// <summary>
    /// Used for new prims, or significant changes to existing prims
    /// </summary>
    /// <param name="packet"></param>
    /// <param name="simulator"></param>
    protected void ObjectUpdateHandler(Packet packet)
    {
        ObjectUpdatePacket update = (ObjectUpdatePacket)packet;

        for (int b = 0; b < update.ObjectData.Length; b++)
        {
            ObjectUpdatePacket.ObjectDataBlock block = update.ObjectData[b];

            //ObjectUpdate objectupdate = new ObjectUpdate();
            Avatar avatar = Self.Avatar;
            NameValue[] nameValues;
            PCode pcode = (PCode)block.PCode;

            if (block.FullID != frame.AgentID)
            {
                //if (pcode == PCode.Avatar)
                //{
                //    Console.WriteLine("ObjectUpdate: block id is " + block.FullID.ToString() + " avatar id is " + frame.AgentID.ToString());
                //}
                continue;
            }

            #region NameValue parsing

            string nameValue = Utils.BytesToString(block.NameValue);
            if (nameValue.Length > 0)
            {
                string[] lines = nameValue.Split('\n');
                nameValues = new NameValue[lines.Length];

                for (int i = 0; i < lines.Length; i++)
                {
                    if (!String.IsNullOrEmpty(lines[i]))
                    {
                        NameValue nv = new NameValue(lines[i]);
                        // if (nv.Name == "AttachItemID") attachment = true;
                        nameValues[i] = nv;
                    }
                }
            }
            else
            {
                nameValues = new NameValue[0];
            }

            #endregion NameValue parsing

            #region Decode Additional packed parameters in ObjectData
            int pos = 0;
            switch (block.ObjectData.Length)
            {
                case 76:
                    // Collision normal for avatar
                    avatar.CollisionPlane = new Vector4(block.ObjectData, pos);
                    pos += 16;

                    goto case 60;
                case 60:
                    // Position
                    avatar.Position = new Vector3(block.ObjectData, pos);
                    pos += 12;
                    // Velocity
                    avatar.Velocity = new Vector3(block.ObjectData, pos);
                    pos += 12;
                    // Acceleration
                    avatar.Acceleration = new Vector3(block.ObjectData, pos);
                    pos += 12;
                    // Rotation (theta)
                    avatar.Rotation = new Quaternion(block.ObjectData, pos, true);
                    pos += 12;
                    // Angular velocity (omega)
                    avatar.AngularVelocity = new Vector3(block.ObjectData, pos);
                    pos += 12;

                    break;
                case 48:
                    // Collision normal for avatar
                    avatar.CollisionPlane = new Vector4(block.ObjectData, pos);
                    pos += 16;

                    goto case 32;
                case 32:
                    // The data is an array of unsigned shorts

                    // Position
                    avatar.Position = new Vector3(
                        Utils.UInt16ToFloat(block.ObjectData, pos, -0.5f * 256.0f, 1.5f * 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 2, -0.5f * 256.0f, 1.5f * 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 4, -256.0f, 3.0f * 256.0f));
                    pos += 6;
                    // Velocity
                    avatar.Velocity = new Vector3(
                        Utils.UInt16ToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 4, -256.0f, 256.0f));
                    pos += 6;
                    // Acceleration
                    avatar.Acceleration = new Vector3(
                        Utils.UInt16ToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 4, -256.0f, 256.0f));
                    pos += 6;
                    // Rotation (theta)
                    avatar.Rotation = new Quaternion(
                        Utils.UInt16ToFloat(block.ObjectData, pos, -1.0f, 1.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 2, -1.0f, 1.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 4, -1.0f, 1.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 6, -1.0f, 1.0f));
                    pos += 8;
                    // Angular velocity (omega)
                    avatar.AngularVelocity = new Vector3(
                        Utils.UInt16ToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f),
                        Utils.UInt16ToFloat(block.ObjectData, pos + 4, -256.0f, 256.0f));
                    pos += 6;

                    break;
                case 16:
                    // The data is an array of single bytes (8-bit numbers)

                    // Position
                    avatar.Position = new Vector3(
                        Utils.ByteToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 1, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f));
                    pos += 3;
                    // Velocity
                    avatar.Velocity = new Vector3(
                        Utils.ByteToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 1, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f));
                    pos += 3;
                    // Accleration
                    avatar.Acceleration = new Vector3(
                        Utils.ByteToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 1, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f));
                    pos += 3;
                    // Rotation
                    avatar.Rotation = new Quaternion(
                        Utils.ByteToFloat(block.ObjectData, pos, -1.0f, 1.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 1, -1.0f, 1.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 2, -1.0f, 1.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 3, -1.0f, 1.0f));
                    pos += 4;
                    // Angular Velocity
                    avatar.AngularVelocity = new Vector3(
                        Utils.ByteToFloat(block.ObjectData, pos, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 1, -256.0f, 256.0f),
                        Utils.ByteToFloat(block.ObjectData, pos + 2, -256.0f, 256.0f));
                    pos += 3;

                    break;
                default:
                    //Logger.Log("Got an ObjectUpdate block with ObjectUpdate field length of " +
                    //    block.ObjectData.Length, Helpers.LogLevel.Warning, Client);

                    continue;
            }
            #endregion

            // Update some internals if this is our avatar
            #region Update Avatar
            // FireOnObjectDataBlockUpdate(simulator, avatar, data, block, objectupdate, nameValues);

            //uint oldSeatID = avatar.ParentID;

            avatar.LocalID = block.ID;
            //Self.Avatar.CollisionPlane = objectupdate.CollisionPlane;
            //Self.Avatar.Position = position;
            //Self.Avatar.Velocity = velocity;
            //Self.Avatar.Acceleration = objectupdate.Acceleration;
            //Self.Avatar.Rotation = objectupdate.Rotation;
            //Self.Avatar.AngularVelocity = objectupdate.AngularVelocity;
            avatar.NameValues = nameValues;
            //Self.Avatar.PrimData = data;
            //if (block.Data.Length > 0) Logger.Log("Unexpected Data field for an avatar update, length " + block.Data.Length, Helpers.LogLevel.Warning);
            avatar.ParentID = block.ParentID;
            avatar.RegionHandle = update.RegionData.RegionHandle;

            // SetAvatarSittingOn(simulator, avatar, block.ParentID, oldSeatID);

            // Textures
            avatar.Textures = new Primitive.TextureEntry(
                                        block.TextureEntry, 0,
                                        block.TextureEntry.Length);

            #endregion Update Avatar
        }
    }

    /// <summary>
    /// A terse object update, used when a transformation matrix or
    /// velocity/acceleration for an object changes but nothing else
    /// (scale/position/rotation/acceleration/velocity)
    /// </summary>
    /// <param name="packet"></param>
    /// <param name="simulator"></param>
    protected void TerseObjectUpdateHandler(Packet packet)
    {
        ImprovedTerseObjectUpdatePacket terse = (ImprovedTerseObjectUpdatePacket)packet;
        //UpdateDilation(simulator, terse.RegionData.TimeDilation);

        for (int i = 0; i < terse.ObjectData.Length; i++)
        {
            ImprovedTerseObjectUpdatePacket.ObjectDataBlock block = terse.ObjectData[i];

            try
            {
                int pos = 0;

                // LocalID
                uint localID = Utils.BytesToUInt(block.Data, pos);
                pos += 4;

                // State
                Byte state = block.Data[pos++];

                // Avatar boolean
                bool isAvatar = (block.Data[pos++] != 0);

                Avatar avatar = Self.Avatar;

                #region Decode update data

                //ObjectUpdate update = new ObjectUpdate();

                // Check if we are interested in this update
                if (!isAvatar)
                {
                    continue;
                }
                else if (localID != avatar.LocalID) 
                {
                    // Console.WriteLine("Terse update: localid is " + localid.ToString() + " avatar id is " + Self.Avatar.LocalID.ToString());
                    if (avatar.LocalID == 0)
                    {
                        OpenMetaverse.Logger.Log("Terse update requesting " + localID.ToString(), Helpers.LogLevel.Debug);
/*                        Console.WriteLine("Terse update requesting " + localID.ToString());*/
                        RequestObject(localID);
                    }
                    continue;
                }

                // Collision normal for avatar
                avatar.CollisionPlane = new Vector4(block.Data, pos);
                pos += 16;

                // Position
                avatar.Position = new Vector3(block.Data, pos);
                pos += 12;
                // Velocity
                avatar.Velocity = new Vector3(
                    Utils.UInt16ToFloat(block.Data, pos, -128.0f, 128.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 2, -128.0f, 128.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 4, -128.0f, 128.0f));
                pos += 6;
                // Acceleration
                avatar.Acceleration = new Vector3(
                    Utils.UInt16ToFloat(block.Data, pos, -64.0f, 64.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 2, -64.0f, 64.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 4, -64.0f, 64.0f));
                pos += 6;
                // Rotation (theta)
                avatar.Rotation = new Quaternion(
                    Utils.UInt16ToFloat(block.Data, pos, -1.0f, 1.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 2, -1.0f, 1.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 4, -1.0f, 1.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 6, -1.0f, 1.0f));
                pos += 8;
                // Angular velocity (omega)
                avatar.AngularVelocity = new Vector3(
                    Utils.UInt16ToFloat(block.Data, pos, -64.0f, 64.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 2, -64.0f, 64.0f),
                    Utils.UInt16ToFloat(block.Data, pos + 4, -64.0f, 64.0f));
                pos += 6;

                // Textures
                // FIXME: Why are we ignoring the first four bytes here?
                if (block.TextureEntry.Length != 0)
                {
                    avatar.Textures = new Primitive.TextureEntry(block.TextureEntry, 4, block.TextureEntry.Length - 4);
                }

                #endregion Decode update data

                // Fire the pre-emptive notice (before we stomp the object)
                // FireOnObjectTerseUpdate(simulator, obj, update, terse.RegionData.RegionHandle, terse.RegionData.TimeDilation);

                //Self.Avatar.Acceleration = update.Acceleration;
                //Self.Avatar.AngularVelocity = update.AngularVelocity;
                //Self.Avatar.CollisionPlane = update.CollisionPlane;
                //Self.Avatar.Position = update.Position;
                //Self.Avatar.Rotation = update.Rotation;
                //Self.Avatar.Velocity = update.Velocity;
                //if (update.Textures != null)
                //    Self.Avatar.Textures = update.Textures;

                // Fire the callback
                //FireOnObjectUpdated(simulator, update, terse.RegionData.RegionHandle, terse.RegionData.TimeDilation);
            }
            catch (Exception e)
            {
                //Logger.Log(e.Message, Helpers.LogLevel.Warning, Client, e);
            }
        }
    }

    protected void CachedUpdateHandler(Packet packet)
    {
        ObjectUpdateCachedPacket update = (ObjectUpdateCachedPacket)packet;
        List<uint> ids;

        if (Self.Avatar.LocalID == 0)
        {
            OpenMetaverse.Logger.Log("Requesting " + update.ObjectData.Length + " cached objects" , Helpers.LogLevel.Debug);
/*            Console.WriteLine("Requesting cached objects");*/
            // Request info for all of the objects because we don't know the avatar's local id yet
            ids = new List<uint>(update.ObjectData.Length);
            for (int i = 0; i < update.ObjectData.Length; i++)
            {
                ids.Add(update.ObjectData[i].ID);
            }
            RequestObjects(ids);
        }
        else
        {
            // Request info for our avatar only
            for (int i = 0; i < update.ObjectData.Length; i++)
            {
                if (update.ObjectData[i].ID == Self.Avatar.LocalID)
                {
                    Console.WriteLine("Requesting cached avatar");
                    RequestObject(Self.Avatar.LocalID);
                    break;
                }
            }
        }
    }

    /// <summary>
    /// Request object information from the sim, primarily used for stale 
    /// or missing cache entries
    /// </summary>
    /// <param name="localID">The objects ID which is local to the simulator the object is in</param>
    public void RequestObject(uint localID)
    {
        RequestMultipleObjectsPacket request = new RequestMultipleObjectsPacket();
        request.AgentData.AgentID = frame.AgentID;
        request.AgentData.SessionID = Self.SessionID;
        request.ObjectData = new RequestMultipleObjectsPacket.ObjectDataBlock[1];
        request.ObjectData[0] = new RequestMultipleObjectsPacket.ObjectDataBlock();
        request.ObjectData[0].ID = localID;
        request.ObjectData[0].CacheMissType = 0;

        proxy.InjectPacket(request, Direction.Outgoing);
    }

    /// <summary>
    /// Request object information for multiple objects all contained in
    /// the same sim, primarily used for stale or missing cache entries
    /// </summary>
    /// <param name="localIDs">An array which contains the IDs of the objects to request</param>
    public void RequestObjects(List<uint> localIDs)
    {
        int i = 0;

        RequestMultipleObjectsPacket request = new RequestMultipleObjectsPacket();
        request.AgentData.AgentID = frame.AgentID;
        request.AgentData.SessionID = Self.SessionID;
        request.ObjectData = new RequestMultipleObjectsPacket.ObjectDataBlock[localIDs.Count];

        foreach (uint localID in localIDs)
        {
            request.ObjectData[i] = new RequestMultipleObjectsPacket.ObjectDataBlock();
            request.ObjectData[i].ID = localID;
            request.ObjectData[i].CacheMissType = 0;
            i++;
        }

        proxy.InjectPacket(request, Direction.Outgoing);
    }

    // SayToUser: send a message to the user as in-world chat
    private void SayToUser(string message)
    {
        ChatFromSimulatorPacket packet = new ChatFromSimulatorPacket();
        packet.ChatData.FromName = Utils.StringToBytes("Metaverse1");
        packet.ChatData.SourceID = UUID.Random();
        packet.ChatData.OwnerID = frame.AgentID;
        packet.ChatData.SourceType = (byte)2;
        packet.ChatData.ChatType = (byte)1;
        packet.ChatData.Audible = (byte)1;
        packet.ChatData.Position = new Vector3(0, 0, 0);
        packet.ChatData.Message = Utils.StringToBytes(message);
        proxy.InjectPacket(packet, Direction.Incoming);
    }

    // BlockField: product type for a block name and field name
    private struct BlockField
    {
        public string block;
        public string field;


        public BlockField(string block, string field)
        {
            this.block = block;
            this.field = field;
        }
    }

    private static void MagicSetField(object obj, string field, object val)
    {
        Type cls = obj.GetType();

        FieldInfo fieldInf = cls.GetField(field);
        if (fieldInf == null)
        {
            PropertyInfo prop = cls.GetProperty(field);
            if (prop == null) throw new Exception("Couldn't find field " + cls.Name + "." + field);
            prop.SetValue(obj, val, null);
            //throw new Exception("FIXME: can't set properties");
        }
        else
        {
            fieldInf.SetValue(obj, val);
        }
    }

    // MagicCast: given a packet/block/field name and a string, convert the string to a value of the appropriate type
    private object MagicCast(string name, string block, string field, string value)
    {
        Type packetClass = openmvAssembly.GetType("OpenMetaverse.Packets." + name + "Packet");
        if (packetClass == null) throw new Exception("Couldn't get class " + name + "Packet");

        FieldInfo blockField = packetClass.GetField(block);
        if (blockField == null) throw new Exception("Couldn't get " + name + "Packet." + block);
        Type blockClass = blockField.FieldType;
        if (blockClass.IsArray) blockClass = blockClass.GetElementType();
        // Console.WriteLine("DEBUG: " + blockClass.Name);

        FieldInfo fieldField = blockClass.GetField(field); PropertyInfo fieldProp = null;
        Type fieldClass = null;
        if (fieldField == null)
        {
            fieldProp = blockClass.GetProperty(field);
            if (fieldProp == null) throw new Exception("Couldn't get " + name + "Packet." + block + "." + field);
            fieldClass = fieldProp.PropertyType;
        }
        else
        {
            fieldClass = fieldField.FieldType;
        }

        try
        {
            if (fieldClass == typeof(byte))
            {
                return Convert.ToByte(value);
            }
            else if (fieldClass == typeof(ushort))
            {
                return Convert.ToUInt16(value);
            }
            else if (fieldClass == typeof(uint))
            {
                return Convert.ToUInt32(value);
            }
            else if (fieldClass == typeof(ulong))
            {
                return Convert.ToUInt64(value);
            }
            else if (fieldClass == typeof(sbyte))
            {
                return Convert.ToSByte(value);
            }
            else if (fieldClass == typeof(short))
            {
                return Convert.ToInt16(value);
            }
            else if (fieldClass == typeof(int))
            {
                return Convert.ToInt32(value);
            }
            else if (fieldClass == typeof(long))
            {
                return Convert.ToInt64(value);
            }
            else if (fieldClass == typeof(float))
            {
                return Convert.ToSingle(value);
            }
            else if (fieldClass == typeof(double))
            {
                return Convert.ToDouble(value);
            }
            else if (fieldClass == typeof(UUID))
            {
                return new UUID(value);
            }
            else if (fieldClass == typeof(bool))
            {
                if (value.ToLower() == "true")
                    return true;
                else if (value.ToLower() == "false")
                    return false;
                else
                    throw new Exception();
            }
            else if (fieldClass == typeof(byte[]))
            {
                return Utils.StringToBytes(value);
            }
            else if (fieldClass == typeof(Vector3))
            {
                Vector3 result;
                if (Vector3.TryParse(value, out result))
                    return result;
                else
                    throw new Exception();
            }
            else if (fieldClass == typeof(Vector3d))
            {
                Vector3d result;
                if (Vector3d.TryParse(value, out result))
                    return result;
                else
                    throw new Exception();
            }
            else if (fieldClass == typeof(Vector4))
            {
                Vector4 result;
                if (Vector4.TryParse(value, out result))
                    return result;
                else
                    throw new Exception();
            }
            else if (fieldClass == typeof(Quaternion))
            {
                Quaternion result;
                if (Quaternion.TryParse(value, out result))
                    return result;
                else
                    throw new Exception();
            }
            else
            {
                throw new Exception("unsupported field type " + fieldClass);
            }
        }
        catch
        {
            throw new Exception("unable to interpret " + value + " as " + fieldClass);
        }
    }

    // ModifyIn: modify an incoming packet
    private Packet ModifyIn(Packet packet, IPEndPoint endPoint)
    {
        return Modify(packet, endPoint, Direction.Incoming);
    }

    // ModifyOut: modify an outgoing packet
    private Packet ModifyOut(Packet packet, IPEndPoint endPoint)
    {
        return Modify(packet, endPoint, Direction.Outgoing);
    }

    // Modify: modify a packet
    private Packet Modify(Packet packet, IPEndPoint endPoint, Direction direction)
    {
        if (modifiedPackets.ContainsKey(packet.Type))
        {
            try
            {
                Dictionary<BlockField, object> changes = modifiedPackets[packet.Type];
                Type packetClass = packet.GetType();

                foreach (KeyValuePair<BlockField, object> change in changes)
                {
                    BlockField bf = change.Key;
                    FieldInfo blockField = packetClass.GetField(bf.block);
                    if (blockField.FieldType.IsArray) // We're modifying a variable block.
                    {
                        // Modify each block in the variable block identically.
                        // This is really simple, can probably be improved.
                        object[] blockArray = (object[])blockField.GetValue(packet);
                        foreach (object blockElement in blockArray)
                        {
                            MagicSetField(blockElement, bf.field, change.Value);
                        }
                    }
                    else
                    {
                        //Type blockClass = blockField.FieldType;
                        object blockObject = blockField.GetValue(packet);
                        MagicSetField(blockObject, bf.field, change.Value);
                    }
                }
            }
            catch (Exception e)
            {
                Console.WriteLine("failed to modify " + packet.Type + ": " + e.Message);
                Console.WriteLine(e.StackTrace);
            }
        }

        return packet;
    }

    // LogPacketIn: log an incoming packet
    private Packet LogPacketIn(Packet packet, IPEndPoint endPoint)
    {
        LogPacket(packet, endPoint, Direction.Incoming);
        return packet;
    }

    // LogPacketOut: log an outgoing packet
    private Packet LogPacketOut(Packet packet, IPEndPoint endPoint)
    {
        LogPacket(packet, endPoint, Direction.Outgoing);
        return packet;
    }

    // LogAll: register logging delegates for all packets
    private void LogAll()
    {
        Type packetTypeType = typeof(PacketType);
        System.Reflection.MemberInfo[] packetTypes = packetTypeType.GetMembers();

        for (int i = 0; i < packetTypes.Length; i++)
        {
            if (packetTypes[i].MemberType == System.Reflection.MemberTypes.Field && packetTypes[i].DeclaringType == packetTypeType)
            {
                string name = packetTypes[i].Name;
                PacketType pType;

                Console.WriteLine("Logging packet type `" + name + "'");
                
                try
                {
                    pType = packetTypeFromName(name);
                }
                catch (Exception)
                {
                    continue;
                }

                loggedPackets[pType] = null;

                proxy.AddDelegate(pType, Direction.Incoming, new PacketDelegate(LogPacketIn));
                proxy.AddDelegate(pType, Direction.Outgoing, new PacketDelegate(LogPacketOut));
            }
        }
    }

    private void LogWhitelist(string whitelistFile)
    {
        try
        {
            string[] lines = File.ReadAllLines(whitelistFile);
            int count = 0;

            for (int i = 0; i < lines.Length; i++)
            {
                string line = lines[i].Trim();
                if (line.Length == 0)
                    continue;

                PacketType pType;

                try
                {
                    pType = packetTypeFromName(line);
                    proxy.AddDelegate(pType, Direction.Incoming, new PacketDelegate(LogPacketIn));
                    proxy.AddDelegate(pType, Direction.Outgoing, new PacketDelegate(LogPacketOut));
                    ++count;
                }
                catch (ArgumentException)
                {
                    Console.WriteLine("Bad packet name: " + line);
                }
            }

            Console.WriteLine(String.Format("Logging {0} packet types loaded from whitelist", count));
        }
        catch (Exception)
        {
            Console.WriteLine("Failed to load packet whitelist from " + whitelistFile);
        }
    }

    private void NoLogBlacklist(string blacklistFile)
    {
        try
        {
            string[] lines = File.ReadAllLines(blacklistFile);
            int count = 0;

            for (int i = 0; i < lines.Length; i++)
            {
                string line = lines[i].Trim();
                if (line.Length == 0)
                    continue;

                PacketType pType;

                try
                {
                    pType = packetTypeFromName(line);
                    string[] noLogStr = new string[] {"/-log", line};
                    CmdNoLog(noLogStr);
                    ++count;
                }
                catch (ArgumentException)
                {
                    Console.WriteLine("Bad packet name: " + line);
                }
            }

            Console.WriteLine(String.Format("Not logging {0} packet types loaded from blacklist", count));
        }
        catch (Exception)
        {
            Console.WriteLine("Failed to load packet blacklist from " + blacklistFile);
        }
    }

    private void SetOutput(string outputFile)
    {
        try
        {
            output = new StreamWriter(outputFile, false);
            Console.WriteLine("Logging packets to " + outputFile);
        }
        catch (Exception)
        {
            Console.WriteLine(String.Format("Failed to open {0} for logging", outputFile));
        }
    }

    // NoLogAll: unregister logging delegates for all packets
    private void NoLogAll()
    {
        Type packetTypeType = typeof(PacketType);
        System.Reflection.MemberInfo[] packetTypes = packetTypeType.GetMembers();

        for (int i = 0; i < packetTypes.Length; i++)
        {
            if (packetTypes[i].MemberType == System.Reflection.MemberTypes.Field && packetTypes[i].DeclaringType == packetTypeType)
            {
                string name = packetTypes[i].Name;
                PacketType pType;

                try
                {
                    pType = packetTypeFromName(name);
                }
                catch (Exception)
                {
                    continue;
                }

                loggedPackets.Remove(pType);

                proxy.RemoveDelegate(pType, Direction.Incoming, new PacketDelegate(LogPacketIn));
                proxy.RemoveDelegate(pType, Direction.Outgoing, new PacketDelegate(LogPacketOut));
            }
        }
    }

    // LogPacket: dump a packet to the console
    private void LogPacket(Packet packet, IPEndPoint endPoint, Direction direction)
    {
        string packetText = DecodePacket.PacketToString(packet);

        if (logGrep == null || (logGrep != null && Regex.IsMatch(packetText, logGrep)))
        {
            string line = String.Format("{0}\n{1} {2,21} {3,5} {4}{5}{6}"
                , packet.Type
                , direction == Direction.Incoming ? "<--" : "-->"
                , endPoint
                , packet.Header.Sequence
                , InterpretOptions(packet.Header)
                , Environment.NewLine
                , packetText
                );

            Console.WriteLine(line);

            if (output != null)
                output.WriteLine(line);
        }
    }

    // InterpretOptions: produce a string representing a packet's header options
    private static string InterpretOptions(Header header)
    {
        return "["
             + (header.AppendedAcks ? "Ack" : "   ")
             + " "
             + (header.Resent ? "Res" : "   ")
             + " "
             + (header.Reliable ? "Rel" : "   ")
             + " "
             + (header.Zerocoded ? "Zer" : "   ")
             + "]"
             ;
    }
}
