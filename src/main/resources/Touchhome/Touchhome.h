//#pragma once
#ifndef TOUCHHOME_H
#define TOUCHHOME_H

#include <EEPROM.h>
#include <ConfigurableFirmata.h>
#include "FirmataFeature.h"

/*
  Uncomment desired implementation
*/
#define COMM_ESP8266_WIFI
//#define COMM_SERIAL


#define SERIAL_DEBUG
#include "utility/firmataDebug.h"

#if defined(TEENSYDUINO)
//  --------------- Teensy -----------------
#if defined(__AVR_ATmega32U4__)
#define BOARD "Teensy 2.0"
#elif defined(__AVR_AT90USB1286__)
#define BOARD "Teensy++ 2.0"
#elif defined(__MK20DX128__)
#define BOARD "Teensy 3.0"
#elif defined(__MK20DX256__)
#define BOARD "Teensy 3.2"
#elif defined(__MKL26Z64__)
#define BOARD "Teensy LC"
#elif defined(__MK64FX512__)
#define BOARD "Teensy 3.5"
#elif defined(__MK66FX1M0__)
#define BOARD "Teensy 3.6"
#else
#error "Unknown board"
#endif
#else // --------------- Arduino ------------------
#if defined(ARDUINO_AVR_ADK)
#define BOARD "Mega Adk"
#elif defined(ARDUINO_AVR_BT)    // Bluetooth
#define BOARD "Bt"
#elif defined(ARDUINO_AVR_DUEMILANOVE)
#define BOARD "Duemilanove"
#elif defined(ARDUINO_AVR_ESPLORA)
#define BOARD "Esplora"
#elif defined(ARDUINO_AVR_ETHERNET)
#define BOARD "Ethernet"
#elif defined(ARDUINO_AVR_FIO)
#define BOARD "Fio"
#elif defined(ARDUINO_AVR_GEMMA)
#define BOARD "Gemma"
#elif defined(ARDUINO_AVR_LEONARDO)
#define BOARD "Leonardo"
#elif defined(ARDUINO_AVR_LILYPAD)
#define BOARD "Lilypad"
#elif defined(ARDUINO_AVR_LILYPAD_USB)
#define BOARD "Lilypad Usb"
#elif defined(ARDUINO_AVR_MEGA)
#define BOARD "Mega"
#elif defined(ARDUINO_AVR_MEGA2560)
#define BOARD "Mega 2560"
#elif defined(ARDUINO_AVR_MICRO)
#define BOARD "Micro"
#elif defined(ARDUINO_AVR_MINI)
#define BOARD "Mini"
#elif defined(ARDUINO_AVR_NANO)
#define BOARD "Nano"
#elif defined(ARDUINO_AVR_NG)
#define BOARD "NG"
#elif defined(ARDUINO_AVR_PRO)
#define BOARD "Pro"
#elif defined(ARDUINO_AVR_ROBOT_CONTROL)
#define BOARD "Robot Ctrl"
#elif defined(ARDUINO_AVR_ROBOT_MOTOR)
#define BOARD "Robot Motor"
#elif defined(ARDUINO_AVR_UNO)
#define BOARD "Uno"
#elif defined(ARDUINO_AVR_YUN)
#define BOARD "Yun"
// These boards must be installed separately:
#elif defined(ARDUINO_SAM_DUE)
#define BOARD "Due"
#elif defined(ARDUINO_SAMD_ZERO)
#define BOARD "Zero"
#elif defined(ARDUINO_ARC32_TOOLS)
#define BOARD "101"
#elif defined(ESP8266)
#define BOARD "ESP8266"
#else
#define BOARD "Unknown board"
#endif
#endif

#define SYSEX_REGISTER 0x40
#define SYSEX_PING 0x49
#define SYSEX_GET_TIME_COMMAND 0x50
#define EEPROM_CONFIGURED 0x4

#if defined (COMM_ESP8266_WIFI)
#define SIZE 20
#else
#define SIZE 0
#endif

namespace {
	union longConvertor {
		struct {
			byte buff[8];
		} nybble;
		uint64_t whole;
	};

	struct ArduinoConfig {
		unsigned int deviceID;
		char ssid[SIZE];
		char pwd[SIZE];
	} config;
}

class Touchhome: public FirmataFeature {
public:
    Touchhome()
    {
    }

    inline boolean handlePinMode(byte pin, int mode) {
        return false;
    }

    inline void handleCapability(byte pin) {
    }

    inline void reset() {
        uniqueID = 0;
    }

    boolean handleSysex(byte command, byte argc, byte* argv);

    void setup();

    void sendTouchhomeCommand(byte command, byte messageID, byte argc, byte argv[]);

    bool loop(unsigned long currentMillis);

    inline uint64_t readULong(byte from, byte* argv)
    {
        union longConvertor data {
        };
        for (uint8_t i = from; i < from + 8; i++) {
            data.nybble.buff[7 - i + from] = argv[i];
        }
        return data.whole;
    }

private:
#ifdef COMM_ESP8266_WIFI
    friend void handleRoot();
	friend void handleOk();
	friend void hostConnectionCallback(byte state);
    void connectToRouterIfRequire();
    void startAP();
#endif

    uint64_t uniqueID;
    unsigned long lastPing;
    unsigned long previousMillis;
};

#endif
