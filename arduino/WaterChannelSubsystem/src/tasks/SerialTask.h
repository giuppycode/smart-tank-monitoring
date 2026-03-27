#ifndef __SERIAL_TASK__
#define __SERIAL_TASK__

#include "kernel/Task.h"
#include "kernel/MsgService.h"
#include "model/Context.h"

class SerialTask : public Task
{

public:
    SerialTask(Context *pContext);
    void tick();

private:
    Context *pContext;
    unsigned long lastSendTime;
};

#endif