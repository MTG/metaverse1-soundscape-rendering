# Metaverse1 soundscape rendering system

[Metaverse1](http://www.metaverse1.org/) was a three year ITEA2 research project, carried out between 2008 and 2011. The project goal was the creation of standards for the interaction between virtual worlds and the real world and the project culminated in the creation of the MPEG-V standard.

This repository contains code and documentation for the soundscape rendering application developed at the [Music Technology Group](http://mtg.upf.edu) for a virtual tourism use case within the Metaverse1 project. The soundscape rendering application was developed in SuperCollider, and the audio streaming server was programmed in Twisted/Python.

The system is a soundscape composition and rendering environment based on a generative graph based model of sound concepts. The code is provided mostly for archival and documentation purposes, in case it is interesting and useful for someone.

Documentation is scarce; if you have any questions or suggestions, please submit an issue in our issue tracker!

## Documentation

General project documentation, a demo video and a list of related publications can be found [on the MTG website](http://mtg.upf.edu/technologies/soundscapes). Additionally, there is some developer and user documentation in the [doc](/doc) directory.

Here's a short repository overview:

| Directory                | Content                                           |
| -------------------------|---------------------------------------------------|
| `config`                 | Darkice and Puppet configuration files            |
| `doc`                    | Developer and user documentation                  |
| `rendering`              | Soundscape rendering application (SuperCollider)  |
| `tools/GridProxy`        | SecondLife-Soundscape position relay utility      |
| `tools/java`             | OpenSoundControl sending utility (Java)           |
| `webapp`                 | Soundscape web API and UDP relay (Twisted/Python) |
