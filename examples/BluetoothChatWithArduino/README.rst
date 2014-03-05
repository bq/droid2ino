========================
BluetoothChatWithArduino
========================

BluetoothChatWithArduino is a simple chat project that uses the droid2ino Android library to communicating with Arduino board using a Bluetooth connection.

This app is a simple example of what you can do with the droid2ino library.

It allows you to:

* Send messages to the Arduino board, which will respond you with an echo of what yo send to it. 

* You can open a Serial Monitor in the Arduino IDE and send messages to the mobile device, and BluetoothChatWithArduino will show them.



Features
========

* Send and receive messages from Arduino board through a Bluetooth connection

* Use of `Android v7 Support Library  <http://developer.android.com/tools/support-library/features.html#v7>`_

* Learn how to give a different style to the dialog shown when searching for the paired Bluetooth devices and new ones in order to get better integration with your app

* Learn how to use the connect and disconnect bluetooth functions, as when the user press the action bar buttons in this example

* The Arduino code for this chat example is in the Arduino folder

* If the Bluetooth was disabled before using the app, then when exiting the app it will be disabled again. If it was enabled, it will remain enabled

* Sample project to show how to use the library


Installation
============

#. Follow the installation instructions of the droid2ino and android support library v7 https://github.com/bq/droid2ino#installation.

#. Import the BluetoothChatWithArduino project in ``File`` > ``Import`` > ``Existing Projects into Workspace`` and browse to the BluetoothChatWithArduino project.

#. Maybe, you will need to update the reference of droid2ino this way:  
	
   - In Eclipse, select your project in the ``Package Explorer`` > ``File`` > ``Properties`` > ``Android`` 

   - Remove the previous wrong library reference.

   - Press the ``Add`` button and select the droid2ino library.



Requirements
============

* Android SDK

* Eclipse IDE (recommended)

* Arduino IDE distribution

* Arduino board with Bluetooth

* The droid2ino library and android support library v7



Limitations
===========

In order to fix the problem when receiving the messages from the Arduino board of obtaining empty strings or divided strings, the droid2ino library uses escape characters in order to obtaining the entire message well.
 
- Start escape characters: ``&&`` 

- End escape characters : ``%%``

So an example of a message written by the Arduino program would be::

	&&I write from Arduino%%


License
=======

BluetoothChatWithArduino is distributed in terms of GPL license. See http://www.gnu.org/licenses/ for more details.

