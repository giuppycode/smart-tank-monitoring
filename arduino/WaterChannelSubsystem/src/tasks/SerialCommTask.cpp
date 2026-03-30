#include "SerialCommTask.h"
#include "kernel/Logger.h"

#define L1 0.20
#define L2 0.30

SerialCommTask::SerialCommTask(Context* pContext, ValveMotor* pValveMotor) {
    this->pContext = pContext;
    this->pValveMotor = pValveMotor;
    this->previousMode = "";
    this->previousPercent = -1;
}

void SerialCommTask::tick() {
    if (MsgService.isMsgAvailable()) {
        Msg* msg = MsgService.receiveMsg();
        String content = msg->getContent();
        
        if (content.startsWith("MODE:")) {
            String mode = content.substring(5);
            if (mode == "AUTOMATIC") {
                pContext->setAutomatic();
                Logger.log("Serial: Mode set to AUTOMATIC");
            } else if (mode == "MANUAL") {
                pContext->setManual();
                Logger.log("Serial: Mode set to MANUAL");
            } else if (mode == "UNCONNECTED") {
                pContext->setUnconnected();
                Logger.log("Serial: Mode set to UNCONNECTED");
            }
        } else if (content.startsWith("VALVE:")) {
            String valStr = content.substring(6);
            int percent = valStr.toInt();
            if (percent >= 0 && percent <= 100) {
                int angle = map(percent, 0, 100, 0, 90);
                pValveMotor->manuallySetAngle(angle);
                Logger.log("Serial: Valve set to " + String(percent) + "%");
            }
        }
        
        delete msg;
    }
    
    sendStateIfChanged();
}

void SerialCommTask::sendStateIfChanged() {
    String currentMode = getCurrentModeString();
    int currentPercent = pValveMotor->getOpeningPercent();
    
    if (currentMode != previousMode || currentPercent != previousPercent) {
        String stateMsg = "STATE:" + currentMode + "," + String(currentPercent);
        MsgService.sendMsg(stateMsg);
        
        previousMode = currentMode;
        previousPercent = currentPercent;
    }
}

String SerialCommTask::getCurrentModeString() {
    if (pContext->isAutomatic()) {
        return "AUTOMATIC";
    } else if (pContext->isManual()) {
        return "MANUAL";
    } else {
        return "UNCONNECTED";
    }
}
