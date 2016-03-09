# Soundscape Generation - Virtual Travel
# UPF-MTG, 2010

## GridProxy for soundscape generation and streaming

The capabilities for creating generative soundscapes in SecondLife are currently quite limited. Thus we intercept the traffic between the SecondLife server (sim) and the viewer in order to send status updates (such as avatar posoition, head rotation, time of day, etc.) to a custom soundscape streaming server with an HTTP API. A separate stream is generated for each client and received through the SecondLife viewer application via a local streaming proxy connection.

We use the [OpenMetaverse][] library (currently version 0.7.0) to intercept the traffic between viewer and server and an adapted version of the [Mentalis][] web proxy application to relay the soundscape stream.

## Prerequisites

On MacOS X you need to install the [Mono][] .NET development environment.

## Usage instructions

You can find all executables in the `bin` directory of this distribution package.

### Starting the GridProxy and SecondLife

If you have installed SecondLife to the standard location, you can use the corresponding shell script for your platform, either by double-clicking it or by starting it from the commandline.

On MacOS X type

    ./SecondLife\ Metaverse1\ OSX

or double-click on the file in the Finder.

On Windows type

    SecondLife Metaverse1 Windows.bat

or double-click on the file in the explorer.

If SecondLife is in a non-standard location, you need to start the GridProxy separately from the commandline.

On MacOS X type

    mono GridProxyApp.exe

On Windows type

    GridProxyApp.exe

Then you need to launch SecondLife with a command line argument to connect to the SL server through the GridProxy.

On MacOS X type

    "/Applications/Second Life Viewer 2.app/Contents/MacOS/Second Life" -loginuri http://localhost:8080/

On Windows type

    C:\Program Files\SecondLifeViewer2\SecondLife.exe -loginuri http://localhost:8080

### Configuring SecondLife

Open the SecondLife preferences (Me>Preferences>Sound&Media). Mute the "Ambient" and "Sound effects" sliders by clicking on the icon, and activate the "Streaming Music" slider.

## Interacting with the GridProxy

For displaying the current streaming client id and streaming url, type

    /streamingInfo

in the SecondLife chat box.

## Additional notes

* The GridProxy application needs to access the HTTP port 8080 to send the position information to the soundscape server. This needs to be taken into account in the firewall configuration.
* The GridProxy also occupies the local ports 8080 and 8000 for the SecondLife login and the streaming relay, respectively. Make sure these ports are available and not occupied by other applications.
* Currently, there is a latency of 2.5 seconds due to the streaming server configuration.

[OpenMetaverse]: http://openmetaverse.org/projects/libopenmetaverse/
[Mentalis]: http://www.mentalis.org/soft/projects/proxy/
[Mono]: http://mono.org/
