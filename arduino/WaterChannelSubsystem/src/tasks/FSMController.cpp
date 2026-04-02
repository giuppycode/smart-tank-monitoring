#include "FSMController.h"
#include <Arduino.h>
#include <stdlib.h>
#include "kernel/Logger.h"

#define L1 0.20
#define L2 0.30
#define PICKUP_THRESHOLD 0.02

FSMController::FSMController(DisplayLcd *pDisplay, ValveMotor *pValveMotor, Potentiometer *pPot, Button *pButton, Context *pContext)
{
    this->pDisplay = pDisplay;
    this->pValveMotor = pValveMotor;
    this->pPot = pPot;
    this->pButton = pButton;
    this->pContext = pContext;

    this->conditionStartTime = 0;
    setState(AUTOMATIC);
}

void FSMController::tick()
{
    handleIncomingMessage();

    switch (state)
    {
    case AUTOMATIC:
        if (!pContext->isUnconnected())
        {
            pContext->setAutomatic();
            if (this->checkAndSetJustEntered())
            {
                Logger.log("[FSM] Entered AUTOMATIC");
                pValveMotor->close();
            }
            if (pButton->isPressed())
            {
                setState(MANUAL);
                sendModeToCUS("MANUAL");
            }

            float currentDistance = pContext->getCurrentDistance();
            int percentage = 0;
            if (currentDistance > L1 && currentDistance < L2)
            {
                pValveMotor->half(); // al 50%
                percentage = 50;
            }
            else if (currentDistance >= L2)
            {
                pValveMotor->open(); // al 100%
                percentage = 100;
            }
            else
            {
                pValveMotor->close(); // al 0%
                percentage = 0;
            }
            pDisplay->showModeAndPercentage("AUTOMATIC", percentage);
            MsgService.sendMsg("VALVE:" + String(percentage));
        }
        else
        {
            setState(UNCONNECTED);
        }

        break;

    case MANUAL:
        if (!pContext->isUnconnected())
        {
            pContext->setManual();
            if (this->checkAndSetJustEntered())
            {
                Logger.log("[FSM] Entered MANUAL");
                pContext->setDBSControlActive(false);
            }
            if (pButton->isPressed())
            {
                setState(AUTOMATIC);
                sendModeToCUS("AUTOMATIC");
            }
            float potValue = pContext->getPotValue();
            
            if (pContext->isDBSControlActive())
            {
                float target = pContext->getTargetValveFromDBS();
                if (abs(potValue - target) <= PICKUP_THRESHOLD)
                {
                    pContext->setDBSControlActive(false);
                    Logger.log("[FSM] Pot picked up DBS control");
                }
            }
            else
            {
                int angle = (int)((1.0 - potValue) * 90);
                int percentage = (int)(potValue * 100);
                pValveMotor->manuallySetAngle(angle);
                pDisplay->showModeAndPercentage("MANUAL", percentage);
            }
        }
        else
        {
            setState(UNCONNECTED);
        }

        break;

    case UNCONNECTED:

        if (this->checkAndSetJustEntered())
        {
            pContext->setUnconnected();
            pDisplay->showModeAndPercentage("UNCONNECTED", 0);
        }

        else if (pContext->isAutomatic())
        {
            setState(AUTOMATIC);
        }
        else if (pContext->isManual())
        {
            setState(MANUAL);
        }
    }
}

void FSMController::handleIncomingMessage()
{
    if (MsgService.isMsgAvailable())
    {
        Msg *msg = MsgService.receiveMsg();
        String content = msg->getContent();
        
        Logger.log("[FSM] Received: " + content);

        if (content.startsWith("MODE:"))
        {
            String mode = content.substring(5);
            if (mode.equals("MANUAL"))
            {
                if (state != MANUAL)
                {
                    setState(MANUAL);
                }
            }
            else if (mode.equals("AUTOMATIC"))
            {
                if (state != AUTOMATIC)
                {
                    setState(AUTOMATIC);
                }
            }
        }
        else if (content.startsWith("VALVE:"))
        {
            String val = content.substring(6);
            int percent = val.toInt();
            if (percent >= 0 && percent <= 100)
            {
                applyValveFromCUS(percent);
            }
        }
        
        delete msg;
    }
}

void FSMController::sendModeToCUS(const String &mode)
{
    MsgService.sendMsg("MODE:" + mode);
    Logger.log("[FSM] Sent MODE to CUS: " + mode);
}

void FSMController::applyValveFromCUS(int percent)
{
    float potValue = (float)percent / 100.0;
    
    if (state == MANUAL)
    {
        pContext->setTargetValveFromDBS(potValue);
        pContext->setDBSControlActive(true);
        
        int angle = (int)((1.0 - potValue) * 90);
        pValveMotor->manuallySetAngle(angle);
        
        pDisplay->showModeAndPercentage("MANUAL", percent);
        Logger.log("[FSM] Valve set from CUS: " + String(percent) + "%");
    }
    else if (state == AUTOMATIC)
    {
        if (percent == 50)
        {
            pValveMotor->half();
        }
        else if (percent == 100)
        {
            pValveMotor->open();
        }
        else
        {
            pValveMotor->close();
        }
        pDisplay->showModeAndPercentage("AUTOMATIC", percent);
        Logger.log("[FSM] Valve set from CUS (auto): " + String(percent) + "%");
    }
}

void FSMController::setState(ControllerState newState)
{
    state = newState;
    stateTimestamp = millis();
    justEntered = true;
}

long FSMController::elapsedTimeInState()
{
    return millis() - stateTimestamp;
}

bool FSMController::checkAndSetJustEntered()
{
    bool bak = justEntered;
    if (justEntered)
    {
        justEntered = false;
    }
    return bak;
}

void FSMController::log(const String &msg)
{
    Logger.log(msg);
}
