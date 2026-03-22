#ifndef __BLINKING_LED_TASK__
#define __BLINKING_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Led.h"
#include <Arduino.h>

class BlinkingLedTask : public Task
{

public:
  BlinkingLedTask(Led *pActionLed, Context *pContext);
  void tick();

private:
  enum BlinkState
  {
    IDLE,
    BLINK_OFF,
    BLINK_ON
  };

  void setState(BlinkState newState);
  long elapsedTimeInState();
  void log(const String &msg);

  bool checkAndSetJustEntered();

  BlinkState state;
  long stateTimestamp;
  bool justEntered;

  Led *pActionLed;
  Context *pContext;
};

#endif