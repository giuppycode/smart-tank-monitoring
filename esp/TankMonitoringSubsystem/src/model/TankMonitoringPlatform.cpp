#include "TankMonitoringPlatform.h"
#include "kernel/Logger.h"

TankMonitoringPlatform::TankMonitoringPlatform()
{
  pSonar = new Sonar(SONAR_ECHO_PIN, SONAR_TRIG_PIN, 30000);
  pMQTTClient = new PubSubClient(espClient);
  pGreenLED = new Led(GREEN_LED_PIN);
  pRedLED = new Led(RED_LED_PIN);
}

void TankMonitoringPlatform::init()
{
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print("Trying to connect to WiFi...\n");
  }
  Logger.log("WiFi connected: " + WiFi.localIP().toString());
  pMQTTClient->setServer(MQTT_BROKER, MQTT_PORT);
}

Led *TankMonitoringPlatform::getGreenLED()
{
  return pGreenLED;
}

Led *TankMonitoringPlatform::getRedLED()
{
  return pRedLED;
}
Sonar *TankMonitoringPlatform::getSonar()
{
  return pSonar;
}

PubSubClient *TankMonitoringPlatform::getMQTTClient()
{
  return pMQTTClient;
}
