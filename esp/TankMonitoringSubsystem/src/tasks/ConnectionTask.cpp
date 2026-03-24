#include "ConnectionTask.h"
#include "kernel/Logger.h"

ConnectionTask::ConnectionTask(PubSubClient *pClient, Led *pGreenLED, Led *pRedLED)
{
    this->pClient = pClient;
    this->pGreenLED = pGreenLED;
    this->pRedLED = pRedLED;
    setState(CHECKING);
}

void ConnectionTask::setState(ConnectionState newState)
{
    state = newState;
    stateTimestamp = millis();
    justEntered = true;
}

bool ConnectionTask::checkAndSetJustEntered()
{
    bool bak = justEntered;
    if (justEntered)
    {
        justEntered = false;
    }
    return bak;
}

void ConnectionTask::tick()
{
    pClient->loop();
    switch (state)
    {
    case CHECKING:
        if (WiFi.status() != WL_CONNECTED)
        {
            Logger.log("[ConnectionTask] WiFi lost, reconnecting...");
            this->pGreenLED->switchOff();
            this->pRedLED->switchOn();
            setState(RECONNECTING_WIFI);
        }
        else if (!pClient->connected())
        {
            Logger.log("[ConnectionTask] MQTT lost, reconnecting...");
            this->pGreenLED->switchOff();
            this->pRedLED->switchOn();
            setState(RECONNECTING_MQTT);
        }
        this->pGreenLED->switchOn();
        this->pRedLED->switchOff();
        break;

    case RECONNECTING_WIFI:
        if (checkAndSetJustEntered())
        {
            WiFi.disconnect();
            WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
        }
        if (WiFi.status() == WL_CONNECTED)
        {
            Logger.log("[ConnectionTask] WiFi restored");
            setState(CHECKING);
        }
        break;

    case RECONNECTING_MQTT:
        if (checkAndSetJustEntered())
        {
            // reset eventuale stato precedente
            pClient->disconnect();
        }
        String clientId = String("esiot-client-") + String(random(0xffff), HEX);
        if (pClient->connect(clientId.c_str()))
        {
            pClient->subscribe(MQTT_TOPIC);
            Logger.log("[ConnectionTask] MQTT restored");
            setState(CHECKING);
        }
        else
        {
            Logger.log("[ConnectionTask] MQTT failed, rc=" + String(pClient->state()));
        }
        break;
    }
}