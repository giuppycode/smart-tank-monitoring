#include <Arduino.h>
#include "config.h"
#include "kernel/Scheduler.h"
#include "model/TankMonitoringPlatform.h"
#include "model/Context.h"
#include "tasks/SensorTask.h"
#include "tasks/PublishTask.h"
#include "tasks/ConnectionTask.h"
#include "kernel/Logger.h"
#include "kernel/MsgService.h"

Scheduler sched;
TankMonitoringPlatform *pTankMonitoringPlatform;
Context *pContext;

void setup()
{
  MsgService.init();
  Serial.begin(115200);

  // 1. WiFi
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Logger.log("WiFi connected: " + WiFi.localIP().toString());

  // 2. MQTT server config
  pTankMonitoringPlatform = new TankMonitoringPlatform();
  pTankMonitoringPlatform->init(); // chiama setServer() internamente

  sched.init(50);
  randomSeed(micros());

#ifndef __TESTING_HW__
  pContext = new Context();

  Task *pSensorTask = new SensorTask(pTankMonitoringPlatform->getSonar(), pContext);
  pSensorTask->init(200);
  sched.addTask(pSensorTask);

  Task *pPublishTask = new PublishTask(pTankMonitoringPlatform->getMQTTClient(), pContext);
  pPublishTask->init(2000);
  sched.addTask(pPublishTask);

  Task *pConnectionTask = new ConnectionTask(pTankMonitoringPlatform->getMQTTClient());
  pConnectionTask->init(50);
  sched.addTask(pConnectionTask);
#endif
}

void loop()
{
  sched.schedule();
}