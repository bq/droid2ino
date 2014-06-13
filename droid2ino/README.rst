This project provides an Android library that allows communicate an Android device with an Arduino board using a Bluetooth connection.

This project is based in the bluetooth-lib of khanmutzuar, but adapted for working with Arduino boards and with several other improvements. You can find bluetooth-lib here:
https://github.com/khanmurtuza/bluetooth-lib


==========
droid2ino
==========

droid2ino is a library project to communicating with Arduino hardware using a Bluetooth connection

It allows you to:

* Request communication with a Arduino board through a Bluetooth connection in a very easy way. Shows a list of the already paired devices and allows the user look for new devices and get be paired with them.

* Send messages to the Arduino board

* Receive messages from the Arduino board

* Obtain information about the Bluetooth connection status

* Give a different style to part of the dialog that appears when you request the BluetoothConnection with the paired devices and new ones.

* Use the Android support libraries v.7 in order to be able of using fragments, the action bar, the holo style, etc.

* All the texts of the library are in English and Spanish, and it is quite easy to translate to other languages.
  
If you have any questions you can contact us through the `DIY forum <http://diy.bq.com/forums/forum/forum/>`_  or sending an email to diy@bq.com.


Features
========

* Send and receive messages from Arduino board through a Bluetooth connection.

* Use of `Android v7 Support Library  <http://developer.android.com/tools/support-library/features.html#v7>`_

* Possibility of give a different style to the dialog shown when searching for the paired Bluetooth devices and new ones in order to get better integration with your app, by overriding the onDeviceListDialogStyleObtained() method.

* Search and pair new devices

* Translated to English and Spanish and easy to translate to other languages through adding XML files

* Fixed the problem when receiving the messages from the Arduino board of obtaining empty strings or divided strings that appeared in both libraries in which is based this one. It uses escape characters in order to obtaining the entire message well: 

  * Start escape characters: ``&&`` 
  * End escape characters : ``%%``

  So an example of a message written by the Arduino program would be::

	&&I write from Arduino%%

* Arduino boards need a specific UUID, so don't change it!

* In order to save device's battery, if the Bluetooth was disabled before using the app, then when going out from the app, it will be disabled again. If it was enabled, it will remain enabled. If the user accepts enabling the Bluetooth (if it was disabled when entered in the app for the first time), the library remember it, so don't ask the user again and enable the Bluetooth directly when the user come back to the app.

* When exiting the program, the Bluetooth connection, if there is one, will be closed. furthermore, the library gives the possibility of closing the current Bluetooth connection in any moment without exiting the app (for example for a disconnect button)

* Sample project to show how to use the library


Usage
=====

#. Clone the repository::

    git clone https://github.com/bq/droid2ino.git

#. Install in your local repository::
  
    cd droid2ino/droid2ino
    gradle install

#. Add your local repository to your root project's build.gradle file::

    repositories {
        mavenLocal()
    }

#. Add the droid2ino dependency to your app's build.gradle file. Due to a bug since gradle 1.9 we must declare the type of the dependencies and force transitiveness to true ::

    dependencies {
      compile('com.bq:droid2ino:1.5@aar') {
          transitive = true
      }
    }


#. Add the Bluetooth permissions to the AndroidManifest.xml of your project::
 
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH" />


Installation
============

#. Install `Android Studio <https://developer.android.com/sdk/installing/studio.html>`_ and `Gradle <http://www.gradle.org/downloads>`_.

#. If you use a 64 bits Linux, you will need to install ia32-libs-multiarch::

	sudo apt-get update
	sudo apt-get upgrade
	sudo apt-get install ia32-libs-multiarch 

#. Clone the repository::

	git clone https://github.com/bq/droid2ino.git

#. In Android Studio go to ``File`` > ``Open`` and select the droid2ino gradle project inside the previous cloned project (that with the green robot icon, the droid2ino library folder not the repository one with the example project inside too).

#. If your are going to use droid2ino for one of your projects, follow the instructions of the `Usage section <https://github.com/bq/droid2ino#usage>`_ in order to installing it in your local repository and add to it the dependency needed.


Requirements
============

- `Java JDK <http://www.oracle.com/technetwork/es/java/javase/downloads/jdk7-downloads-1880260.html>`_ 

- `Android Studio <https://developer.android.com/sdk/installing/studio.html>`_ 

- `Maven <http://maven.apache.org/download.cgi>`_.  If you use Ubuntu::
    
    sudo apt-get update
    sudo apt-get install maven

- `Gradle <http://www.gradle.org/downloads>`_ recommended version 1.10
  
- `Arduino IDE <http://arduino.cc/en/Main/Software#.UzBT5HX5Pj4>`_ 

- Arduino board with Bluetooth

- The app that will use this library must add the following permission, if not it will throw an Exception and will close::

    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH" />

- The app theme must have the Theme.AppCompat as parent in the style.xml file


Limitations
===========

In order to fix the problem when receiving the messages from the Arduino board of obtaining empty strings or divided strings, this library uses escape characters in order to obtaining the entire message well.
 
- Start escape characters: ``&&`` 

- End escape characters : ``%%``

So an example of a message written by the Arduino program would be::

	&&I write from Arduino%%


License
=======

droid2ino is distributed in terms of LGPL license. See http://www.gnu.org/licenses/lgpl.html for more details.

