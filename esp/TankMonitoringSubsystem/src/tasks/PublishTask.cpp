#include "PublishTask.h"
#include "kernel/Logger.h"
#include "devices/Sonar.h"

PublishTask::PublishTask(PubSubClient *pClient, Context *pContext)
{
    this->pClient = pClient;
    this->pContext = pContext;
}

void PublishTask::tick()
{
    float distance = pContext->getCurrentDistance();
    bool connected = pContext->isConnected();

    if (!connected)
    {
        Logger.log("[PublishTask] Not connected, skipping publish");
        return;
    }
    if (distance == NO_OBJ_DETECTED)
    {
        Logger.log("[PublishTask] No valid distance, skipping");
        return;
    }

    char msg[50];
    snprintf(msg, sizeof(msg), "%.2f", distance);
    pClient->publish(MQTT_TOPIC, msg);
    Logger.log("[PublishTask] Published: " + String(msg) + " cm");
}