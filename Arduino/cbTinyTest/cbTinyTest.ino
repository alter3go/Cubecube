/*
  Analog Input
 Demonstrates analog input by reading an analog sensor on analog pin 0 and
 turning on and off a light emitting diode(LED)  connected to digital pin 13. 
 The amount of time the LED will be on and off depends on
 the value obtained by analogRead(). 
 
 The circuit:
 * Potentiometer attached to analog input 0
 * center pin of the potentiometer to the analog pin
 * one side pin (either one) to ground
 * the other side pin to +5V
 * LED anode (long leg) attached to digital output 13
 * LED cathode (short leg) attached to ground
 
 * Note: because most Arduinos have a built-in LED attached 
 to pin 13 on the board, the LED is optional.
 
 
 Created by David Cuartielles
 modified 30 Aug 2011
 By Tom Igoe
 
 This example code is in the public domain.
 
 http://arduino.cc/en/Tutorial/AnalogInput
 
 */

const int PIN_COUNT = 4;
const int PIN[] = { A3, A2, A4, A5 };
int stack_height[] = { 0, 0, 0, 0 };

void setup() {
  Serial.begin(9600);
}

void loop() {
  int deltaH, raw_value;
  
  for (int i = 0; i < PIN_COUNT; ++i) {
    raw_value = analogRead(PIN[i]);
  
    if(raw_value == 0) {
      deltaH = 0 - stack_height[i];
      stack_height[i] = 0;
    }
    else if(raw_value > 268 && raw_value < 277) {
      deltaH = 1 - stack_height[i];
      stack_height[i] = 1;
    }
    else if(raw_value > 425 && raw_value < 435) {
      deltaH = 2 - stack_height[i];
      stack_height[i] = 2;
    }
    else {
      deltaH = 0;
    }
    
    if (deltaH != 0) {
      Serial.print(deltaH);
      Serial.print("(");
      Serial.print(i);
      Serial.print(")");
    }
  }
}
