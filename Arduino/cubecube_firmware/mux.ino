const int mux_enable_pin[] = { 5, 6, 7, 10 }; // A, B, C, D
const int mux_signal_pin[] = { 3, 2, 1, 0 };  // A, B, C, D
const int mux_control_pin[] = { 12, 11, 9, 8 }; // s3, s2, s1, s0

void setupMuxPins()
{
  for (int i = 0; i < 4; ++i) {
    pinMode(mux_enable_pin[i], OUTPUT);
    pinMode(mux_control_pin[i], OUTPUT);
    digitalWrite(mux_control_pin[i], LOW);
  }
}

int readMux(int mux, int channel){
  // Enable the mux
  digitalWrite(mux_enable_pin[mux], LOW);

  // Select the channel
  for (int i = 0; i < 4; ++i) {
    digitalWrite(mux_control_pin[i], (channel >> (3 - i)) & 0x1);
  }

  //read the value at the SIG pin
  int value =  analogRead(mux_signal_pin[mux]);
  
  //Disable the mux
  digitalWrite(mux_enable_pin[mux], HIGH);
  
  return value;
}
