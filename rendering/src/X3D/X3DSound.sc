/*
   Copyright (C) 2009 MTG, Stefan Kersten <stefan.kersten@upf.edu>
   
   This program is free software; you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the Free
   Software Foundation; either version 2 of the License, or (at your option)
   any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT
   ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
   FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
   more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc., 59
   Temple Place - Suite 330, Boston, MA 02111-1307, USA.

   Music Technology Group
   Universitat Pompeu Fabra
   http://mtg.upf.edu/

*/

/* This abstract node type is the base for all sound nodes.
*/
X3DSoundNode : X3DChildNode {
}

/* This abstract node type is used to derive node types that can emit audio data.
*/
X3DSoundSourceNode : X3DTimeDependentNode { 
    *x3dName { ^'SoundSource' }
    declareFields {
        super.declareFields;
        declareX3D(
            [ SFString.type , [in,out] , \description      , ""       ],
            [ SFFloat.type  , [in,out] , \pitch            , 1        ],
            [ SFTime.type   , [out]    , \duration_changed , \default ]
        );
    }
}

/* An AudioClip node specifies audio data that can be referenced by Sound
   nodes.

   The description field specifies a textual description of the audio source.
   A browser is not required to display the description field but may choose
   to do so in addition to playing the sound.

   The url field specifies the URL from which the sound is loaded. Browsers
   shall support at least the wavefile format in uncompressed PCM format (see
   [WAV]). It is recommended that browsers also support the MIDI file type 1
   sound format (see 2.[MIDI]) and the MP3 compressed format (see
   2.[I11172-1]). MIDI files are presumed to use the General MIDI patch set.
   9.2.1 URLs contains details on the url field.

   The loop, pauseTime, resumeTime, startTime, and stopTime inputOutput fields
   and the elapsedTime, isActive, and isPaused outputOnly fields, and their
   effects on the AudioClip node, are discussed in detail in 8 Time component.
   The "cycle" of an AudioClip is the length of time in seconds for one
   playing of the audio at the specified pitch.

   The pitch field specifies a multiplier for the rate at which sampled sound
   is played. Values for the pitch field shall be greater than zero. Changing
   the pitch field affects both the pitch and playback speed of a sound. A
   set_pitch event to an active AudioClip is ignored and no pitch_changed
   field is generated. If pitch is set to 2.0, the sound shall be played one
   octave higher than normal and played twice as fast. For a sampled sound,
   the pitch field alters the sampling rate at which the sound is played. The
   proper implementation of pitch control for MIDI (or other note sequence
   sound clips) is to multiply the tempo of the playback by the pitch value
   and adjust the MIDI Coarse Tune and Fine Tune controls to achieve the
   proper pitch change.

   A duration_changed event is sent whenever there is a new value for the
   "normal" duration of the clip. Typically, this will only occur when the
   current url in use changes and the sound data has been loaded, indicating
   that the clip is playing a different sound source. The duration is the
   length of time in seconds for one cycle of the audio for a pitch set to
   1.0. Changing the pitch field will not trigger a duration_changed event. A
   duration value of "−1" implies that the sound data has not yet loaded or
   the value is unavailable for some reason. A duration_changed event shall be
   generated if the AudioClip node is loaded when the X3D file is read or the
   AudioClip node is added to the scene graph.

   The isActive field may be used by other nodes to determine if the clip is
   currently active. If an AudioClip is active, it shall be playing the sound
   corresponding to the sound time (i.e., in the sound's local time system
   with sample 0 at time 0):

       t = (now − startTime) modulo (duration / pitch)
*/
X3DAudioClip : X3DSoundSourceNode {
    *x3dName { ^\AudioClip }
    declareFields {
        super.declareFields;
        this.mixin(X3DUrlObject);
    }
}

/* This abstract node type is the base for all sound nodes.
*/
X3DSound : X3DSoundNode {
    *x3dName { ^'Sound' }
    declareFields {
        super.declareFields;
        this.declareX3D(
            [SFVec3f.type                    , [in,out] , \direction  , Vector3(0, 0, 1) ],
            [SFFloat.type                    , [in,out] , \intensity  , 1                 ],
            [SFVec3f.type                    , [in,out] , \location   , Vector3(0, 0, 0) ],
            [SFFloat.type                    , [in,out] , \maxBack    , 10                ],
            [SFFloat.type                    , [in,out] , \maxFront   , 10                ],
            [SFFloat.type                    , [in,out] , \minBack    , 1                 ],
            [SFFloat.type                    , [in,out] , \minFront   , 1                 ],
            [SFFloat.type                    , [in,out] , \priority   , 0                 ],
            [SFNode.type(X3DSoundSourceNode) , [in,out] , \source     , nil               ],
            [SFBool.type                     , []       , \spatialize , true              ]
        );
    }
}

/* Room effect extension */
/*X3DRoomEffect : X3DGroupingNode {
    *x3dName { ^\RoomEffect }
    declareFields {
        declareX3D(
            [ ]
        );
    }
}
*/
