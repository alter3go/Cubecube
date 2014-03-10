/**
 *  modelbuilder_stacking_prims
 *
 *  by Scott Kildall
 *
 *  Primitive-stacking code using modelbuilder libraries
 *
 *  Will arrange primitives in an 8x8 grid, like a chess board.
 *  Units are in mm
 *  
 *  "a" will add an 8x8 'layer' of blocks
 *  "r" will generate random blocks
 *  "spacebar" will export 'output.stl' to sketch folder
 *
 *
 *  -> Right now, only have support for cubes, but will add others later
 *  
 *  
 */
 
import processing.serial.*;

import unlekker.util.*;
import unlekker.modelbuilder.*;
import unlekker.modelbuilder.filter.*;

UNav3D nav;          // allows for mouse navigation
UGeometry model;

color c;
Boolean showAxes = true;

float primSize = 12.7f;
float primOffset = 0f;

int numRows = 2;
int numColumns = 2;

float boardXSize = (numRows * primSize) + (numRows * primOffset);
float boardYSize = (numColumns * primSize) + (numColumns * primOffset);

int numRandomPrims = 4;

int totalPrims = 0;

// How many "prims" we have stacked in each location
int[][] prims;

Serial myPort;
String serial_buffer = null;

public void setup() {
  size(1280, 720, P3D);

  // instantiate and center camera
  nav = new UNav3D(this);
  nav.setTranslation(width/2, height/2, 0);
  
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
  
  String portName = Serial.list()[5];
  myPort = new Serial(this, portName, 9600);
  myPort.clear();
}

public void draw() {
  if (myPort.available() > 0) {
    serial_buffer = myPort.readStringUntil((int) ')');
    println(serial_buffer);
    if (serial_buffer != null) {
      String addsub = serial_buffer.substring(0, serial_buffer.indexOf('('));
      String rowcol = serial_buffer.substring(serial_buffer.indexOf('(') + 1, serial_buffer.indexOf(')'));
      int i_addsub = int(addsub);
      int i_rowcol = int(rowcol);
      if (i_addsub > 0) {
        for (int k = 0; k < i_addsub; ++k) {
          addPrim(i_rowcol % 2, i_rowcol / 2);
        }
      }
      else {
        removePrims(i_rowcol % 2, i_rowcol / 2, -i_addsub);
      }
    }
  }
  
  background(255);
  nav.doTransforms();
  // draw a simple box, centered on the screen (yawn)
  fill(255,100,50);
  lights();
//  pointLight(200, 200, 200, width/2, height/2, 200);  uncomment to 'aim' the light source
  model.draw(this);
}

public void keyPressed() {
  //-- save model as an STL
  if( key == ' ' ) {
    //  UGeometry gb = new
     model.writeSTL(this, "output.stl");
   }
  
  //-- add one layer
  if( key == 'a' ) {
     for(int i = 0; i < numRows; i++ ) {
      for(int j = 0; j < numColumns; j++ ) {
             addPrim(i,j);
         }
      }
  }  
  
  // Add random set of cubes
  if( key == 'r' ) {
     for( int i = 0; i < numRandomPrims; i ++ )
       addPrim(int(random(numRows)),int(random(numColumns)) );
  }
  
  //
  if ( key == 'd' ) {
    if (totalPrims > 0) {
      int row, col;
      do {
        row = int(random(numRows));
        col = int(random(numColumns));
      } while (prims[row][col] == 0);
      removePrims(row, col, 1);
    }
  }
}

//-- Adds a primitive to (row, column). Imagine looking at a chessboard, row = x, columns = y
public void addPrim( int row, int column ) {
  doAddPrim(row, column, prims[row][column]++);
  totalPrims++;
}

public void doAddPrim(int row, int column, int z)
{
  UGeometry g;
  
  // Not sure why this is, but the primSize seems to need to be halved
  g = UPrimitive.box(primSize/2, primSize/2, primSize/2); 
  
  g.translate(new UVec3(row * (primSize+primOffset), column * (primSize+primOffset),z * primSize));
  
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
  


