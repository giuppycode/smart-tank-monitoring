#include <Arduino.h>
#include "config.h"

#include "kernel/Scheduler.h"
#include "kernel/Logger.h"
#include "kernel/MsgService.h"

#include "model/WaterChannelPlatform.h"

#include "tasks/FSMController.h"
#include "tasks/PotReader.h"

Scheduler sched;

WaterChannelPlatform *pWaterChannelPlatform;
Context *pContext;

void setup()
{
  MsgService.init();
  sched.init(50);

  Logger.log("Starting Water Channel Subsystem...");

  pWaterChannelPlatform = new WaterChannelPlatform();
  pWaterChannelPlatform->init();

#ifndef __TESTING_HW__
  pContext = new Context();

  Task *pFSMController = new FSMController(pWaterChannelPlatform->getDisplayLcd(), pWaterChannelPlatform->getValveMotor(), pWaterChannelPlatform->getPot(), pWaterChannelPlatform->getButton(), pContext);
  pFSMController->init(200);

  Task *pPotReader = new PotReader(pWaterChannelPlatform->getPot(), pContext);
  pPotReader->init(100);

  sched.addTask(pFSMController);
  sched.addTask(pPotReader);

#endif
}

void loop()
{
  sched.schedule();
}