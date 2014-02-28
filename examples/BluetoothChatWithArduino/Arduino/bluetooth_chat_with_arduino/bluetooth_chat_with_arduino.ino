//////////////////////////////////////////////////////////////////////////////////
//PROJECT BluetoothChatWithArduino
//
//Estefania Sarasola Elvira
//
//////////////////////////////////////////////////////////////////////////////////


#include <SoftwareSerial.h>

#define rxPin 11
#define txPin 12

/*
* SoftwareSerial is used in order to be able of using the Serial for the monitor serial
* of the Arduino IDE, and use the SoftwareSerial to communicate with the Android app
*/
SoftwareSerial mySerial(rxPin, txPin);

// array for store the instruction, 63 bytes is the maximun size that the serial can receive
char mobileSerialBuffer[63];      
char computerSerialBuffer[63];

int i=0; // index for iterating the mobileSerialBuffer
int j = 0; // index for iterating the computerSerialBuffer


void setup(){
  
  // define pin modes for tx, rx pins to communicate with the Android app:
  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);
  
  mySerial.begin(38400); // open the serial with the baud rate of the used bluetooth
  mySerial.flush();        // empty the serial
  
  Serial.begin(9600);  // open the serial port for the serial monitor
  Serial.flush();
  
}


void loop(){
 
  /** READ FROM MOBILE DEVICE AND WRITE IN THE COMPUTER SERIAL **/
  
  // reset the variables
  i = 0;
  memset(mobileSerialBuffer, 0, sizeof(mobileSerialBuffer));  // Clear the serial buffer 
  
  if (mySerial.available() > 0){    // if there is something in the serial port
   
    delay(63); // let the buffer fill up   ///IMPORTANT : Depends on the length of the data
    int numCharMobileSerial = mySerial.available();    // number of characters that arrives to the serial
    
    if (numCharMobileSerial>63) {
          numCharMobileSerial=63;
    }

    while (numCharMobileSerial--) {
      mobileSerialBuffer[i++] = mySerial.read();
      
      //As data trickles in from your serial port you are grabbing as much as you can, 
      //but then when it runs out (as it will after a few bytes because the processor 
      //is much faster than a 9600 baud device) you exit loop, which then restarts, 
      //and resets i to zero, and someChar to an empty array.So please be sure to keep this delay 
      delay(3);
    } 
    
    // Write what was received to the Serial Monitor of Arduino
    Serial.print("Received from Android app: ");
    Serial.println(mobileSerialBuffer);
    
    // Write the echo response to the Android app
    mySerial.print("&&Arduino received: %%");
    delay(63);
    mySerial.print("&&");
    mySerial.print(mobileSerialBuffer);
    mySerial.print("%%");
    delay(63);
      
   mySerial.flush();
   Serial.flush();
    
  }
  
  
  
  /** READ FROM COMPUTER SERIAL AND WRITE IN THE MOBILE DEVICE SERIAL **/
  
  // reset the variables
  j = 0;
  memset(computerSerialBuffer, 0, sizeof(computerSerialBuffer));   // Clear the serial buffer  
  
  // when characters arrive over the serial port...
  if(Serial.available() > 0) {
    
    delay(63); // let the buffer fill up   ///IMPORTANT : Depends on the length of the data
    int numCharComputerSerial = Serial.available();    // number of characters that arrives to the serial
    
    if (numCharComputerSerial > 63) {
          numCharComputerSerial = 63;
    }

    while (numCharComputerSerial--) {
      computerSerialBuffer[j++] = Serial.read();
      
      //As data trickles in from your serial port you are grabbing as much as you can, 
      //but then when it runs out (as it will after a few bytes because the processor 
      //is much faster than a 9600 baud device) you exit loop, which then restarts, 
      //and resets i to zero, and someChar to an empty array.So please be sure to keep this delay 
      delay(3);
    } 
    
    mySerial.print("&&Written from the SerialMonitor of Arduino: %%");
    delay(63);
    mySerial.print("&&");
    mySerial.print(computerSerialBuffer);
    mySerial.print("%%");
    delay(63);
    
    Serial.print("You have sent to Android app: ");
    Serial.println(computerSerialBuffer);
    
   Serial.flush();   
   mySerial.flush();
  }
  
}  


