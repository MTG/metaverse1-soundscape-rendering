package edu.upf.mtg.metaverse;

import java.util.logging.Logger;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import org.jdesktop.lg3d.wonderland.darkstar.client.ChannelController;
import org.jdesktop.lg3d.wonderland.darkstar.client.ChannelController.LoginResult.Status;

import org.jdesktop.lg3d.wonderland.scenemanager.UserMotionListener;
public class WorldListener implements UserMotionListener{

	private Point3f prevPosition;
	private Vector3f prevLookDirection;	
	private float prevVelocity= 0;
	private boolean loggedIn = false;
	private static final Logger logger = Logger.getLogger(WorldListener.class.getName()); //@TODO
	private static final float EPSILON = (float)0.02;
	
	private OSCSender sender;
		
	
	public WorldListener(){
		System.out.println("METAVERSE - new Listener");
		sender = new OSCSender();				
		ChannelController.getController().addLoginListener(new ChannelController.LoginListener() {
            public void beforeLogin() {}
            public void afterLogin(Status status) {loggedIn = true;}
		});				
	}
	
	public void userMoved(Point3f position, Vector3f lookDirection, Vector3f velocity, Vector3f upVector) {
		if (!loggedIn) return;
		
		if(sender==null)sender = new OSCSender();
		if(prevPosition != null)sender.sendPosition(position);											
		prevPosition = position;
		
		if(prevLookDirection!=null)sender.sendOrientation(lookDirection);						
		prevLookDirection = lookDirection;
		if (Math.abs(velocity.length()-prevVelocity)>EPSILON) sender.sendVelocity(velocity.length());
		prevVelocity = velocity.length();		
	}	
}
