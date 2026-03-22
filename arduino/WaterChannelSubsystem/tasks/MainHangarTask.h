#ifndef __MAIN_HANGAR_TASK__
#define __MAIN_HANGAR_TASK__

#include <Arduino.h>
#include "kernel/Task.h"
#include "model/Context.h"

#include "devices/PresenceSensor.h"
#include "devices/ProximitySensor.h"
#include "devices/DisplayLcd.h"
#include "devices/Led.h"
#include "devices/HangarDoor.h"

class MainHangarTask : public Task
{

public:
  MainHangarTask(PresenceSensor *pPresenceSensor, ProximitySensor *pProximitySensor, DisplayLcd *pDisplay, Led *staticLed, HangarDoor *pHangarDoor, Context *pContext);
  void tick();

private:
  enum HangarState
  {
    IDLE_INSIDE,
    TAKING_OFF,
    CHECK_TAKING_OFF,
    OUTSIDE,
    LANDING,
    CHECK_LANDING
  };

  void setState(HangarState newState);
  long elapsedTimeInState();
  void log(const String &msg);

  bool checkAndSetJustEntered();
  void openDoorIfClosed();
  void closeDoorIfOpen();

  HangarState state;

  long stateTimestamp;
  bool justEntered;
  unsigned long conditionStartTime;

  PresenceSensor *pPresenceSensor;
  ProximitySensor *pProximitySensor;
  DisplayLcd *pDisplay;
  Led *pStaticLed;
  HangarDoor *pHangarDoor;
  Context *pContext;

  int currentPos;
  bool toBeStopped;
};

#endif