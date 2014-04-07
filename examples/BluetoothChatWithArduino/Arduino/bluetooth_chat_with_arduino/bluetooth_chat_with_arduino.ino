/*  
 *
 *     ********************************************************
 *     ********************************************************
 *     ***                                                  ***
 *     ***           Bluetooth Chat With Arduino            ***
 *     ***                                                  ***
 *     ******************************************************** 
 *     ********************************************************
 *
 *    Arduino code for the Android BluetoothChatWithArduino app.
 *    This is a simple chat between the Arduino board and the 
 *    Android device. 
 *    The Arduino board sends an echo to the Android device with
 *    the message that receive from the Android device through
 *    the Bluetooth Serial. 
 *    It allows to send messages from the Serial Monitor of the
 *    Arduino IDE to the Android device. 
 *     
 *   ****************************************************
 *   * Fecha: 05/03/2014                                *
 *   * Autor:Estefana Sarasola Elvira                   *
 *   * Mail: diy@bq.com                                 *
 *   * Licencia: GNU General Public License v3 or later *
 *   ****************************************************
 */

/******************************************************************/
/******************************************************************/



/******************************************************************
 *                           Libraries                            *
 ******************************************************************/ 

#include <SoftwareSerial.h>


/******************************************************************
 *                    Definition of variables                     *
 ******************************************************************/

/* Pin definition of the board to be used */
#define rxPin 11
#define txPin 12

/* Values definition */
// 63 bytes is the max. that the function Serial.read() can manage
#define bufferSize 63 

/*
   SoftwareSerial is used in order to be able of using the Serial
   for the monitor serial of the Arduino IDE, and use the 
   SoftwareSerial to communicate with the Android app
*/
SoftwareSerial bluetoothSerial(rxPin, txPin);

/* Array for store the instruction, 63 bytes is the maximun size 
that the serial can receive */
char mobileSerialBuffer[bufferSize];      
char computerSerialBuffer[bufferSize];

/* Buffer iterators */
int i=0; // index for iterating the mobileSerialBuffer
int j = 0; // index for iterating the computerSerialBuffer

/* Number of characters availables in the Serials */
int numCharMobileSerial = 0;
int numCharComputerSerial = 0;


/******************************************************************
 *                             Setup                              *
 ******************************************************************/

void setup(){
  
  /* define pin modes for tx, rx pins to communicate with the 
     Android app: */
  pinMode(rxPin, INPUT);
  pinMode(txPin, OUTPUT);
  
  /* Open the Bluetooth Serial and empty it */
  bluetoothSerial.begin(38400); 
  bluetoothSerial.flush();      
  
  /* Open the Serial Monitor and empty it */
  Serial.begin(9600);  
  Serial.flush();
  
}


/******************************************************************
 *                       Main program loop                        *
 ******************************************************************/

void loop(){
 
  /** READ FROM MOBILE DEVICE AND WRITE IN THE COMPUTER SERIAL **/
  
  /* If there is something in the Bluetooth serial port */
  if (bluetoothSerial.available() > 0){    

    /* Reset the iterator and clear the buffer */
    i = 0;
    memset(mobileSerialBuffer, 0, sizeof(mobileSerialBuffer));  
    
    /* Wait for let the buffer fills up. Depends on the length of 
       the data, 1 ms for each character more or less */
    delay(bufferSize);

    /* Number of characters availables in the Bluetooth Serial */
    numCharMobileSerial = bluetoothSerial.available();    
    
    /* Limit the number of characters that will be read from the
       Serial to avoid reading more than the size of the buffer */
    if (numCharMobileSerial > bufferSize) {
      numCharMobileSerial = bufferSize;
    }

    /* Read the Bluetooth Serial and store it in the buffer*/ 
    while (numCharMobileSerial--) {
      mobileSerialBuffer[i++] = bluetoothSerial.read();
      
      /* As data trickles in from your serial port you are 
         grabbing as much as you can, but then when it runs out 
         (as it will after a few bytes because the processor is 
         much faster than a 9600 baud device) you exit loop, which
         then restarts, and resets i to zero, and someChar to an 
         empty array.So please be sure to keep this delay */
      delay(3);
    } 
    
    /* Write what was received to the Serial Monitor of Arduino */
    Serial.print("Received from Android app: ");
    Serial.println(mobileSerialBuffer);
    
    /* Write the echo response to the Android app */
    bluetoothSerial.print("&&Arduino received: %%");
    delay(bufferSize);
    bluetoothSerial.print("&&");
    bluetoothSerial.print(mobileSerialBuffer);
    bluetoothSerial.print("%%");
    delay(bufferSize);
     
    /* Empty the Serials */ 
    bluetoothSerial.flush();
    Serial.flush();
    
  }
  
  
  /** READ FROM COMPUTER SERIAL AND WRITE IN THE MOBILE DEVICE 
      SERIAL **/ 
  
  /* If there is something in the Serial Monitor port */
  if(Serial.available() > 0) {

    /* Reset the iterator and clear the buffer */
    j = 0;
    memset(computerSerialBuffer, 0, sizeof(computerSerialBuffer));
    
    /* Wait for let the buffer fills up. Depends on the length of 
       the data, 1 ms for each character more or less */
    delay(bufferSize); 
    /* Number of characters availables in the Bluetooth Serial */
    numCharComputerSerial = Serial.available();   
    
    /* Limit the number of characters that will be read from the
    Serial to avoid reading more than the size of the buffer */
    if (numCharComputerSerial > bufferSize) {
          numCharComputerSerial = bufferSize;
    }

    /* Read the Serial Monitor and store it in the buffer*/ 
    while (numCharComputerSerial--) {
      computerSerialBuffer[j++] = Serial.read();
      
      /* As data trickles in from your serial port you are 
         grabbing as much as you can, but then when it runs out 
         (as it will after a few bytes because the processor is 
         much faster than a 9600 baud device) you exit loop, which
         then restarts, and resets i to zero, and someChar to an 
         empty array.So please be sure to keep this delay */
      delay(3);
    } 
    
    /* Write the message read in the Serial Monitor of Arduino to 
       the Android app */
    bluetoothSerial.print("&&Written from the SerialMonitor of Arduino: %%");
    delay(bufferSize);
    bluetoothSerial.print("&&");
    bluetoothSerial.print(computerSerialBuffer);
    bluetoothSerial.print("%%");
    delay(bufferSize);
    
    /* Write in the Serial Monitor of Arduino what you wrote in 
       it */
    Serial.print("You have sent to Android app: ");
    Serial.println(computerSerialBuffer);
    
    /* Empty the Serials */ 
    Serial.flush();   
    bluetoothSerial.flush();
  }
  
}  

