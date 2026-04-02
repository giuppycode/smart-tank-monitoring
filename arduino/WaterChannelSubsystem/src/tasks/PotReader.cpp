#include "PotReader.h"
#include <Arduino.h>
#include <stdlib.h>
#include "kernel/Logger.h"

#define PICKUP_THRESHOLD 0.02

PotReader::PotReader(Potentiometer *pPot, ValveMotor *pValveMotor, Context *pContext)
{
    this->pPot = pPot;
    this->pValveMotor = pValveMotor;
    this->pContext = pContext;
    this->conditionStartTime = 0;
    this->lastPotValue = -1.0;
    setState(IDLE);
}

void PotReader::tick()
{
    switch (state)
    {
    case IDLE:
        if (pContext->isManual())
        {
            setState(READING);
            lastPotValue = -1.0;
        }
        break;

    case READING:
        if (pContext->isManual())
        {
            pPot->sync();
            float potValue = pPot->getValue();
            pContext->setPotValue(potValue);
            
            if (pContext->isDBSControlActive())
            {
                float target = pContext->getTargetValveFromDBS();
                if (abs(potValue - target) <= PICKUP_THRESHOLD)
                {
                    pContext->setDBSControlActive(false);
                    moveMotorToPot();
                    int percentage = (int)(potValue * 100);
                    sendValveToCUS(percentage);
                    lastPotValue = potValue;
                    Logger.log("[PotReader] DBS control taken over by pot");
                }
            }
            else
            {
                int percentage = (int)(potValue * 100);
                if (lastPotValue < 0 || abs(potValue - lastPotValue) > 0.02)
                {
                    sendValveToCUS(percentage);
                    lastPotValue = potValue;
                }
            }
        }
        else if (pContext->isAutomatic())
        {
            setState(IDLE);
        }
    }
}

void PotReader::moveMotorToPot()
{
    float potValue = pPot->getValue();
    int angle = (int)((1.0 - potValue) * 90);
    pValveMotor->manuallySetAngle(angle);
}

void PotReader::sendValveToCUS(int percent)
{
    MsgService.sendMsg("VALVE:" + String(percent));
    Logger.log("[PotReader] Sent VALVE to CUS: " + String(percent) + "%");
}

void PotReader::setState(PotentiometerState newState)
{
    state = newState;
    stateTimestamp = millis();
    justEntered = true;
}

long PotReader::elapsedTimeInState()
{
    return millis() - stateTimestamp;
}

bool PotReader::checkAndSetJustEntered()
{
    bool bak = justEntered;
    if (justEntered)
    {
        justEntered = false;
    }
    return bak;
}

void PotReader::log(const String &msg)
{
    Logger.log(msg);
}
