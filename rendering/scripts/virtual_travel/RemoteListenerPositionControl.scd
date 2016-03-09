{\rtf1\ansi\ansicpg1252\cocoartf1038\cocoasubrtf250
{\fonttbl\f0\fnil\fcharset0 Monaco;}
{\colortbl;\red255\green255\blue255;\red191\green0\blue0;\red0\green0\blue191;\red96\green96\blue96;
\red0\green115\blue0;}
\pard\tx560\tx1120\tx1680\tx2240\tx2800\tx3360\tx3920\tx4480\tx5040\tx5600\tx6160\tx6720\ql\qnatural\pardirnatural

\f0\fs18 \cf2 //to move listener in the steam server call \cf0 \
m = \cf3 NetAddr\cf0 (\cf4 "metaverse1@ws062583.ca.upf.edu"\cf0 , 57120); \cf2 // the url should be host name or//IP: 10.80.5.139\cf0 \
\cf2 //port 57120 is the one SuperCollider opens by default in order to listen incoming messages. \cf0 \
\cf2 //m.sendMsg("/spatdif/core/listener/1/position", posx, posy, orient); \cf0 \
m = \cf3 NetAddr\cf0 (\cf4 "10.80.5.139"\cf0 , 57120);\
m = \cf3 NetAddr\cf0 (\cf4 "127.0.0.1"\cf0 , 57120); \cf2 //local host address\cf0 \
m.sendMsg(\cf4 "/spatdif/core/listener/1/position"\cf0 , 150, 100); \
\
m.sendMsg(\cf4 "/spatdif/core/listener/1/move"\cf0 , \cf5 'gostraight'\cf0 ); \
m.sendMsg(\cf4 "/spatdif/core/listener/1/move"\cf0 , \cf4 "goback"\cf0 ); \
m.sendMsg(\cf4 "/spatdif/core/listener/1/move"\cf0 , \cf4 "turnleft"\cf0 ); \
m.sendMsg(\cf4 "/spatdif/core/listener/1/move"\cf0 , \cf4 "turnright"\cf0 ); \
\
\
\cf2 GeoListenerGUI.new(address: "10.80.5.139"); // Remote control\
\
\
\
\
GeoListenerGUI.new(address: \cf4 "127.0.0.1"\cf2 ); //Local Machine}