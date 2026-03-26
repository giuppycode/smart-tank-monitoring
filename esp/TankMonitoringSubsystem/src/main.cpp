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

  pTankMonitoringPlatform = new TankMonitoringPlatform();
  pTankMonitoringPlatform->init();

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

  Task *pConnectionTask = new ConnectionTask(pTankMonitoringPlatform->getMQTTClient(), pTankMonitoringPlatform->getGreenLED(), pTankMonitoringPlatform->getRedLED(), pContext);
  pConnectionTask->init(1000);
  sched.addTask(pConnectionTask);
#endif
}

void loop()
{
  sched.schedule();
}