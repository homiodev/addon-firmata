#include "Touchhome.h"

#ifdef COMM_ESP8266_WIFI

#define STREAM_PORT 3030

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include "utility/WiFiClientStream.h"
#include "utility/WiFiServerStream.h"
#include <WiFiUdp.h>

WiFiUDP Udp;
#define UDP_PAYLOAD "th:" BOARD ":"
char udpPayload[30];
byte udpPayloadLength;

ESP8266WebServer server(80);
WiFiServerStream stream(STREAM_PORT);
bool checkConnection;

#if defined(SERIAL_DEBUG)
#define IS_IGNORE_PIN(p)  ((p) == 1)
#endif

String html_header = "<html>\
  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\
  <head>\
    <title>ESP8266 Settings</title>\
    <style>\
      body { background-color: #cccccc; font-family: Arial, Helvetica, Sans-Serif; Color: #000088; }\
    </style>\
  </head>";

void handleRoot() {
  String str = "";
  str += html_header;
  str += "<body>\
		<form method=\"POST\" action=\"ok\">\
		  <input name=\"ssid\"> WIFI Net</br>\
		  <input name=\"pswd\"> Password</br></br>\
		  <input type=SUBMIT value=\"Save settings\">\
		</form>\
	  </body>\
	</html>";
	server.send ( 200, "text/html", str );
}

void handleOk() {
  String str = "";
  str += html_header;
  str += "<body>";

  String ssid_ap = server.arg(0);
  String pass_ap = server.arg(1);

  if(ssid_ap != ""){
	pass_ap.toCharArray(config.pwd, 20);
    ssid_ap.toCharArray(config.ssid, 20);

	EEPROM.put(1, config);
	EEPROM.commit();
	str +="Configuration saved</br></body></html>";
    server.send(200, "text/html", str);
	DEBUG_PRINTLN("Success change pwd/ssid");
	delay(1000);
    checkConnection = true;
  }
  else {
    str += "No WIFI Net</br><a href=\"/\">Return</a> to settings page</br></body></html>";
	server.send ( 200, "text/html", str );
  }
}

void hostConnectionCallback(byte state)
{
  switch (state) {
    case HOST_CONNECTION_CONNECTED:
      DEBUG_PRINTLN("TCP connection established");
      break;
    case HOST_CONNECTION_DISCONNECTED:
      DEBUG_PRINTLN("TCP connection disconnected");
      break;
  }
}

void Touchhome::startAP() {
	    digitalWrite(LED_BUILTIN, HIGH);
		DEBUG_PRINTLN("Creating WiFi AP");
        checkConnection = false;

	    WiFi.disconnect();
        WiFi.softAP("esp8266_th", "esp8266_th");
		server.on("/", handleRoot);
		server.on("/ok", handleOk);
        server.begin();
		DEBUG_PRINTLN("Start AccessPoint esp8266_th. Use AP and ip address 192.168.4.1");

        while(!checkConnection) {
            server.handleClient();
        }

		DEBUG_PRINTLN("End AccessPoint esp8266_th");
        server.close();
		DEBUG_PRINTLN("Server closed");
        WiFi.disconnect(true);
		DEBUG_PRINTLN("AP disconnected");
		digitalWrite(LED_BUILTIN, LOW);
}

void Touchhome::connectToRouterIfRequire() {
	if(WiFi.status() == WL_CONNECTED) {
		return;
	}
	DEBUG_PRINTLN("Connect to WiFi network using ssid/pwd ");
	DEBUG_PRINTLN(config.ssid);
	DEBUG_PRINTLN(config.pwd);
	DEBUG_PRINTLN(config.deviceID);

    WiFi.begin(config.ssid, config.pwd); // STA mode

    while(WiFi.status() != WL_CONNECTED) {
		DEBUG_PRINTLN("WiFi status");
	    DEBUG_PRINTLN(WiFi.status());
        // wait for readyness
        byte connectionAttempts = 0;
        while(WiFi.status() != WL_CONNECTED && ++connectionAttempts <= 60) {
            delay(500);
			DEBUG_PRINT(".");
        }
		if(WiFi.status() != WL_CONNECTED) {
		   DEBUG_PRINTLN("Unable connect to WiFi. Status");
  	       DEBUG_PRINTLN(WiFi.status());
           startAP();

		   DEBUG_PRINTLN("Reset WiFi to STA mode and begin");
           WiFi.begin(config.ssid, config.pwd);
		}
    }
	DEBUG_PRINT("Connected. Local IP Address: ");
    DEBUG_PRINTLN(WiFi.localIP());

	DEBUG_PRINT("Signal strength (RSSI): ");
    long rssi = WiFi.RSSI();
    DEBUG_PRINT(rssi);
    DEBUG_PRINTLN(" dBm");

	//WiFi.disconnect(true);
	//DEBUG_PRINTLN("Wifi disconnected");
	//stream.begin(config.ssid, config.pwd);
	//DEBUG_PRINTLN("Stream started");

    Firmata.begin(stream);
}
#endif

void Touchhome::setup()
{
	DEBUG_PRINTLN("setup...");
	pinMode(LED_BUILTIN, OUTPUT);

	#ifdef COMM_ESP8266_WIFI
	   DEBUG_BEGIN(9600);

	   WiFi.disconnect(true);
       WiFi.persistent(false);
	   if(wifi_get_phy_mode() != PHY_MODE_11G) {
		   wifi_set_phy_mode(PHY_MODE_11G);
	   }
       delay(500);

	   EEPROM.begin(512);
	#endif

	if (EEPROM.read(0) == EEPROM_CONFIGURED) {
        EEPROM.get(1, config);
		DEBUG_PRINTLN("read configuration");
		DEBUG_PRINTLN(config.deviceID);
    }
    else {
        config.deviceID = abs(int(random(99, 32767)));
		//String ssid = "Ruslan";
		//String pwd = "RusMas2018";
		//ssid.toCharArray(config.ssid, 20);
		//pwd.toCharArray(config.pwd, 20);

        EEPROM.put(1, config);
        EEPROM.write(0, EEPROM_CONFIGURED);
		DEBUG_PRINTLN("create configuration");
		DEBUG_PRINTLN(config.deviceID);
		#ifdef COMM_ESP8266_WIFI
			EEPROM.commit();
		#endif
    }

    #ifdef COMM_ESP8266_WIFI
	    String payload = UDP_PAYLOAD + String(config.deviceID);
		udpPayloadLength = payload.length() + 1;
		payload.toCharArray(udpPayload, udpPayloadLength);

		#ifdef SERIAL_DEBUG
			Firmata.setPinMode(1, PIN_MODE_IGNORE);
		#endif
		stream.attach(hostConnectionCallback);

        connectToRouterIfRequire();
	#endif

	#ifdef TH_SERIAL
		Firmata.begin(57600);
	#endif
}

bool Touchhome::loop(unsigned long currentMillis) {
    if (previousMillis == 0 || currentMillis - previousMillis > 30000) { // check not often than once per 30 sec
        previousMillis = currentMillis;
        if (currentMillis - lastPing > 600000) { // if device pinged too ago 10 min. Server pings each 3 min
		    DEBUG_PRINTLN("Set uniqueID to 0 because no pings from paired device");
            uniqueID = 0;
        }
        if (uniqueID == 0) {
			DEBUG_PRINTLN("Handle empty uniqueID");
			#ifdef COMM_ESP8266_WIFI
				connectToRouterIfRequire();
				DEBUG_PRINTLN("Send broadcast");
				DEBUG_PRINTLN(config.deviceID);

				Udp.beginPacket(IPAddress(255, 255, 255, 255), 8266);
				Udp.write(udpPayload, udpPayloadLength);
                Udp.endPacket();
			#endif
			sendTouchhomeCommand(SYSEX_REGISTER, 0, sizeof(BOARD), (byte*)BOARD);
        }
    }
#ifdef COMM_ESP8266_WIFI
    stream.maintain();
#endif
    return uniqueID != 0;
}

void Touchhome::sendTouchhomeCommand(byte command, byte messageID, byte argc, byte argv[])
{
    byte payload[3 + argc];
    payload[0] = messageID;
    payload[1] = lowByte(config.deviceID);
    payload[2] = highByte(config.deviceID);
    for (byte i = 0; i < argc; i++)
        payload[3 + i] = argv[i];
	DEBUG_PRINTLN("Send SYSEX");
    Firmata.sendSysex(command, 3 + argc, payload);
}

boolean Touchhome::handleSysex(byte command, byte argc, byte *argv)
{
    // skip handling for some commands
    if(command == ANALOG_MAPPING_QUERY || command ==PIN_MODE_SERIAL || command == PIN_STATE_QUERY || command == CAPABILITY_QUERY) {
        return false;
    }
    if (command >= 0x40 && command <= 0x50) {
        byte messageID = (byte)argv[0];
        unsigned int target = argv[1] << 8 | argv[2];
        if (target == config.deviceID) {
            switch (command) {
            case SYSEX_REGISTER:
                if (uniqueID == 0) {
                    uniqueID = readULong(3, argv);
                    lastPing = millis();
                }
                break;
            case SYSEX_PING:
                lastPing = millis();
                break;
            case SYSEX_GET_TIME_COMMAND:
                union longConvertor data {
                };
                data.whole = millis();
                sendTouchhomeCommand(SYSEX_GET_TIME_COMMAND, messageID, 8, data.nybble.buff);
                break;
            }
        }
        return true;
    }
    return uniqueID == 0; // return true - intercept command if uniqueID is empty
}
