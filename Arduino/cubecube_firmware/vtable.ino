int vtable(int x){
  
  int y;
  
  if(x==0){
    y = 0;
  }
  
  if(x>185 && x<190){
    y = 1;
  }
  
  if(x>315 && x<320){
    y = 2;
  }
  
  if(x>410 && x<420){
    y = 3;
  }
  
  if(x>480 && x<490){
    y = 4;
  }
  
  if(x>540 && x<545){
    y = 5;
  }
  
  if(x>585 && x<595){
    y = 6;
  }
  
  if(x>620 && x<630){
    y = 7;
  }
  
  if(x>655 && x<665){
    y = 8;
  }
  
  if(x>680 && x<690){
    y = 9;
  }
  
  if(x>705 && x<715){
    y = 10;
  }  
  
  if(x>715){
    y = 11;
  } 
  
  return y;
}