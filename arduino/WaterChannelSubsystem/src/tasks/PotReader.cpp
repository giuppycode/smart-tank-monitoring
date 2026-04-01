#include "PotReader.h"
#include <Arduino.h>
#include "kernel/Logger.h"

PotReader::PotReader(Potentiometer *pPot, Context *pContext)
{
    this->pPot = pPot;
    this->pContext = pContext;
    this->conditionStartTime = 0;
    this->lastPotValue = -1.0;  // Initialize to invalid value to force first update
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
            lastPotValue = -1.0;  // Reset to force first send
        }
        break;

    case READING:
        if (pContext->isManual())
        {
            pPot->sync();
            float potValue = pPot->getValue();
            pContext->setPotValue(potValue);
            
            int percentage = (int)(potValue * 100);
            
            // Send to CUS only if value changed significantly (threshold of 2%)
            if (lastPotValue < 0 || abs(potValue - lastPotValue) > 0.02)
            {
                sendValveToCUS(percentage);
                lastPotValue = potValue;
            }
        }
        else if (pContext->isAutomatic())
        {
            setState(IDLE);
        }
    }
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
