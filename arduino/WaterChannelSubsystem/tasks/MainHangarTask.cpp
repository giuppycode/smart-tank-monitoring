#include "MainHangarTask.h"
#include <Arduino.h>
#include "kernel/Logger.h"

#define T1 2000
#define T2 5000
#define TAKE_OFF_DISTANCE 0.04
#define LANDING_DISTANCE 0.08

MainHangarTask::MainHangarTask(PresenceSensor *pPresenceSensor, ProximitySensor *pProximitySensor,
                               DisplayLcd *pDisplay, Led *pStaticLed, HangarDoor *pHangarDoor, Context *pContext)
{
    this->pPresenceSensor = pPresenceSensor;
    this->pProximitySensor = pProximitySensor;
    this->pDisplay = pDisplay;
    this->pStaticLed = pStaticLed;
    this->pHangarDoor = pHangarDoor;
    this->pContext = pContext;

    this->conditionStartTime = 0;
    setState(IDLE_INSIDE); // initial state
}

void MainHangarTask::tick()
{
    switch (state)
    {
    case IDLE_INSIDE:
        pContext->setIdleInside();
        {
            if (this->checkAndSetJustEntered())
            {
                Logger.log(F("[MHT] IDLE_INSIDE"));
                closeDoorIfOpen();
                pStaticLed->switchOn();
                Logger.log(F("[LED] ON"));
                pDisplay->showMessage("DRONE INSIDE");
            }

            // float currentDistance = pProximitySensor->getDistance();
            // pContext->setCurrentDistance(currentDistance);

            // Transition condition: take-off command received and no prealarm
            if (pContext->isTakeOffCommanded() && (!pContext->isPreAlarming()))
            {
                setState(TAKING_OFF);
            }
            // Serial.println(pContext->getCurrentDistance());
            break;
        }
    case TAKING_OFF:
        pContext->setTakingOff();
        {
            if (this->checkAndSetJustEntered())
            {
                Logger.log(F("[MHT] TAKING_OFF"));
                openDoorIfClosed();
                pDisplay->showMessage("TAKING OFF");
                // AZZERA il timer SOLO quando entri in questo stato per la prima volta
                conditionStartTime = 0;
                pContext->clearCommands();
            }

            // Transition condition: drone has taken off
            float currentDistance = pProximitySensor->getDistance();
            pContext->setCurrentDistance(currentDistance);
            if (currentDistance > TAKE_OFF_DISTANCE)
            {
                setState(CHECK_TAKING_OFF);
            }
            break;
        }

    case CHECK_TAKING_OFF:
    {
        float currentDistance = pProximitySensor->getDistance();
        pContext->setCurrentDistance(currentDistance);
        Logger.log(F("[CHECK_TAKING_OFF] ENTERED"));

        if (currentDistance < TAKE_OFF_DISTANCE)
        {
            Logger.log(F("[CHECK_TAKING_OFF] EXIT: TAKE OFF ABORTED"));
            setState(IDLE_INSIDE);
        }
        else if (elapsedTimeInState() >= T1)
        {
            setState(OUTSIDE);
        }
        break;
    }

    case OUTSIDE:
        pContext->setOutside();
        {
            if (this->checkAndSetJustEntered())
            {
                Logger.log(F("[MHT] OUTSIDE"));
                closeDoorIfOpen();
                pDisplay->showMessage("DRONE OUTSIDE");
            }
            // Transition condition: drone is landingand no alarm

            if (pContext->isLandCommanded() && !pContext->isPreAlarming() && pPresenceSensor->isDetected())
            {
                setState(LANDING);
            }
            break;
        }
    case LANDING:
        pContext->setLanding();
        {
            if (this->checkAndSetJustEntered())
            {
                Logger.log(F("[MHT] LANDING"));
                openDoorIfClosed();
                pDisplay->showMessage("LANDING");
                conditionStartTime = 0;
                pContext->clearCommands();
            }

            // Transition condition: drone has landed
            float currentDistance = pProximitySensor->getDistance();
            pContext->setCurrentDistance(currentDistance);
            if (currentDistance < LANDING_DISTANCE)
            {
                setState(CHECK_LANDING);
            }
            break;
        }

    case CHECK_LANDING:
    {
        float currentDistance = pProximitySensor->getDistance();
        pContext->setCurrentDistance(currentDistance);
        Logger.log(F("[CHECK_LANDING] ENTERED"));

        if (currentDistance > LANDING_DISTANCE)
        {
            Logger.log(F("[CHECK_LANDING] EXIT: LANDING ABORTED"));
            setState(OUTSIDE);
        }
        else if (elapsedTimeInState() >= T2)
        {
            setState(IDLE_INSIDE);
        }
        break;
    }
    }
}

void MainHangarTask::setState(HangarState newState)
{
    state = newState;
    stateTimestamp = millis();
    justEntered = true;
}

long MainHangarTask::elapsedTimeInState()
{
    return millis() - stateTimestamp;
}

bool MainHangarTask::checkAndSetJustEntered()
{
    bool bak = justEntered;
    if (justEntered)
    {
        justEntered = false;
    }
    return bak;
}

// Utility functions to manage the door
void MainHangarTask::closeDoorIfOpen()
{
    if (pHangarDoor->isOpen())
    {
        pHangarDoor->close();
        Logger.log(F("[MHT] DOOR CLOSED"));
    }
}

void MainHangarTask::openDoorIfClosed()
{
    if (!pHangarDoor->isOpen())
    {
        pHangarDoor->open();
        Logger.log(F("[MHT] DOOR OPENED"));
    }
}