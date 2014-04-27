//Firmware for Cubecube v.0.1
//Created by Kavi Laud 2014

//LED pins
int redPin = 13;
int greenPin = 18;

//Button pin
int buttonPin = 19;

int stack_height[4][16];

void setup() {
  for (int i = 0; i < 4; ++i) {
    for (int j = 0; j < 16; ++j) {
      stack_height[i][j] = 0;
    } 
  }
  setupMuxPins();
  Serial.begin(9600);
}

void loop() {
  int h, deltaH;
  
  for (int mux = 0; mux < 4; ++mux) {
    for (int channel = 0; channel < 16; ++channel) {
      h = vtable(readMux(mux, channel));
      deltaH = h - stack_height[mux][channel];
      if (deltaH != 0) {
        Serial.print(deltaH);
        Serial.print("(");
        Serial.print(channel % 8); // print row, 0-8, numbered starting from top
        Serial.print(",");
        Serial.print(7 - (mux*2 + channel / 8)); // print col, 0-8, numbered starting from left
        Serial.print(")");
      }
      stack_height[mux][channel] = h;
    }
  }
}
