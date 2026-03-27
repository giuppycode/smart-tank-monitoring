#include "FSMController.h"
#include <Arduino.h>
#include "kernel/Logger.h"

#define L1 0.20
#define L2 0.30

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
            }
            if (pButton->isPressed())
            {
                setState(AUTOMATIC);
            }
            float potValue = pContext->getPotValue();
            int angle = (int)((1.0 - potValue) * 90); // Inverted: 0% pot = 90° (closed), 100% pot = 0° (open)
            int percentage = (int)(potValue * 100); // potValue 0-1 maps to 0-100%

            pValveMotor->manuallySetAngle(angle);
            pDisplay->showModeAndPercentage("MANUAL", percentage);
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
