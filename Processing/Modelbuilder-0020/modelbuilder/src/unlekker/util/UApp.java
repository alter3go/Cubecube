package unlekker.util;

import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.media.opengl.GL;

import controlP5.ControlEvent;

import processing.core.*;
import processing.opengl.PGraphicsOpenGL;
import unlekker.modelbuilder.UNav3D;
import unlekker.modelbuilder.UVec3;

public class UApp extends UAppBase implements UConstants {
	public static boolean appNoUpdate=false;

	public UConfig conf;
	
  public int appWidth,appHeight,appWindowX,appWindowY;
  public String appName,appRenderer;
  public boolean appUndecorated;
  
  public UAppSketch sketch[],sketchCurrent;
  public int sketchNum=0,sketchCurrentID;
	protected int switchToSketchID=-1;

	public USimpleGUI gui;
	public UNav3D nav;
	public UCameraTransition cam;
	protected boolean doLights;
	
	public UApp(PApplet p) {
		super(p);
		appName=p.getClass().getSimpleName();
		
	}
	
	public void addGUI(USimpleGUI gui) {
		this.gui=gui;
		if(nav!=null) nav.setGUI(gui);
		if(cam!=null) cam.setGUI(gui);
	}
	
	public void addCam(int viewNum) {
		cam=new UCameraTransition(p).setViewNum(viewNum);
		if(gui!=null) cam.addGUI(gui);
	}
	
	public void addNav3D() {
		nav=new UNav3D(p);
		nav.setTranslation(p.width/2,p.height/2,0);
		if(gui!=null) nav.setGUI(gui);
	}
	
	public void draw() {
		p.pushMatrix();
		
		if(doLights) lightsSet();
		
		if(cam!=null) {
			cam.update();
			cam.nav.doTransforms();
		}
		else if(nav!=null) nav.doTransforms();
		
		if(sketchCurrent!=null) sketchCurrent.draw();
		
		p.popMatrix();
		
		if(switchToSketchID>-1) {
			switchSketchImmediate(switchToSketchID);
			log(sketchCurrent.getClass().getSimpleName());
		}
		
		if(gui!=null) {
			p.rectMode(p.CORNER);
			gui.draw();
			
//			p.fill(255,0,0);
//			p.rect(0,0, 5,5);
		}
	}
	
	public void disableGUI() {
		if(gui!=null) gui.disable();
	}

	public void enableGUI() {
		if(gui!=null) gui.enable();
	}

	public void lightsSet() {
		p.lights();
	}

	public void lights() {
		doLights=true;
	}

	public void noLights() {
		doLights=false;
	}

	public void loadConfig(String filename) {
		conf=new UConfig(filename);
		appWidth=conf.getInt("appWidth", appWidth);
		appHeight=conf.getInt("appHeight", appHeight);
		appWindowX=conf.getInt("appWindowX", appWindowX);
		appWindowY=conf.getInt("appWindowY", appWindowY);
	}
	
	public void init() {
    if(p.frame!=null && appUndecorated) try {
    	p.frame.dispose();
    	p.frame.setUndecorated(true);
    }
    catch(Exception e) {
    	log(e.toString());
    }		
    
	}
	
  public void setupWindow(int _w,int _h,int _locx,int _locy,String _renderer,boolean hasFrame) {
  	appWidth=_w;
  	appHeight=_h;
  	appWindowX=_locx;
  	appWindowY=_locy;
  	appUndecorated=!hasFrame;
  	appRenderer=_renderer;
  }
  
  public void switchSketch(int id) {
  	if(id<sketchNum && sketch[id]!=null) {
  		switchToSketchID=id;
  	}
  }
  
  public void switchSketchImmediate(int id) {
  	sketchCurrent=sketch[switchToSketchID];
  	sketchCurrent.reinit();
  	sketchCurrentID=switchToSketchID;
  	switchToSketchID=-1;
  }
  
  public void addSketch(UAppSketch newSketch) {
  	if(sketch==null) sketch=new UAppSketch[10];
  	sketch[sketchNum++]=newSketch;
  }
	
	public void setup() {
		p.size(appWidth,appHeight,appRenderer);
//		p.noCursor();
		p.frame.setLocation(appWindowX,appWindowY);
  	// add a MouseWheelListener so we can use the mouse wheel
    // to zoom with
    p.frame.addMouseWheelListener(new MouseWheelInput());    
		p.registerKeyEvent(this);
		
	}

  public void logToFile() {
  	String fname=UIO.savePath(
  			UIO.getCurrentDir()+
  			UIO.DIRCHAR+"logs"+UIO.DIRCHAR+
  			UUtil.dateStr()+" "+appName+".log");
  	UUtil.logToFile(fname);
  }

  public void keyPressed() {
  	try {
			int val=p.keyCode-KeyEvent.VK_F1;
			if(val>-1 && val<10 && sketch!=null && sketch[val]!=null) {
				switchToSketchID=val;
			}
			if(sketchCurrent!=null) sketchCurrent.keyPressed();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  }

	public void controlEvent(ControlEvent ev) {
//		p.println("UApp controlEvent");

		if(cam!=null) cam.controlEvent(ev);
		if(sketchCurrent!=null) sketchCurrent.controlEvent(ev);
	}


	public float getCamT() {
		if(cam!=null) return cam.camT;
		return -1;
	}
	
	// convenience class to listen for MouseWheelEvents and
	// use it for that classic "zoom" effect

	class MouseWheelInput implements MouseWheelListener {
		Method method;
		
		public MouseWheelInput() {
			try {
				Class params[]={Float.TYPE};
				Class pcls=p.getClass();
				method=pcls.getMethod("mouseWheelMoved", params);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
				UUtil.log("mouseWheelMoved() method not found.");
			}
		}
		
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(method!=null) try {
				int step=e.getWheelRotation();
				
					method.invoke(p, new Object [] {new Float((float)e.getWheelRotation())});
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
		}

	}

  public void translate(UVec3 c) {
    p.translate(c.x, c.y,c.z);
    
  }
	

}
