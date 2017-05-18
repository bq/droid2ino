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
 *                    Definition of variables                     *
 ******************************************************************/

/* Bauderate of the Bluetooth*/
#define BQ_ZUM_BLUETOOTH 19200

/* Values definition */
// 63 bytes is the max. that the function Serial.read() can manage
#define bufferSize 63 

/* Array for store the instruction, 63 bytes is the maximun size 
that the serial can receive */
char mobileSerialBuffer[bufferSize];

/* Buffer iterators */
int i=0; // index for iterating the mobileSerialBuffer

/* Number of characters availables in the Serials */
int numCharMobileSerial = 0;


/******************************************************************
 *                             Setup                              *
 ******************************************************************/

void setup(){
  
  /* Open the Bluetooth Serial and empty it */  
  Serial.begin(BQ_ZUM_BLUETOOTH); 
  Serial.flush();      
  
}


/******************************************************************
 *                       Main program loop                        *
 ******************************************************************/

void loop(){
 
  /** READ FROM MOBILE DEVICE AND WRITE IN THE COMPUTER SERIAL **/
  
  /* If there is something in the Bluetooth serial port */
  if (Serial.available() > 0){    

    /* Reset the iterator and clear the buffer */
    i = 0;
    memset(mobileSerialBuffer, 0, sizeof(mobileSerialBuffer));  
    
    /* Wait for let the buffer fills up. Depends on the length of 
       the data, 1 ms for each character more or less */
    delay(bufferSize);

    /* Number of characters availables in the Bluetooth Serial */
    numCharMobileSerial = Serial.available();    
    
    /* Limit the number of characters that will be read from the
       Serial to avoid reading more than the size of the buffer */
    if (numCharMobileSerial > bufferSize) {
      numCharMobileSerial = bufferSize;
    }

    /* Read the Bluetooth Serial and store it in the buffer*/ 
    while (numCharMobileSerial--) {
      mobileSerialBuffer[i++] = Serial.read();
      
      /* As data trickles in from your serial port you are 
         grabbing as much as you can, but then when it runs out 
         (as it will after a few bytes because the processor is 
         much faster than a 9600 baud device) you exit loop, which
         then restarts, and resets i to zero, and someChar to an 
         empty array.So please be sure to keep this delay */
      delay(3);
    } 
    
    /* Write the echo response to the Android app */
    Serial.print("&&Arduino received: %%");
    delay(bufferSize);
    Serial.print("&&");
    Serial.print(mobileSerialBuffer);
    Serial.print("%%");
    delay(bufferSize);
     
    /* Empty the Serial */ 
    Serial.flush();
    
  }
  
}  

