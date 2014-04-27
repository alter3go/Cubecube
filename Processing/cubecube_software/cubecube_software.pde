//Cubecube software
//Created by Kavi Laud, 2014
//Uses modelbuilder libraries
//Based on Scott Kildall's "Stacking Prims" sketch, 2014

//PRESS SPACEBAR to export your cubecube model!
//model will save to the sketch folder (Sketch>Show Sketch Folder)

import processing.serial.*;

import unlekker.util.*;
import unlekker.modelbuilder.*;
import unlekker.modelbuilder.filter.*;

//Select serial port here!
int serialPort = 0;

UNav3D nav;          // allows for mouse navigation
UGeometry model;

color c;
Boolean showAxes = true;

float primSize = 12.7f;
float primOffset = 0f;

int numRows = 8;
int numColumns = 8;

float boardXSize = (numRows * primSize) + (numRows * primOffset);
float boardYSize = (numColumns * primSize) + (numColumns * primOffset);

int numRandomPrims = 1;

int totalPrims = 0;

// How many "prims" we have stacked in each location
int[][] prims;

Serial myPort;
String serial_buffer = null;

public void setup() {
  size(900, 900, P3D);

  // instantiate and center camera
  nav = new UNav3D(this);
  nav.setTranslation(width/2, height/1.8, 550);
  nav.setRotation(.75,0,-.35);
  
  noStroke();
  model = new UGeometry();
  
  // Allocate the 2D array
  prims = new int[numRows][numColumns];

  // Two nested loops allow us to visit every spot in a 2D array.   
  // For every column I, visit every row J.
  // This initializes the array to zero, not necessary but shows 2D array traversal
  for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numColumns; j++) {
          prims[i][j] = 0;
      }
  }
  
  println(Serial.list());
  
  String portName = Serial.list()[serialPort];
  myPort = new Serial(this, portName, 9600);
  myPort.clear();
}

public void draw() {
  

  
  if (myPort.available() >= 4) {
    serial_buffer = myPort.readStringUntil((int) ')');
    //println(serial_buffer);
    if (serial_buffer != null) {
      String addsub = serial_buffer.substring(0, serial_buffer.indexOf('('));
      String row = serial_buffer.substring(serial_buffer.indexOf('(') + 1, serial_buffer.indexOf(','));
      String col = serial_buffer.substring(serial_buffer.indexOf(',') + 1, serial_buffer.indexOf(')'));
      println("Adding " + addsub + " blocks to row" + row + ", column" + col);
      int i_addsub = int(addsub);
      int i_row = int(row);
      int i_col = int(col);
      if (i_addsub > 0) {
        for (int k = 0; k < i_addsub; ++k) {
          addPrim(i_row, i_col);
        }
      }
      else {
        removePrims(i_row, i_col, -i_addsub);
      }
    }
  }
  
    background(0);

  nav.doTransforms();
  
  fill(255,100,50);
  lights();
  pointLight(100, 100, 100, width/2, height/2, 50);  //uncomment to 'aim' the light source
  model.draw(this);
}

public void keyPressed() {
  //-- save model as an STL
  if( key == ' ' ) {
    //  UGeometry gb = new
     model.writeSTL(this, "output.stl");
   }
  

  
  //
 
}

//-- Adds a primitive to (row, column). 
public void addPrim( int row, int column ) {
  doAddPrim(row, column, prims[row][column]++);
  totalPrims++;
}

public void doAddPrim(int row, int column, int z)
{
  UGeometry g;
  
  // Not sure why this is, but the primSize seems to need to be halved
  g = UPrimitive.box(primSize/2, primSize/2, primSize/2); 
  
  g.translate(new UVec3(column * (primSize+primOffset), row * (primSize+primOffset),z * primSize));
  
  // will center the "board"
  g.translate(new UVec3(-boardXSize/2,-boardYSize/2,0));
  
  model.add(g);
}

public void removePrims(int row, int column, int count)
{
  totalPrims -= count;
  prims[row][column] -= count;
  model.reset();
  for (int i = 0; i < numRows; ++i) {
    for (int j = 0; j < numColumns; ++j) {
      for (int k = 0; k < prims[i][j]; ++k) {
        doAddPrim(i, j, k);
      }
    }
  }
}
  


