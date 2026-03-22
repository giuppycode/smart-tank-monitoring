#ifndef __ALARM_TASK__
#define __ALARM_TASK__

#include "kernel/Task.h"
#include "model/Context.h"
#include "devices/Led.h"
#include "devices/Button.h"
#include "devices/DisplayLcd.h"
#include "devices/HangarDoor.h"
#include "devices/TempSensor.h"

class AlarmTask : public Task
{

public:
    AlarmTask(TempSensor *pTempSensor, Button *pResetButton, Led *pAlarmLed,
              DisplayLcd *pDisplay, HangarDoor *pHangarDoor, Context *pContext);
    void tick();

private:
    enum AlarmState
    {
        NORMAL,
        CHECK_PRE_ALARM,
        PRE_ALARM,
        CHECK_ALARM,
        ALARM
    };

    void setState(AlarmState newState);
    long elapsedTimeInState();
    bool checkAndSetJustEntered();

    AlarmState state;
    long stateTimestamp;
    bool justEntered;

    TempSensor *pTempSensor;
    Button *pResetButton;
    Led *pAlarmLed;
    DisplayLcd *pDisplay;
    HangarDoor *pHangarDoor;
    Context *pContext;
};

#endif