//#define COMM_ESP8266_WIFI
//#define COMM_SERIAL

//#define ENABLE_DHT
//#define ENABLE_I2C
//#define ENABLE_SPI
#define ENABLE_ANALOG
#define ENABLE_DIGITAL
//#define ENABLE_ONE_WIRE
//#define ENABLE_ACCELSTEPPER
//#define ENABLE_FREQUENCY
#ifndef ESP32
//#define ENABLE_SERVO
#endif
#if defined (ESP32) || defined (ARDUINO_ARCH_AVR)
//#define ENABLE_SLEEP
#endif

#include <ConfigurableFirmata.h>

#include "homio.h"
homio homio;

#ifdef ENABLE_SLEEP
#include "ArduinoSleep.h"
ArduinoSleep sleeper(39, 0);
#endif

#ifdef ENABLE_DIGITAL
#include <DigitalInputFirmata.h>
DigitalInputFirmata digitalInput;

#include <DigitalOutputFirmata.h>
DigitalOutputFirmata digitalOutput;
#endif

#ifdef ENABLE_ANALOG
#include <AnalogInputFirmata.h>
AnalogInputFirmata analogInput;

#include <AnalogOutputFirmata.h>
AnalogOutputFirmata analogOutput;
#endif

#ifdef ENABLE_DHT
#include <DhtFirmata.h>
DhtFirmata dhtFirmata;
#endif

#ifdef ENABLE_SERVO
#include <Servo.h>
#include <ServoFirmata.h>
ServoFirmata servo;
#endif

#ifdef ENABLE_I2C
#include <Wire.h>
#include <I2CFirmata.h>
I2CFirmata i2c;
#endif

#ifdef ENABLE_ONE_WIRE
#include <OneWireFirmata.h>
OneWireFirmata oneWire;
#endif

#ifdef ENABLE_ACCELSTEPPER
#include <AccelStepperFirmata.h>
AccelStepperFirmata accelStepper;
#endif

#ifdef ENABLE_FREQUENCY
#include <Frequency.h>
Frequency frequency;
#endif

#include <FirmataExt.h>
FirmataExt firmataExt;

#include <FirmataReporting.h>
FirmataReporting reporting;

void systemResetCallback()
{
#ifndef ESP32
  for (byte i = 0; i < TOTAL_PINS; i++) {
    if (FIRMATA_IS_PIN_ANALOG(i)) {
      Firmata.setPinMode(i, PIN_MODE_ANALOG);
    } else if (IS_PIN_DIGITAL(i)) {
      Firmata.setPinMode(i, PIN_MODE_OUTPUT);
    }
  }
#endif
  firmataExt.reset();
}

void initFirmata()
{
  firmataExt.addFeature(homio);
  #ifdef ENABLE_DIGITAL
  	firmataExt.addFeature(digitalInput);
  	firmataExt.addFeature(digitalOutput);
  #endif
  #ifdef ENABLE_ANALOG
  	firmataExt.addFeature(analogInput);
  	firmataExt.addFeature(analogOutput);
  #endif
  #ifdef ENABLE_SERVO
  	firmataExt.addFeature(servo);
  #endif
  #ifdef ENABLE_I2C
  	firmataExt.addFeature(i2c);
  #endif
  #ifdef ENABLE_ACCELSTEPPER
  	firmataExt.addFeature(accelStepper);
  #endif
  #ifdef ENABLE_ONE_WIRE
  	firmataExt.addFeature(oneWire);
  #endif
  firmataExt.addFeature(reporting);
  #ifdef ENABLE_DHT
  	firmataExt.addFeature(dhtFirmata);
  #endif
  #ifdef ENABLE_SLEEP
  	firmataExt.addFeature(sleeper);
  #endif
  #ifdef ENABLE_FREQUENCY
  	firmataExt.addFeature(frequency);
  #endif

  Firmata.attach(SYSTEM_RESET, systemResetCallback);
}

void setup()
{
  Firmata.setFirmwareNameAndVersion("ConfigurableFirmata", FIRMATA_FIRMWARE_MAJOR_VERSION, FIRMATA_FIRMWARE_MINOR_VERSION);
  initTransport();
  Firmata.sendString(F("Booting device. Stand by..."));
  initFirmata();
  homio.setup();

  Firmata.parse(SYSTEM_RESET);
}

void loop()
{
  while(Firmata.available()) {
    Firmata.processInput();
    if (!Firmata.isParsingMessage()) {
      break;
    }
  }

  if(homio.loop(millis())) {
    firmataExt.report(reporting.elapsed());
  }
}
