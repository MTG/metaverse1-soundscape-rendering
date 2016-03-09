/*
    Copyright © 2002, The KPD-Team
    All rights reserved.
    http://www.mentalis.org/

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

    - Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer. 

    - Neither the name of the KPD-Team, nor the names of its contributors
       may be used to endorse or promote products derived from this
       software without specific prior written permission. 

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
  THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
  STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
  OF THE POSSIBILITY OF SUCH DAMAGE.
*/

using System;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Reflection;
using System.Collections;
using System.Net.Sockets;
using System.Security.Cryptography;
using OpenMetaverse;
using Org.Mentalis.Proxy;
using Org.Mentalis.Proxy.Http;

namespace Org.Mentalis.Proxy {
	/// <summary>
	/// Represents an item in a Listeners collection.
	/// </summary>
	public struct ListenEntry {
		/// <summary>
		/// The Listener object.
		/// </summary>
		public Listener listener;
		/// <summary>
		/// The Listener's ID. It must be unique troughout the Listeners collection.
		/// </summary>
		public Guid guid;
		/// <summary>
		/// Determines whether the specified Object is equal to the current Object.
		/// </summary>
		/// <param name="obj">The Object to compare with the current Object.</param>
		/// <returns>True if the specified Object is equal to the current Object; otherwise, false.</returns>
		public override bool Equals(object obj) {
			return ((ListenEntry)obj).guid.Equals(guid);
		}
	}
	/// <summary>
	/// Defines the class that controls the settings and listener objects.
	/// </summary>
	public class Proxy {
		/// <summary>
		/// Entry point of the application.
		/// </summary>
		public static Proxy HttpProxy(string host, int port, System.Uri relayUrl) {
			Proxy prx = new Proxy();
            Listener listener = new Org.Mentalis.Proxy.Http.HttpListener(Dns.Resolve(host).AddressList[0], port, relayUrl);
            listener.Start();
            prx.AddListener(listener);
			return prx;
		}
		/// <summary>
		/// Starts a new Proxy server by reading the data from the configuration file and start listening on the specified ports.
		/// </summary>
		public void Start() { }
		/// <summary>
		/// Stops the proxy server.
		/// </summary>
		/// <remarks>When this method is called, all listener and client objects will be disposed.</remarks>
		public void Stop() {
			// Stop listening and clear the Listener list
			for (int i = 0; i < ListenerCount; i++) {
                OpenMetaverse.Logger.Log(this[i].ToString() + " stopped.", Helpers.LogLevel.Info);
				this[i].Dispose();
			}
			Listeners.Clear();
		}
		/// <summary>
		/// Adds a listener to the Listeners list.
		/// </summary>
		/// <param name="newItem">The new Listener to add.</param>
		public void AddListener(Listener newItem) {
			if (newItem == null)
				throw new ArgumentNullException();
			ListenEntry le = new ListenEntry();
			le.listener = newItem;
			le.guid = Guid.NewGuid();
			while (Listeners.Contains(le)) {
				le.guid = Guid.NewGuid();
			}
			Listeners.Add(le);
            OpenMetaverse.Logger.Log(newItem.ToString() + " started.", Helpers.LogLevel.Info);
		}
		/// <summary>
		/// Gets the collection that contains all the Listener objects.
		/// </summary>
		/// <value>An ArrayList object that contains all the Listener objects.</value>
		protected ArrayList Listeners {
			get {
				return m_Listeners;
			}
		}
		/// <summary>
		/// Gets the number of Listener objects.
		/// </summary>
		/// <value>An integer specifying the number of Listener objects.</value>
		internal int ListenerCount {
			get {
				return Listeners.Count;
			}
		}
		/// <summary>
		/// Gets the Listener object at the specified position.
		/// </summary>
		/// <value>The Listener instance at position <c>index</c>.</value>
		internal virtual Listener this[int index] {
			get {
				return ((ListenEntry)Listeners[index]).listener;
			}
		}
		/// <summary>Holds the value of the Listeners property.</summary>
		private ArrayList m_Listeners = new ArrayList();
	}
}
