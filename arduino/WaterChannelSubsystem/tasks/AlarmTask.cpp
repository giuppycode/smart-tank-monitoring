#include "AlarmTask.h"
#include "config.h"
#include "kernel/Logger.h"
#include <Arduino.h>

// Parametri richiesti dalla consegna
#define TEMP1 22.0
#define TEMP2 23.0
#define T3 3000
#define T4 3000

AlarmTask::AlarmTask(TempSensor *pTempSensor, Button *pResetButton,
                     Led *pAlarmLed, DisplayLcd *pDisplay,
                     HangarDoor *pHangarDoor, Context *pContext) {
    this->pTempSensor = pTempSensor;
    this->pResetButton = pResetButton;
    this->pAlarmLed = pAlarmLed;
    this->pDisplay = pDisplay;
    this->pHangarDoor = pHangarDoor;
    this->pContext = pContext;

    setState(NORMAL);
}

void AlarmTask::tick() {
    float currentTemp = pTempSensor->getTemperature();

    switch (state) {
    case NORMAL: {
        if (this->checkAndSetJustEntered()) {
            pContext->clearPreAlarm();
            pContext->clearAlarm();
            pAlarmLed->switchOff();
        }

        if (currentTemp >= TEMP2) {
            setState(CHECK_ALARM);
        } else if (currentTemp >= TEMP1) {
            setState(CHECK_PRE_ALARM);
        }
        break;
    }
    case CHECK_PRE_ALARM: {
        if (currentTemp >= TEMP2) {
            setState(CHECK_ALARM);
        } else if (currentTemp < TEMP1) {
            setState(NORMAL);
        } else if (elapsedTimeInState() >= T3) {
            setState(PRE_ALARM);
        }
        break;
    }
    case PRE_ALARM: {
        if (this->checkAndSetJustEntered()) {
            Logger.log(F("[ALARM] PRE-ALARM TRIGGERED!"));
            pContext->setPreAlarm();
        }

        if (currentTemp >= TEMP2) {
            setState(CHECK_ALARM);
        } else if (currentTemp < TEMP1) {
            setState(NORMAL);
        }
        break;
    }
    case CHECK_ALARM: {
        if (currentTemp < TEMP2) {
            // Se scende sotto TEMP2, passiamo a PRE_ALARM per ricontrollare se
            // è ancora sopra TEMP1
            setState(PRE_ALARM);
        } else if (elapsedTimeInState() >= T4) {
            setState(ALARM);
        }
        break;
    }
    case ALARM: {
        if (this->checkAndSetJustEntered()) {
            Logger.log(F("[ALARM] SYSTEM BLOCKED!"));
            pContext->setAlarm();

            pAlarmLed->switchOn();
            pDisplay->showMessage("ALARM");

            if (pHangarDoor->isOpen()) {
                pHangarDoor->close();
                Logger.log(F("[ALARM] DOOR FORCE CLOSED"));
            }

            // Inviare il segnale DRU via seriale se il drone è fuori
            // if (pContext->isOutside()) {
            //     Serial.println("ALARM_DRONE_OUTSIDE");
            // }
        }

        // Il sistema si sblocca SOLO con la pressione del bottone fisico
        if (pResetButton->isPressed()) {
            Logger.log(F("[ALARM] RESET BY OPERATOR"));

            // Rimettiamo la scritta corretta sull'LCD in base a dove si trova
            // il drone
            /*
            if (pContext->isOutside()) {
                pDisplay->showMessage("DRONE OUTSIDE");
            } else {
                pDisplay->showMessage("DRONE INSIDE");
            }
            */
            pDisplay->clear();
            setState(NORMAL);
        }
        break;
    }
    }
}

void AlarmTask::setState(AlarmState newState) {
    state = newState;
    stateTimestamp = millis();
    justEntered = true;
}

long AlarmTask::elapsedTimeInState() { return millis() - stateTimestamp; }

bool AlarmTask::checkAndSetJustEntered() {
    bool bak = justEntered;
    if (justEntered) {
        justEntered = false;
    }
    return bak;
}