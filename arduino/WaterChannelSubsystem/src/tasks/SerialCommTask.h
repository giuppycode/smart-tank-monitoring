#ifndef __SERIALCOMMTASK__
#define __SERIALCOMMTASK__

#include <Arduino.h>
#include "kernel/Task.h"
#include "kernel/MsgService.h"
#include "model/Context.h"
#include "devices/ValveMotor.h"

class SerialCommTask : public Task {

public:
    SerialCommTask(Context* pContext, ValveMotor* pValveMotor);
    void tick();

private:
    void sendStateIfChanged();
    String getCurrentModeString();
    
    Context* pContext;
    ValveMotor* pValveMotor;
    String previousMode;
    int previousPercent;
};

#endif
