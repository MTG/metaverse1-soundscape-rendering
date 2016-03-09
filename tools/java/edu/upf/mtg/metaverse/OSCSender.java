package edu.upf.mtg.metaverse;

import java.net.InetAddress;
import java.util.logging.Logger;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Tuple3f;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class OSCSender {
	
	private OSCPortOut oscPort;
	private static final Logger logger = Logger.getLogger(OSCSender.class.getName());
	private static final String POSITION = "/position";
	private static final String ORIENTATION = "/orientation";
	private static final String VELOCITY = "/velocity";
	private static final String AUDIO_ENGINE_HOST = "10.80.4.164";
	private static final int AUDIO_ENGINE_PORT = 57120;

	public OSCSender(){
		createOSCPort();
	}
	
	private void createOSCPort(){
		try{
			oscPort = new OSCPortOut(InetAddress.getByName(AUDIO_ENGINE_HOST),AUDIO_ENGINE_PORT);
		}catch(Exception e){			
			System.out.println("METAVERSE - FAILED TO CREATE  PORT");
			e.printStackTrace();
		}								
	}
	
	private Object[] asObjectArray(Tuple3f tuple){
		Object[] array ={new Float(tuple.x), new Float(tuple.y),new Float(tuple.z)};
		return array;
	}

	private void send(OSCMessage msg){
		try{
			if(oscPort==null)createOSCPort();
			oscPort.send(msg);			
		}
		catch(Exception e){
			System.out.println("METAVERSE - FAILED TO SEND MESSAGE");
			e.printStackTrace();
		}			
	}
	
	public void sendPosition(Point3f position){	
		System.out.println("position:"+position);
		OSCMessage msg = new OSCMessage(POSITION, asObjectArray(position));
		send(msg);
	}
	
	public void sendOrientation(Vector3f orientation){
		System.out.println("orientation:"+orientation);
		OSCMessage msg = new OSCMessage(ORIENTATION, asObjectArray(orientation));
		send(msg);
	}
	
	public void sendVelocity(float val){
		Object[] array ={new Float(val)};
		OSCMessage msg = new OSCMessage(VELOCITY, array);
		send(msg);
	}
}
