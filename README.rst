This project provides a Android library that allows communicate an Android device with an Arduino board using a Bluetooth connection.

This project is based in the bluetooth-lib of khanmutzuar, but adapted for working with Arduino boards and with several other improvements. You can find bluetooth-lib here:
https://github.com/khanmurtuza/bluetooth-lib


==========
Androidino
==========

Androidino is a library project to communicating with Arduino hardware using a Bluetoot connection

It allows you to:

* Request communication with a Arduino board through a Bluetooth connection in a very easy way. Shows a list of the already paired devices and allows the user look for new devices and get be paired with them.

* Send messages to the Arduino board

* Receive messages from the Arduino board

* Obtain information about the Bluetooth connection status

* Give a different style to part of the dialog that appears when you request the BluetoothConnection with the paired and new devices.

* Use the Android support libraries v.7 in order to be able of using fragments, the action bar, the holo style, etc.

* All the texts of the library are in English and Spanish, and it is quite easy to translate to other languages.


Features
========

* Send and receive messages from Arduino board through a Bluetooth connection.

* Use of `Android v7 Support Library  <http://developer.android.com/tools/support-library/features.html#v7>`_

* Possibility of give a different style to the dialog shown when searching for the paired and new Bluetooth devices in order to get better integration with your app

* Search and pair new devices

* Translated to English and Spanish and easy to translate to other languages through adding XML files

* Fixed the problem when receiving the messages from the Arduino board of obtaining empty strings or divided strings that appeared in both libraries in which is based this one. It uses escape characters in order to obtaining the entire message well: 

  * Start escape characters: ``&&`` 
  * End escape characters : ``%%``

  So an example of a message written by the Arduino program would be::

	&&I write from Arduino%%

* Arduino boards needs a specific UUID, so don't change it!

* If the Bluetooth was disabled before using the app, then when exiting it will be disabled again. If it was enabled, it will remain enabled.

* When exiting the program, the Bluetooth connection, if there is one, will be closed. furthermore, the library gives the possibility of closing the current Bluetooth connection in any moment without exiting the app (for example for a disconnect button)

* Sample project to show how to use the library


Installation
============

#. Install the ADT Bundle (Android SDK + Eclipse with ADT plugin installed among others.

#. If you use a 64 bits Linux, you will need to install ia32-libs-multiarch::

	sudo apt-get update
	sudo apt-get upgrade
	sudo apt-get install ia32-libs-multiarch 

#. Clone the repository::

	git clone https://github.com/bq/androidino.git

#. UNDER CONSTRUCTION

#. UNDER CONSTRUCTION

#. Set the Androidino library to your app project:  
	
   - In Eclipse, select your project in the Package Explorer > File > Properties > Android 

   - Press the ``Add`` button and select the Androidino library.

#. Add the Bluetooth permissions to the AndroidManifest.xml of your project::
 
	<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
	<uses-permission android:name="android.permission.BLUETOOTH" />



Requirements
============

* Android SDK

* Eclipse IDE (recommended)

* Arduino IDE distribution

* Arduino board with bluetooth

The app that will use this library must add the following permission, if not it will throw an Exception and close::

    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH" />


Limitations
===========

In order to fix the problem when receiving the messages from the Arduino board of obtaining empty strings or divided strings that appeared in both libraries in which is based this one, this library uses escape characters in order to obtaining the entire message well.
 
- Start escape characters: ``&&`` 

- End escape characters : ``%%``

So an example of a message written by the Arduino program would be::

	&&I write from Arduino%%


License
=======

Androidino is distributed in terms of LGPL license. See http://www.gnu.org/licenses/lgpl.html for more details.

