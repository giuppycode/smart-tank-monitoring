#include "HangarPlatform.h"
#include <Arduino.h>

#include "kernel/MsgService.h"
#include "kernel/Logger.h"
#include "config.h"

#include "devices/Led.h"
#include "devices/DisplayLcd.h"
#include "devices/HangarDoor.h"
#include "devices/ButtonImpl.h"
#include "devices/Sonar.h"
#include "devices/Pir.h"
#include "devices/TempSensor.h"

void wakeUp() {}

HangarPlatform::HangarPlatform()
{
  pButton = new ButtonImpl(BT_PIN);
  pStaticLed = new Led(LED_PIN1);
  pActionLed = new Led(LED_PIN2);
  pAlarmLed = new Led(LED_PIN3);
  pSonar = new Sonar(SONAR_ECHO_PIN, SONAR_TRIG_PIN, SONAR_TIMEOUT);
  pPir = new Pir(PIR_PIN);
  pDisplayLcd = new DisplayLcd(LCD_I2C_ADDRESS, LCD_COLS, LCD_ROWS);
  pHangarDoor = new HangarDoor(MOTOR_PIN);
  pTempSensor = new TempSensor(TEMP_SENSOR_PIN);
}

void HangarPlatform::init()
{
  pDisplayLcd->init();
  pHangarDoor->forceClose();
  pSonar->setTemperature(20.0);
  pTempSensor->begin(); // Inizializza il sensore di temperatura
  Logger.log("HangarPlatform initialized");
}

Button *HangarPlatform::getButton()
{
  return this->pButton;
}

Led *HangarPlatform::getStaticLed()
{
  return this->pStaticLed;
}

Led *HangarPlatform::getActionLed()
{
  return this->pActionLed;
}

Led *HangarPlatform::getAlarmLed()
{
  return this->pAlarmLed;
}

HangarDoor *HangarPlatform::getHangarDoor()
{
  return this->pHangarDoor;
}

Sonar *HangarPlatform::getSonar()
{
  return this->pSonar;
}
Pir *HangarPlatform::getPir()
{
  return this->pPir;
}
DisplayLcd *HangarPlatform::getDisplayLcd()
{
  return this->pDisplayLcd;
}

TempSensor *HangarPlatform::getTempSensor()
{
  return this->pTempSensor;
}