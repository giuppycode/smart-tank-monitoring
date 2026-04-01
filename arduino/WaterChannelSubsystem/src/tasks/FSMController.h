#ifndef __FSMCONTROLLER__
#define __FSMCONTROLLER__

#include <Arduino.h>
#include "kernel/Task.h"
#include "kernel/MsgService.h"
#include "model/Context.h"
#include "devices/DisplayLcd.h"
#include "devices/ValveMotor.h"
#include "devices/Pot.h"
#include "devices/Button.h"

class FSMController : public Task
{

public:
  FSMController(DisplayLcd *pDisplay, ValveMotor *pValveMotor, Potentiometer *pPot, Button *pButton, Context *pContext);
  void tick();

private:
  enum ControllerState
  {
    AUTOMATIC,
    MANUAL,
    UNCONNECTED
  };

  void setState(ControllerState newState);
  long elapsedTimeInState();
  void log(const String &msg);
  void handleIncomingMessage();
  void sendModeToCUS(const String &mode);
  void applyValveFromCUS(int percent);

  bool checkAndSetJustEntered();

  ControllerState state;

  long stateTimestamp;
  bool justEntered;
  unsigned long conditionStartTime;

  DisplayLcd *pDisplay;
  ValveMotor *pValveMotor;
  Potentiometer *pPot;
  Button *pButton;
  Context *pContext;
};

#endif