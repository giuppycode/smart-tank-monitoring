#ifndef __HANGAR_PLATFORM__
#define __HANGAR_PLATFORM__

#include "config.h"
#include "devices/Button.h"
#include "devices/Led.h"
#include "devices/HangarDoor.h"
#include "devices/Sonar.h"
#include "devices/Pir.h"
#include "devices/DisplayLcd.h"
#include "devices/TempSensor.h"

class HangarPlatform
{

public:
  HangarPlatform();
  void init();

  Button *getButton();
  Led *getStaticLed();
  Led *getActionLed();
  Led *getAlarmLed();
  HangarDoor *getHangarDoor();
  Sonar *getSonar();
  Pir *getPir();
  DisplayLcd *getDisplayLcd();
  TempSensor *getTempSensor();

private:
  Button *pButton;
  Led *pStaticLed;
  Led *pActionLed;
  Led *pAlarmLed;
  HangarDoor *pHangarDoor;
  Sonar *pSonar;
  Pir *pPir;
  DisplayLcd *pDisplayLcd;
  TempSensor *pTempSensor;
};

#endif