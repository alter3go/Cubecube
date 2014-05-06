package unlekker.modelbuilder;

/*
 * TODO
 * 
 * - Fix duplicate mesh storage
 * - UFace / UQuad - inset() function
 * - Inset uses vector of two faces, added together and normalized, then multiplied to achieve
 * desired inset
 * - Check to see that inset is consistent
 * - Method for overriding inset if too great
 * - UVec3.delta(v1,v2) / UVec3.delta(v1,v2,normLength)
 * - select / remove quads / triangles based on vertices / vertex IDs
 */
/**
 * Utility class to deal with mesh geometries. It uses the PApplet's <code>beginShape() / vertex() / endShape()</code> to build meshes.
 * A secondary function of the class is to act as a collection of 
 * {@link unlekker.modelbuilder.UVertexList UVertexList} objects, for 
 * instance to perform transformations. 
 *  
 * @author <a href="http://workshop.evolutionzone.com">Marius Watz</a> (portfolio: <a href="http://mariuswatz.com">mariuswatz.com</a>
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import unlekker.util.*;

public class UGeometry implements UConstants {
	public static String NONAME="No name";
	public static int USENORMALS=1,USEFACECOLOR=2,USEVERTEXCOLOR=8;
	public boolean doNoDuplicates=false;
	public String name;
	
	private int bvCnt,bvCol[],shapeType=-1;
	private UVec3 bv[];

	/**
	 * List of faces.
	 */
	public UFace face[];
	/**
	 * Current number of faces.
	 */
	public int faceNum;
	/**
	 * Array of vertex lists that are stored in this <code>UGeometry</code> object. This is only intended as a convenient
	 * way to collect vertex lists, for instance to perform collective transformations on them. The vertex lists may be drawn
	 * to screen using <code>drawVertexLists()</code>.
	 */
	public UVertexList vl[];
	
	public ArrayList<UGeometry> child=new ArrayList<UGeometry>();
	
	/**
	 * Vertex list containing all vertices used in this object. Manipulating 
	 * vertices directly will change the geometry.
	 */
	public UVertexList vert=new UVertexList();
	
	public UQuad quad[];
	public int quadNum;

	public UStrip strip[];
	public int stripNum;

	/**
	 * Number of vertex lists stored in this object.
	 */
	public int vln;
	/**
	 * Boundng box. 
	 */
	public UBBox bb;
	public float w,d,h;
	private UStrip shapeRecord;
	
	/** 
	 * Create unnamed instance.
	 */
	public UGeometry() {
		name=NONAME;
	}

	/** 
	 * Create named instance.
	 */
	public UGeometry(String _name) {
		name=_name;
	}
	
	/** 
	 * Create copy of existing UGeometry object.
	 */
	public UGeometry(UGeometry _g) {
		set(_g);
	}
	
	/**
	 * Creates an array of <code>n</code> Ugeometry objects and
	 * initalizes each position with an empty object.
	 * @param n Number of instances to create
	 * @return Array of initialized empty instances
	 */
	public static UGeometry [] getUGeometry(int n) {
		UGeometry [] g=new UGeometry[n];
		for(int i=0; i<n; i++) g[i]=new UGeometry();
		return g;
	}
	
	public static void draw(PApplet p,UGeometry g[]) {
		for(int i=0; i<g.length; i++) if(g[i]!=null) g[i].draw(p);
	}
	
	public void drawFaceNormals(PApplet p,float len) {
		p.beginShape(p.LINES);
		for(int i=0; i<faceNum; i++) {
			if(face[i].n==null) face[i].calcNormal(); 
			if(face[i].centroid==null) face[i].calcCentroid(); 
			p.vertex(face[i].centroid.x,face[i].centroid.y,face[i].centroid.z);
			p.vertex(
					face[i].centroid.x+face[i].n.x*len,
					face[i].centroid.y+face[i].n.y*len,
					face[i].centroid.z+face[i].n.z*len);
		}
		p.endShape();
	}

	/**
	 * Sets the contents of this UGeometry object by 
	 * copying the input geometry.  
	 * @param _g
	 */
	public void set(UGeometry _g) {
		reset();
		name=_g.name;

		if(_g.faceNum>0) {
			for(int i=0; i<_g.faceNum; i++) addFace(_g.face[i].getVertices());
			if(_g.bb!=null) calcBounds();
		}
//		UUtil.log("Added "+faceNum+" faces.");
		
		if(_g.vln>0) {			
			for(int i=0; i<_g.vln; i++) addVertexList(new UVertexList(_g.vl[i]));
		}
		calcBounds();
	}

	/**
	 * Resets geometry to empty.
	 */	
	public void reset() {
		faceNum=0;
		vln=0;
		bb=null;
    bvCnt=0;
    quadNum=0;
    stripNum=0;
    vert.reset();
	}

	/**
	 * Adds QUAD_STRIP of faces from a list of vertex pairs.
	 * @param vl Array of vertex pairs
	 * @param reverseOrder Build in reverse order?
	 */
	public UGeometry quadStrip(UVertexList vl) {
		return quadStrip(vl, false);
	}
	
	/**
	 * Adds QUAD_STRIP of faces from a list of vertex pairs.
	 * @param vl Array of vertex pairs
	 * @param reverseOrder Build in reverse order?
	 */
	public UGeometry quadStrip(UVertexList vl, boolean reverseOrder) {
		int id=0,numv=vl.n/2;

		beginShape(QUAD_STRIP);
		if(reverseOrder) {
			id=vl.n-2;
			for(int j=0; j<numv; j++) {
				vertex(vl.v[id]);
				vertex(vl.v[id+1]);
				id-=2;
			}
		}
		else for(int j=0; j<numv; j++) {
			vertex(vl.v[id++]);
			vertex(vl.v[id++]);
		}
		
		endShape();
		return this;
	}

	/**
	 * Adds QUAD_STRIP of faces built from two vertex lists. 
	 * @param v1 First edge of QUAD_STRIP
	 * @param v2 Second edge of QUAD_STRIP
	 */
	public UGeometry quadStrip(UVertexList v1,UVertexList v2) {
//		UUtil.log("quadStrip(v1,v2) "+v1.n+" "+v2.n);
		return quadStrip(v1,v2,false);
	}
	
	/**
	 * Adds QUAD_STRIP of faces built from two vertex lists. 
	 * @param v1 First edge of QUAD_STRIP
	 * @param v2 Second edge of QUAD_STRIP
	 * @param reverse Build in reverse order?
	 */
	public UGeometry quadStrip(UVertexList v1,UVertexList v2,boolean reverse) {
		int numv=v1.n;
		int id=0;

		if(v1.doColor && v2.doColor) { // render with vertex color
//			UUtil.log("render with vertex color");
			beginShape(QUAD_STRIP);
			if(reverse) {
				id=numv-1;
				for(int j=0; j<numv; j++) {
					vertex(v1.v[id],v1.vertexCol[id]);
					vertex(v2.v[id],v2.vertexCol[id]);
					id--;
				}
			}
			else {
				id=0;
				for(int j=0; j<numv; j++) {
					vertex(v1.v[id],v1.vertexCol[id]);
					vertex(v2.v[id],v2.vertexCol[id]);
					id++;
				}
			}
			endShape();
		}
		else {
//			UUtil.log("don't render with vertex color");
			beginShape(QUAD_STRIP);
			if(reverse) {
				id=numv-1;
				for(int j=0; j<numv; j++) {
					vertex(v1.v[id]);
					vertex(v2.v[id]);
					id--;
				}
			}
			else {
				id=0;
				for(int j=0; j<numv; j++) {
					vertex(v1.v[id]);
					vertex(v2.v[id]);
					id++;
				}
			}
			endShape();
		}
		
		return this;
	}
	
	/**
	 * Adds a mesh of QUAD_STRIPs built from an array of vertex lists.
	 * Each vertex list is treated as a single edge and will be connected to the next edge in the array.
	 * All vertex lists must have the same number of vertices. 
	 * @param vl Array of vertex lists to be used as edges.
	 */
	public UGeometry quadStrip(UVertexList vl[]) {
		return quadStrip(vl,vl.length);
	}
		

	public UGeometry quadStrip(UVertexList[] vl, boolean close) {
//		for(int i=0; i<vl.length; i++) UUtil.log(i+" "+vl[i].n+" "+UUtil.toString(vl[i].v));
		quadStrip(vl);
		if(close) quadStrip(vl[vl.length-1],vl[0]);
		return this;		
	}

	/**
	 * Adds a mesh of QUAD_STRIPs built from an array of vertex lists.
	 * Each vertex list is treated as a single edge and will be connected to the next edge in the array.
	 * All vertex lists must have the same number of vertices. 
	 * @param vl Array of vertex lists to be used as edges.
	 * @param vln Number of lists to use from array
	 */
	public UGeometry quadStrip(UVertexList vl[],int vln) {
		if(vln<2) return this;

		for(int i=0; i<vln-1; i++) if(vl[i]!=null && vl[i+1]!=null){
			quadStrip(vl[i], vl[i+1]);
		}		
		
		return this;
	}
	
	/**
	 * Adds a TRIANGLE_FAN constructed from input vertex list. If useCentroid==true then a centroid is calculated 
	 * and used as the center point for the fan, if not the first vertex in the vertex list is used. 
	 * @param vl Array of vertex pairs
	 * @param useCentroid Build from calculated centroid?
	 * @param reverseOrder Build in reverse order?
	 * @return 
	 */
	public UGeometry triangleFan(UVertexList vl,boolean useCentroid,boolean reverseOrder) {
		UVec3 c=new UVec3();
		int vstart=0;
		
		if(vl.n<3) return this;
		
		if(useCentroid) {
			int nn=vl.n;
			if(vl.v[0].distanceTo(vl.v[vl.n-1])<0.1f) nn--;
			for(int i=0; i<nn; i++) c.add(vl.v[i]);
			c.div(nn);
		}		
		else {
			c=vl.v[0];
			vstart=1;
		}
		
		beginShape(TRIANGLE_FAN);
		vertex(c);
		if(reverseOrder) {
			for(int i=vl.n-1; i>vstart-1; i--) vertex(vl.v[i]);			
		}
		else {
			for(int i=vstart; i<vl.n; i++) vertex(vl.v[i]);
		}
		endShape();
		
		return this;
	}
	
	/**
	 * Sweep a 2D profile along a 2D path to make a 3D sweeped object. 
	 * Assumes a UVertexList path in the XZ plane and a profile in the XY plane.
	 * @param prof
	 * @param path
	 * @return
	 */
	public UGeometry sweepXZ(UVertexList prof,UVertexList path) {
		float a[]=new float[path.n];
		UVec3 vv=new UVec3();
		UVertexList pp[]=new UVertexList[path.n];
		
		for(int i=0; i<path.n-1; i++) {
			vv.set(path.v[i+1]).sub(path.v[i]);//.rotateY(-HALF_PI);
			vv.set(-vv.z,vv.x).norm();

			a[i]=-vv.angle2D();
		}
		
		a[path.n-1]=a[path.n-2];
		
		for(int i=0; i<path.n; i++) {
			pp[i]=new UVertexList(prof);
			pp[i].rotateY(-a[i]).translate(path.v[i]);
		}

		for(int i=0; i<path.n-1; i++) {
			beginShape(QUAD_STRIP);
			for(int j=0; j<prof.n; j++) {
				vertex(pp[i].v[j]);
				vertex(pp[i+1].v[j]);
			}
			endShape();
		}

		return this;
	}
	
	///////////////////////////////////////////////////
	// BEGINSHAPE / ENDSHAPE METHODS
	
	
	/**
	 * Starts building a new series of faces, using the same logic 
	 * as <a href="http://processing.org/reference/beginShape_.html">PApplet.beginShape()</a>.
	 * Currently supports the following types: TRIANGLE_FAN, TRIANGLE_STRIP, TRIANGLES, QUADS, QUAD_STRIP
	 * 
	 * While shape is being built vertices are stored in a temporary 
	 * array, and only the ones that are used are copied to the vert vertexlist.
	 * @param _type Shape type (TRIANGLE_FAN, TRIANGLE_STRIP, TRIANGLES, QUADS, QUAD_STRIP)
	 */
	public UGeometry beginShape(int _type) {
		bvCnt=0;
		if(bv==null) bv=new UVec3[100];
		if(face==null) face=new UFace[100];
		
//		shapeRecord=new UStrip(_type);

		shapeType=_type;
		return this;
	}

	public UGeometry endShape() {
    switch (shapeType) {
      case TRIANGLE_FAN: {
      	UStrip s=new UStrip(TRIANGLE_FAN, 
      			this,(UVec3 [])UUtil.expandArray(bv, bvCnt));
      	add(s);
//        }
      }
      break;

      case TRIANGLES: {
        int stop = bvCnt - 2;
        for (int i = 0; i < stop; i += 3) 
//        	addFace(bv[i], bv[i+2], bv[i+1]);
        	addFace(new UVec3[] {bv[i], bv[i+2], bv[i+1]});
      }
      break;

      case TRIANGLE_STRIP: {
        int stop = bvCnt - 2;
        for (int i = 0; i < stop; i++) {
        	// HANDED-NESS ISSUE
//        	if(i%2==1) addFace(bv[i], bv[i+2], bv[i+1]);
//        	else addFace(bv[i], bv[i+1], bv[i+2]);
        	if(i%2==1) addFace(new UVec3[] {bv[i], bv[i+2], bv[i+1]});
        	else addFace(new UVec3[] {bv[i], bv[i+1], bv[i+2]});
        }
      }
      break;

      // Processing order: bottom left,bottom right,top right,top left
//      addTriangle(i, i+1, i+2);
//      addTriangle(i, i+2, i+3);
      case QUADS: {
        int stop = bvCnt-3;
        for (int i = 0; i < stop; i += 4) {
        	addFace(new UVec3[] {bv[i],bv[i+1],bv[i+2],bv[i+3]});
        }
      }
      break;

      case QUAD_STRIP: {
      	UStrip s=new UStrip(QUAD_STRIP, 
    			this,(UVec3 [])UUtil.expandArray(bv, bvCnt));
        add(s);
      }
      break;

      case POLYGON:{
      	UUtil.log("beginShape(POLYGON) currently unsupported.");
//      	UUtil.log("beginShape(POLYGON) "+bvCnt+" "+bv.length);
//      	if(bvCnt!=bv.length) bv=(Vec3 [])UUtil.resizeArray(bv, bvCnt);
//      	
//      	Vec3 tri[]=Triangulate.triangulate(bv); 
//      	if(tri!=null && tri.length>0) {
//      		int id=0;
//      		for(int i=0; i<tri.length/3; i++) {
//      			addFace(tri[id++],tri[id++],tri[id++]);
//      		}
//
//      	}
//        addPolygonTriangles();
      }
      break;
    }
    bvCnt=0;

//		UUtil.log("Faces: "+faceNum);
		return this;

	}
	
	/**
	 * Add vertex to shape being built by <code>beginShape() / endShape()</code>
	 * @param x
	 * @param y
	 * @param z
	 * @return 
	 */
	public UGeometry vertex(float x,float y,float z) {
		vertex(new UVec3(x,y,z));
		return this;
	}

	/**
	 * Add UVec3 vertex to shape being built by <code>beginShape() / endShape()</code>
	 * The vertex information is copied, leaving the original UVec3 instance unchanged.
	 * @param v
	 * @return 
	 */
	public UGeometry vertex(UVec3 v) {
		if(bv.length==bvCnt) bv=(UVec3 [])UUtil.expandArray(bv);
		bv[bvCnt]=new UVec3(v);
//		if(shapeRecord!=null) shapeRecord.add(bv[bvCnt]);
		
		bvCnt++;
		return this;
	}

	public UGeometry vertex(UVec3 v, int col) {
		if(bvCol==null) bvCol=new int[bv.length];
		vertex(v);
		if(bvCol.length==bvCnt) bvCol=UUtil.expandArray(bvCol); 
		bvCol[bvCnt-1]=col;
		return this;
	}

	/**
	 * Add vertex list to shape being built by <code>beginShape() / endShape()</code>. 
	 * All vertices are copied, leaving the original instances unchanged.
	 * @param vl 
	 * @param reverseOrder Add in reverse order?
	 * @return 
	 */
	public UGeometry vertex(UVertexList vl,boolean reverseOrder) {
		vertex(vl.v,vl.n, reverseOrder);
		return this;
	}

	/**
	 * Adds vertex list to shape being built by <code>beginShape() / endShape()</code>. 
	 * All vertices are copied, leaving the original instances unchanged.
	 * @return 
	 */
	public UGeometry vertex(UVertexList vl) {
		return vertex(vl.v,vl.n, false);
	}

	/**
	 * Adds array of UVec3 vertices to shape being built by <code>beginShape() / endShape()</code>. 
	 * All objects are copied, leaving the original instances unchanged.
	 * @param _v Array of vertices
	 * @param nv Number of vertices to add
	 * @return 
	 */
	public UGeometry vertex(UVec3 _v[],int nv) {
		return vertex(_v,nv,false);
	}

	/**
	 * Adds array of UVec3 vertices to shape being built by <code>beginShape() / endShape()</code>. 
	 * All objects are copied, leaving the original instances unchanged.
	 * @param _v Array of vertices
	 * @param nv Number of vertices to add
	 * @param reverseOrder Add in reverse order?
	 */
	public UGeometry vertex(UVec3 _v[],int nv,boolean reverseOrder) {
		if(bv.length<bvCnt+nv) 
			bv=(UVec3 [])UUtil.expandArray(bv,bvCnt+nv+100);		
		
		if(shapeRecord!=null) {
			UVec3 vv;
			if(reverseOrder) {
				for(int i=nv-1; i>-1; i--) {
					vv=new UVec3(_v[i]);
					shapeRecord.add(vv);
					bv[bvCnt++]=vv;
				}
			}
			else {
				for(int i=0; i<nv; i++) {
					vv=new UVec3(_v[i]);
					shapeRecord.add(vv);
					bv[bvCnt++]=vv;
				}
			}
		}
		else {
			if(reverseOrder) {
				for(int i=nv-1; i>-1; i--) bv[bvCnt++]=new UVec3(_v[i]);
			}
			else {
				for(int i=0; i<nv; i++) bv[bvCnt++]=new UVec3(_v[i]);
			}
		}
		
		
		return this;
	}

	
	/**
	 * Adds single face or quad. If <code>vv.length==3</code> a UFace will be
	 * added, otherwise a UQuad will be added.
	 * @param vv Array of 3 or 4 vertices, depending on whether face is a triangle
	 * or quad.
	 * @return  
	 */
	public int addFace(UVec3 vv[]) {
		int type=TRIANGLE;
		if(vv.length==4) type=QUAD;
		
		int id[]=addVerticesToMasterList(vv);
//		UUtil.log(UUtil.toString(vv)+" "+UUtil.toString(id));
		
		if(type==TRIANGLE) {
			if(face==null) face=new UFace[100];
			if(face.length==faceNum) face=(UFace[])UUtil.expandArray(face);
			face[faceNum++]=new UFace(this,id);
			return faceNum-1;
		}
		else {			
			add(new UQuad(this,vv));
//			UUtil.log("quadNum "+quadNum+" "+faceNum+" "+UUtil.toString(vv));
			return quadNum-1;
		}


	}

	/**
	 * Adds a UVertexList object to the <code>vl</code> array. The object 
	 * is stored by reference, so any changes made to it through this class will
	 * also affect the original instance.  
	 * Not to be confused with {@link #vertex(UVertexList vl)}  
	 * @param vv 
	 * @return
	 */
	public UGeometry addVertexList(UVertexList vv) {
		if(vl==null) vl=new UVertexList[10];
		else if(vln==vl.length) vl=(UVertexList[])UUtil.expandArray(vl);
		vl[vln++]=vv;
		
		return this;
	}

	public UGeometry unflatten(float data[]) {
		return rotateZ(data[6]).rotateY(-data[5]).rotateZ(data[4]).
				rotateY(data[3]).translate(data[0], data[1], data[2]);
	}

	public UGeometry flatten() {
		return flatten(-1);
	}
	
	public UGeometry flatten(float rowW) {
		for(int i=0; i<faceNum; i++) face[i].flatten2();
		
		if(rowW<0) return this;
		
		for(int i=0; i<faceNum; i++) {
			float x=0,rowX=0,rowY=0,y=0,ymax=0;
			UVec3 dim=face[i].getDimensions();
//			UUtil.log(dim.toString()+" "+UUtil.toString(face[i].v));
			x=dim.x;
			y=PApplet.max(y,dim.y);
			
			if(rowX+x>rowW) {
				rowX=0;
//				x=0;
				rowY+=y+5;
				y=0;
			}
			
			face[i].translate(rowX,rowY,0);
			rowX+=x+5;
		}
		return this;
	}
	
	public UGeometry noDuplicates() {
		doNoDuplicates=true;
		return this;
		
	}
	
	public void removeDuplicateFaces() {
		boolean ok;
		int dupes=0;
		
		for(int i=1; i<faceNum; i++) {
			for(int j=i; j<faceNum; j++) {
				if(i!=j && face[j]!=null && face[i]!=null &&
						face[i].compareTo(face[j])==0) {
//					UUtil.log("Duplicate found."+face[j].toString());
					dupes++;
					face[j]=null;
//					face[j].translate(0, 0, UUtil.rnd.random(-100, 100));
				}
			}
		}
		
		ArrayList<UFace> ff=new ArrayList<UFace>();
		
		for(int i=0; i<faceNum; i++) {
			if(face[i]!=null) ff.add(face[i]);
		}

		faceNum=0;
		for(UFace newf:ff) add(newf); 
		
		UUtil.log("Duplicates found: "+dupes);//face[j].toString());
	}
	

	public UGeometry addChild(UGeometry sub) {
		child.add(sub);
		return this;
	}
		
	/**
	 * Adds all the faces of a UGeometry instance. The faces is copied and the original instance is left unchanged. 
	 * @param g UGeometry to add
	 * @return
	 */
	public UGeometry add(UGeometry g) {
		
		int added[]=new int[g.faceNum];
		for(int i=0; i<g.faceNum; i++) added[i]=-1;
		
//		if(g.stripNum>0) {
//			for(int i=0; i<g.stripNum; i++) {
//				add(new UStrip(this,g.strip[i]));
//				UStrip s=strip[stripNum-1];
//				for(int q=0; q<s.quadNum; q++) {
//					String str=""+s.quad[q];
////					UUtil.log(q+"/"+g.quadNum+" "+s.quad[q].fid1+" "+s.quad[q].fid2+" / "+faceNum+
////							" "+str);
//					
//					added[g.quad[q].fid1]=1;
//					added[g.quad[q].fid2]=1;
//				}
//			}			
//		}

//		UUtil.log(UUtil.toString(added));
		
		for(int i=0; i<g.faceNum; i++) 
			if(added[i]<0) addFace(g.face[i].getVertices());
		
		
		return this;
	}
	
	public UGeometry add(UGeometry[] models) {
		for(int i=0; i<models.length; i++) 
			if(models[i]!=null) add(models[i]);
		return this;
	}


	
	/**
	 * Adds single vertex to master vertex list and returns the generated
	 * vertex ID.
	 * @param v
	 * @return Vertex ID
	 */
	public int addVertexToMasterList(UVec3 vv) {
		if(vert==null) vert=new UVertexList();
		if(doNoDuplicates) vert.doNoDuplicates=true;
		int id=vert.addGetID(vv);
		return id;
	}
	
	/**
	 * Adds array of vertices to master vertex list and returns the generated
	 * vertex IDs
	 * @param v
	 * @return Array of vertex IDs
	 */
	public int [] addVerticesToMasterList(UVec3 vv[]) {
		if(vert==null) vert=new UVertexList();
		if(doNoDuplicates) vert.doNoDuplicates=true;
		int id[]=new int[vv.length];
		for(int i=0; i<vv.length; i++) id[i]=vert.addGetID(vv[i]);
		return id;
	}
	
	/**
	 * Add single face by copying vertices from a UFace instance
	 * @param f
	 */
	public void add(UFace f) {
		if(f==null) return;
		addFace(f.getVertices());		
	}
	
	public void add(UFace[] f) {
		if(f==null) return;
		for(int i=0; i<f.length; i++) if(f[i]!=null) add(f[i]);
	}

	
	public void add(UQuad uQuad) {
		if(quad==null) quad=new UQuad[100];
		if(quad.length==quadNum) quad=(UQuad [])UUtil.expandArray(quad);
		quad[quadNum++]=uQuad;
	}

	public void drawChildren(PApplet p) {
		for(UGeometry sub:child) sub.draw(p);
	}
	

	public void draw(PApplet p) {
		draw(p.g,-1);
	}
	

	   /**
     * Draws all faces contained in this UGeometry object.
     * @param p Reference to PApplet instance to draw into
     */

    public void draw(PApplet p,int opt) {
      draw(p.g,opt);
    }
    
	/**
	 * Draws all faces contained in this UGeometry object.
	 * @param p Reference to PApplet instance to draw into
	 */

	public void draw(PGraphics g,int opt) {
	  boolean useNormals= false,useFaceColor= false,useVColor= false;
	  UFace f;
		int fid=0;
		UVec3 vv;

		if(opt>0) {
          useNormals=((opt & USEFACECOLOR)>0);
          useFaceColor=((opt & USEFACECOLOR)>0);
          useVColor=((opt & USEVERTEXCOLOR)>0);
		}
		
		if(g.getClass().getSimpleName().equals("PGraphicsJava2D")) {
			g.beginShape(TRIANGLES);
			for(int i=0; i<faceNum; i++) {			
				f=face[i];
				if(useFaceColor) g.fill(f.c);
				
				fid=0;
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y);
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y);
				vv=vert.v[f.vid[fid]];
				g.vertex(vv.x,vv.y);
			}
			g.endShape();

			return;
		}
		
		g.beginShape(TRIANGLES);
		if(useNormals) 
			for(int i=0; i<faceNum; i++) {			
				f=face[i];
				if(useFaceColor) g.fill(f.c);
				
				fid=0;
				g.normal(f.n.x, f.n.y, f.n.z);
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid]];
				g.vertex(vv.x,vv.y,vv.z);
			}
		else if(useVColor) {
			for(int i=0; i<faceNum; i++) {			
				f=face[i];				
				fid=0;
				vv=vert.v[f.vid[fid++]];
				g.fill(vv.col);
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid++]];
				g.fill(vv.col);
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid]];
				g.fill(vv.col);
				g.vertex(vv.x,vv.y,vv.z);
			}
		}
		else 
			for(int i=0; i<faceNum; i++) {			
				f=face[i];
        if(useFaceColor) g.fill(f.c);

                fid=0;
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid++]];
				g.vertex(vv.x,vv.y,vv.z);
				vv=vert.v[f.vid[fid]];
				g.vertex(vv.x,vv.y,vv.z);
			}
		
		g.endShape();		
	}

	/**
	 * CURRENTLY BROKEN.
	 * @param p
	 */
	public void drawFaceLabels(PApplet p) {
		UVec3 pos=new UVec3(),head;
		
		for(int i=0; i<faceNum; i++) {
			if(face[i].centroid==null)  face[i].calcCentroid();
			if(face[i].n==null) face[i].calcNormal();

			p.pushMatrix();
			pos.set(face[i].v[0]).add(face[i].v[1]).mult(0.5f);
			p.translate(pos.x,pos.y,pos.z);
			p.ellipse(0,0, 10,10);
			p.popMatrix();

			pos.set(face[i].n).norm(5).add(face[i].centroid);
			head=pos.getHeadingAngles(pos);


			p.pushMatrix();
			p.translate(pos.x,pos.y,pos.z);
//			p.rotateY(HALF_PI);
			p.rotateZ(-head.x);
			p.rotateY(-head.y);
			p.text("id=="+i,0,0);
			p.popMatrix();
		}
	}


/*	public void drawColor(PApplet p) {
		UFace f;
		int fid=0,id=0;
		float vArr[];

		p.beginShape(TRIANGLES);		
		for(int i=0; i<faceNum; i++) {			
			f=face[i];
			if(f.vArr==null) f.calcVertexArray();
			vArr=f.vArr;
			fid=0;
			if(f.c==null) { 
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
			}
			else { 
				p.fill(f.c[0]);
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
				p.fill(f.c[1]);
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
				p.fill(f.c[2]);
				p.vertex(vArr[fid++],vArr[fid++],vArr[fid++]);
			}

		}
		p.endShape();		
	}*/
	
	/**
	 * Static convenience function to draw a quad strip to screen from
	 * two UVertexList objects.
	 * @param p
	 * @param vl1
	 * @param vl2
	 */
	public static void drawQuadstrip(PApplet p,UVertexList vl1,UVertexList vl2) {
		if(p.g.getClass().getSimpleName().equals("PGraphicsJava2D")) {
			p.beginShape(QUAD_STRIP);
			for(int i=0; i<vl1.n; i++) {
				p.vertex(vl1.v[i].x,vl1.v[i].y);
				p.vertex(vl2.v[i].x,vl2.v[i].y);			
			}
			p.endShape();
		}
		else { // is 3D
			p.beginShape(QUAD_STRIP);
			for(int i=0; i<vl1.n; i++) {
				p.vertex(vl1.v[i].x,vl1.v[i].y,vl1.v[i].z);
				p.vertex(vl2.v[i].x,vl2.v[i].y,vl2.v[i].z);			
			}
			p.endShape();
		}
	}

	/////////////////////////////////////////////////////////
	// TRANSFORMATIONS

	public UGeometry rotateX(float a) {
		vert.rotateX(a);
		for(int i=0; i<vln; i++) vl[i].rotateX(a);
		if(bb!=null) calcBounds();
		return this;
	}

	public UGeometry rotateY(float a) {
		vert.rotateY(a);
		for(int i=0; i<vln; i++) vl[i].rotateY(a);
		if(bb!=null) calcBounds();
		return this;
	}

	public UGeometry rotateZ(float a) {
		vert.rotateZ(a);
		for(int i=0; i<vln; i++) vl[i].rotateZ(a);
		if(bb!=null) calcBounds();
		return this;
	}

	public UGeometry translate(float x,float y,float z) {
		vert.translate(x,y,z);
		for(int i=0; i<vln; i++) vl[i].translate(x,y,z);
		if(bb!=null) bb.translate(x, y, z);
		return this;
	}
	
	public UGeometry translate(UVec3 vv) {
		translate(vv.x,vv.y,vv.z);
		return this;
	}

	/**
	 * Calculates bounding box and translates the mesh to origin by calling <code>translate(-bb.min.x,-bb.min.y,-bb.min.z);</code>
	 */
	public UGeometry toOrigin() {
		if(bb==null) calcBounds();
		translate(-bb.min.x,-bb.min.y,-bb.min.z);
		return this;
	}


	public UGeometry scale(float m) {
		scale(m,m,m);
		if(bb!=null) bb.scale(m,m,m);
		return this;
	}

	public UGeometry scale(float mx,float my,float mz) {
		vert.scale(mx,my,mz);
		for(int i=0; i<vln; i++) vl[i].scale(mx,my,mz);
		if(bb!=null) bb.scale(mx,my,mz);
		return this;
	}

	/**
	 * Convenience method to produce a copy of this UGeometry instance. 
	 * @return Copy of UGeometry
	 */
	public UGeometry getCopy() {
		UGeometry g;
		
		g=new UGeometry(this);
		return g;
	}


	public static UGeometry extrudeNonFlat(
			UVertexList vl1,UVertexList vl2,
			float z,boolean reverse){
		UGeometry o1=null,o2;
		UVertexList vl[]=new UVertexList[4];
		UVertexList n1=new UVertexList();
		UVertexList n2=new UVertexList();
		UVec3 n=new UVec3();
		
		z*=0.5f;
		
		for(int i=0; i<vl1.n; i++) {
			if(i==0) {
				n1.add(UVec3.calcFaceNormal(vl1.v[1], vl1.v[0],vl2.v[0]).mult(1));
				n2.add(UVec3.calcFaceNormal(vl2.v[1], vl2.v[0], vl1.v[1]).mult(1));
			}
//			else if(i==vl1.n-1) {
//				n1.add(UVec3.calcFaceNormal(vl1.v[i], vl1.v[i-1],vl2.v[i]));
//				n2.add(UVec3.calcFaceNormal(vl2.v[i], vl2.v[i-1], vl1.v[1]));				
//			}
			else {
				n=UVec3.calcFaceNormal(vl1.v[i], vl1.v[i-1],vl2.v[i]);
				n1.add(n);
				n=UVec3.calcFaceNormal(vl2.v[i], vl2.v[i-1], vl1.v[i]);
				n2.add(n);
			}
		}
		
		for(int i=0; i<4; i++) vl[i]=new UVertexList();
		for(int i=0; i<vl1.n; i++) {
			n.set(n1.v[i]).mult(-z).add(vl1.v[i]);
			vl[0].add(n);
			n.set(n2.v[i]).mult(-z).add(vl1.v[i]);
			vl[1].add(n);
			
			n.set(n1.v[i]).mult(z).add(vl2.v[i]);
			vl[2].add(n);
			n.set(n2.v[i]).mult(z).add(vl2.v[i]);
			vl[3].add(n);
		}
		
		o1=new UGeometry();
		o1.quadStrip(vl[1],vl[0]);
		o1.quadStrip(vl[2],vl[3]);
		o1.quadStrip(vl[2],vl[1]);
		o1.quadStrip(vl[0],vl[3]);
		
		o1.beginShape(QUAD);
		o1.vertex(vl[2].v[0]);
		o1.vertex(vl[3].v[0]);
		o1.vertex(vl[0].v[0]);
		o1.vertex(vl[1].v[0]);
		o1.endShape();

		o1.beginShape(QUAD);
		o1.vertex(vl[1].v[vl[0].n-1]);
		o1.vertex(vl[0].v[vl[0].n-1]);
		o1.vertex(vl[3].v[vl[0].n-1]);
		o1.vertex(vl[2].v[vl[0].n-1]);
		o1.endShape();

//		o1.beginShape(QUAD_STRIP);
//			.quadStrip(vl[2], vl[3], true);
//			.quadStrip(vl[1],vl[2])
//			.quadStrip(vl[3],vl[0],true);
		return o1;
	}

	/**
	 * Produces a UGeometry mesh by extruding a QUAD_STRIP along its face 
	 * normal. The input is a list of vertices making up the QUAD_STRIP. The 
	 * face normal of the first quad is used to calculate the extrusion,  
	 * multiplied by the parameter <code>z</code>. All vertices are presumed to be co-planar.  
	 * @param vl Vertex list defining a QUAD_STRIP mesh
	 * @param z Offset to extrude by along the face normal, can be positive or negative
	 * @param reverse Construct in reverse order? Useful in case face normals of 
	 * resulting mesh is incorrect. 
	 * @return
	 */
	public static UGeometry extrude(UVertexList vl,float z,boolean reverse){
		UVertexList vl2;
		vl=new UVertexList(vl);
		vl2=new UVertexList(vl);
		if(reverse) {
			vl.reverseOrder();
			vl2.reverseOrder();
		}
		
		int n=vl.n/2,id=0;
		UGeometry g=new UGeometry();
		UVec3 vv,offs;
		
		
		offs=UVec3.crossProduct(vl.v[1].x-vl.v[0].x,
				vl.v[1].y-vl.v[0].y,
				vl.v[1].z-vl.v[0].z,
				vl.v[2].x-vl.v[0].x,
				vl.v[2].y-vl.v[0].y,
				vl.v[2].z-vl.v[0].z).norm(z);

//		UUtil.log("extrude - normal "+offs.toString());
//		UUtil.log(vl.toDataString());

		offs.mult(0.5f);
		vl.translate(-offs.x,-offs.y,-offs.z);
		g.quadStrip(vl,reverse);

		vl2.translate(offs);
		g.quadStrip(vl2, !reverse);
		
//		vl.QStoOutline();
//		vl.add(vl.v[0]);
//		vl2.QStoOutline();
//		vl2.add(vl2.v[0]);
		g.quadStrip(vl2,vl,!reverse);
		
		
		return g;
	}

	public UGeometry calcBounds() {
		if(bb==null) bb=new UBBox();
		bb.calc(this);
		return this;
 	}

	public UGeometry calcFaceNormals() {
		for(int i=0; i<faceNum; i++) face[i].calcNormal();
		return this;
	}

	public UGeometry calcFaceCentroids() {
		for(int i=0; i<faceNum; i++) face[i].calcCentroid();
		return this;
	}

	/**
	 * Calculates bounding box and centers all faces by calling <code>translate(-bb.c.x,-bb.c.y,-bb.c.z)</code>
	 */
	public UGeometry center() {
		calcBounds();
		return translate(-bb.c.x,-bb.c.y,-bb.c.z);		
	}

	public UGeometry setDimensions(float m) {
		calcBounds();
		scale(m/bb.maxDimension);
		return this;
	}

	public UGeometry setDimensionsXZ(float m) {
		calcBounds();
		float dim=bb.sz.x;
		if(bb.sz.z>dim) dim=bb.sz.z;
		
		scale(m/dim);
		return this;
	}

	public UGeometry setDimensionsXY(float m) {
		calcBounds();
		float dim=bb.sz.x;
		if(bb.sz.y>dim) dim=bb.sz.y;
		
		scale(m/dim);
		return this;
	}

	public UGeometry setDimensionsYZ(float m) {
		calcBounds();
		float dim=bb.sz.z;
		if(bb.sz.y>dim) dim=bb.sz.y;
		
		scale(m/dim);
		return this;
	}

	/**
	 * Output binary STL file of mesh geometry.
	 * @param p Reference to PApplet instance
	 * @param filename Name of file to save to
	 */
	public void writeSTL(PApplet p,String filename) {
  	byte [] header;
  	ByteBuffer buf;
  	UFace f;
  	
    try {
    	if(!filename.toLowerCase().endsWith("stl")) filename+=".stl";
//    	FileOutputStream out=(FileOutputStream)IO.getOutputStream(filename);
    	FileOutputStream out=(FileOutputStream)UIO.getOutputStream(p.sketchPath(filename));

  		buf = ByteBuffer.allocate(200);
  		header=new byte[80];
  		buf.get(header,0,80);
    	out.write(header);
  		buf.rewind();

  		buf.order(ByteOrder.LITTLE_ENDIAN);
  		buf.putInt(faceNum);
  		buf.rewind();
  		buf.get(header,0,4);
    	out.write(header,0,4);
  		buf.rewind();
  		
  		UUtil.logDivider("Writing STL '"+filename+"' "+faceNum);

    	buf.clear();
    	header=new byte[50];
    	if(bb!=null) UUtil.log(bb.toString());
    	
			for(int i=0; i<faceNum; i++) {
				f=face[i];
				if(f.n==null) f.calcNormal();
				
				buf.rewind();
				buf.putFloat(f.n.x);
				buf.putFloat(f.n.y);
				buf.putFloat(f.n.z);
				
				for(int j=0; j<3; j++) {
					buf.putFloat(f.v[j].x);
					buf.putFloat(f.v[j].y);
					buf.putFloat(f.v[j].z);
				}
				
				buf.rewind();
				buf.get(header);
				out.write(header);
			}

			out.flush();
			out.close();
			UUtil.log("Closing '"+filename+"'. "+faceNum+" triangles written.\n");
		} catch (Exception e) {
			e.printStackTrace();
		}          
  }

	/**
	 * Output binary STL file of mesh geometry.
	 * @param p Reference to PApplet instance
	 * @param filename Name of file to save to
	 */
	public static void writeSTL(PApplet p,String filename,UGeometry geo[]) {
  	byte [] header;
  	ByteBuffer buf;
  	UFace f;
  	int fn=0;
  	
    try {
//    	FileOutputStream out=(FileOutputStream)IO.getOutputStream(filename);
    	FileOutputStream out=(FileOutputStream)UIO.getOutputStream(p.sketchPath(filename));

    	for(int k=0; k<geo.length; k++) if(geo[k]!=null) fn+=geo[k].faceNum;

  		buf = ByteBuffer.allocate(200);
  		header=new byte[80];
  		buf.get(header,0,80);
    	out.write(header);
  		buf.rewind();

  		buf.order(ByteOrder.LITTLE_ENDIAN);
  		buf.putInt(fn);
  		buf.rewind();
  		buf.get(header,0,4);
    	out.write(header,0,4);
  		buf.rewind();
  		
  		UUtil.logDivider("Writing STL '"+filename+"' "+fn);

    	for(int k=0; k<geo.length; k++) if(geo[k]!=null) {
    		UGeometry g=geo[k];
    		
      	buf.clear();
      	header=new byte[50];
//      	if(bb!=null) UUtil.log(g.bb.toString());
      	
  			for(int i=0; i<g.faceNum; i++) {
  				f=g.face[i];
  				if(f.n==null) f.calcNormal();
  				
  				buf.rewind();
  				buf.putFloat(f.n.x);
  				buf.putFloat(f.n.y);
  				buf.putFloat(f.n.z);
  				
  				for(int j=0; j<3; j++) {
  					buf.putFloat(f.v[j].x);
  					buf.putFloat(f.v[j].y);
  					buf.putFloat(f.v[j].z);
  				}
  				
  				buf.rewind();
  				buf.get(header);
  				out.write(header);
  			}
    	}

			out.flush();
			out.close();
			UUtil.log("Closing '"+filename+"'. "+fn+" triangles written.\n");
		} catch (Exception e) {
			e.printStackTrace();
		}          
  }

/*  public static UGeometry readData(UDataText data) {
  	UGeometry geo=new UGeometry();
  	data.parseSkipLine();
  	int n=UUtil.parseInt(data.parseGetLine());
  	UUtil.log("Reading UGeometry from DataText - "+n+" faces.");
  	for(int i=0; i<n; i++) geo.addFace(UFace.fromDataString(data.parseGetLine()));
  	UUtil.log("UGeometry "+geo.faceNum+" faces.");
  	data.parseSkipLine();
  	return geo;
  }
*/
	
	public void writeData(UDataText data) {
		data.addDivider("UGeometry - "+faceNum+" faces.");
		data.add(faceNum).endLn();
		for(int i=0; i<faceNum; i++) data.add(face[i].toDataString()).endLn();
		data.endBlock();
	}
	
	public void writePOVRayMesh(PApplet p,String filename) {
		StringBuffer strbuf;
		PrintWriter outWriter;
		Writer outStream;
		String pre="triangle {<",div=">,<",end=">}";
		UFace ff;
  	int num,perc,lastperc=-1,step=5,stepMult=20;

		strbuf=new StringBuffer();
		try {
			outStream=new OutputStreamWriter(
					UIO.getOutputStream(filename,false));		
			outWriter=new PrintWriter(outStream);
			
			outWriter.println("#declare UGeometryMesh = mesh {");
    	if(faceNum>1000000) {step=1; stepMult=100;}
    	
			for (int i=0; i<faceNum; i++) {
				ff=face[i];
				
				strbuf.setLength(0);
				strbuf.append(pre).
				append(UUtil.nf(ff.v[0].x)).append(',').
				append(UUtil.nf(ff.v[0].y)).append(',').
				append(UUtil.nf(ff.v[0].z)).append(div);
				strbuf.
				append(UUtil.nf(ff.v[1].x)).append(',').
				append(UUtil.nf(ff.v[1].y)).append(',').
				append(UUtil.nf(ff.v[1].z)).append(div);
				strbuf.
				append(UUtil.nf(ff.v[2].x)).append(',').
				append(UUtil.nf(ff.v[2].y)).append(',').
				append(UUtil.nf(ff.v[2].z)).append(end);
				
				outWriter.println(strbuf.toString());
	  		perc=(int)(stepMult*(float)i/(float)(faceNum-1));
	  		if(perc!=lastperc) {
	  			lastperc=perc;
	  			System.out.println(UUtil.nf(lastperc*step,2)+"% | "+(i+1)+" triangles written.");//f[i]);
	  		}
			}
			
			outWriter.println("}");
			outWriter.flush();
			outStream.close();		
			

//		  UUtil.log("Saved '"+filename+"' "+numStr);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("writePOVRayMesh failed: "+e.getMessage());
		}
	  catch (Exception e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
	  }
		
	}
	
  /////////////////////////////////////////////
  // FUNCTIONS FOR STL INPUT
  
  public static UGeometry readSTL(PApplet p,String path) {
  	byte [] header,byte4;
  	ByteBuffer buf;
  	int num=0,step,stepMult;
  	float vv[]=new float[12];
    File file=null;
    String filename;
    UGeometry geo=null;
    
    UProgressInfo progress=new UProgressInfo();
    float lastPerc=-5;

		header=new byte[80];
		byte4=new byte[4];

    try { 
			if (path != null) {
			  filename=path;
			  file = new File(path);
			  if (!file.isAbsolute()) file=new File(p.savePath(path));
			  if (!file.isAbsolute()) 
			    throw new RuntimeException("RawSTLBinary requires an absolute path " +
			    "for the location of the input file.");
			}
			
			FileInputStream in=new FileInputStream(file);
    	System.out.println("\n\nReading "+file.getName());
			
			in.read(header);
			
  		// test if ASCII STL
  		String asciiTest="";
  		for(int i=0; i<5; i++) asciiTest+=(char)header[i];
  		
  		if(asciiTest.compareTo("solid")==0) { // IS ASCII STL
  			String dat;
  			
  			in.close();
      	geo=new UGeometry();
  			
  			BufferedReader read = new BufferedReader(new FileReader(path));  			
  			dat=read.readLine();
  			UUtil.log(dat);
  			
  			UVec3 v[]=UVec3.getVec3(3);
  			String tok[],FACET="facet";
  			
  			while(dat!=null) {
  				dat=read.readLine(); // should be "facet"
  				if(dat==null || dat.indexOf(FACET)==-1) { // error or "endsolid"
  	  			UUtil.log(dat);
  					dat=null;
  					read.close();
  				}
  				else {
    				dat=read.readLine(); // "outer loop"
    				
    				for(int i=0; i<3; i++) {
  	  				dat=read.readLine().trim();  // "vertex x y z"
//    	  			UUtil.log(dat);
  	  				tok=dat.split(" ");
  	  				v[i].set(
  	  						UUtil.parseFloat(tok[1]),
  	  						UUtil.parseFloat(tok[2]),
  	  						UUtil.parseFloat(tok[3]));
    				}
    				
    				geo.addFace(v);
    				if(geo.faceNum%1000==0) UUtil.log(geo.faceNum+" faces read.");
    				
    				dat=read.readLine(); // "end loop"
    				dat=read.readLine(); // "end facet"
  				}  				
  			}
  		}
  		else { // BINARY STL
  			in.read(byte4);
    		buf = ByteBuffer.wrap(byte4);
    		buf.order(ByteOrder.nativeOrder());
    		num=buf.getInt();
    		
    		UUtil.log("Polygons to read: "+num);

      	header=new byte[50];    	
      	
      	geo=new UGeometry();

      	progress.start();
    		int id=0;
  			for(int i=0; i<num; i++) {
  				in.read(header);
  				buf = ByteBuffer.wrap(header);
  	  		buf.order(ByteOrder.nativeOrder());
  	  		buf.rewind();
  	  		
  	  		for(int j=0; j<12; j++) vv[j]=buf.getFloat();
		  		id=3;
		  		geo.addFace(
	  				new UVec3[] {
		  				new UVec3(vv[id++],vv[id++],vv[id++]),
		  				new UVec3(vv[id++],vv[id++],vv[id++]),
		  				new UVec3(vv[id++],vv[id++],vv[id++])
	  				});
	  		
		  		progress.update(p, 100f*(float)i/(float)(num-1));
		  		if(progress.perc-lastPerc>5 || progress.perc>99.9f) {
  	  			lastPerc=progress.perc;
  	  			UUtil.log(geo.faceNum+" faces read, "+progress.lastUpdate);
		  		}
  			}
  		} // END READ BINARY

  		if(num>0)
  			UUtil.log("Faces read: "+geo.faceNum+" ("+num+" reported in file)");
  		else 
  			UUtil.log("Faces read: "+geo.faceNum);

    } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    return geo;
    
  }

	public static float triangleArea(UVec3 v1,UVec3 v2,UVec3 v3) {
		float p,a,b,c,val=0;
		
		// Heron's formula 
		// http://www.mathopenref.com/heronsformula.html
		a=UVec3.dist(v1, v2);
		b=UVec3.dist(v1,v3);
		c=UVec3.dist(v2,v3);
		p=(a+b+c)*0.5f;
		val=(float)Math.sqrt(p*(p-a)*(p-b)*(p-c));
		
		return val;
	}

	public float surfaceArea() {
		int id=0;
		float val=0;
		
		for(int i=0; i<faceNum; i++) {
			val+=triangleArea(face[i].v[0],face[i].v[1],face[i].v[2]);
		}
		
		return val;
	}

	/**
	 * Convenience method to call {@link unlekker.modelbuilder.UVertexList.drawVertices() UVertexList.drawVertices()}
	 * on all vertex lists. NOTE: begin/endShape() are not called.
	 * @param p Reference to PApplet instance
	 */
	public void drawVertexLists(PApplet p) {
		for(int i=0; i<vln; i++) vl[i].draw(p);			
	}

	public void add(UStrip s) {
		if(strip==null) strip=new UStrip[100];
		if(strip.length==stripNum) strip=(UStrip [])UUtil.expandArray(strip);
		strip[stripNum++]=s;
		
//		UUtil.log("stripNum "+stripNum+" "+quadNum+" "+faceNum);
	}
	
	public UGeometry removeDuplicateVertices() {
		int id[][]=vert.removeDuplicates();
		if(bb!=null) calcBounds();
		
		for(int i=0; i<faceNum; i++) {
//			UUtil.log(i+" "+face[i].vid[0])
			face[i].vid[0]=id[1][face[i].vid[0]];
			face[i].vid[1]=id[1][face[i].vid[1]];
			face[i].vid[2]=id[1][face[i].vid[2]];
			face[i].getVertices();
		}
		
		return this;
	}

	/**
	 * Takes array of vertex indices and returns the matching UVec3
	 * instances from the <code>vert</code> vertex list. If the parameter <code>v</code>
	 * is not null it is filled with pointers to the matching instances, if
	 * not a new UVec3 array of <code>id.length</code> length.
	 * @param id Array of vertex IDs to match
	 * @param v Array to populate with instances, if <code>null</code>
	 * a new UVec3 array is created.
	 * @return Array of UVec3 instances matching vertex IDs
	 */
	public UVec3[] matchIDtoVertex(int[] id, UVec3[] v) {
		if(v==null) v=new UVec3[id.length];
		for(int i=0; i<id.length; i++) v[i]=vert.v[id[i]];
		
		return v;
	}


	public UFace [] getNonQuads() {
		UFace [] f=new UFace[100];
		int fn=0;
		boolean found;
		
		for(int i=0; i<faceNum; i++) {
			found=false;
			for(int j=0; j<quadNum && !found; j++) {
				if(quad[j].fid1==i || quad[j].fid2==i) found=true;
//				if(found) UUtil.log(i+" "+quad[j].fid1+" "+quad[j].fid2);
			}
			if(!found) {
				if(f.length==fn) f=(UFace [])UUtil.expandArray(f);
				f[fn++]=face[i];
			}
		}
		
//		UUtil.log("getNonQuads() "+fn+" found out of "+faceNum+" faces ("+quadNum+" quads)");
		if(fn==0) return null;
		return (UFace [])UUtil.resizeArray(f, fn);
	}
	
	
	

	public String toString() {
		String s="UGeometry: f="+faceNum+" q="+quadNum+" v="+vert.n+" children="+child.size();
		if(bb!=null) s+=" "+bb.toString();
		return s;
	}


}

