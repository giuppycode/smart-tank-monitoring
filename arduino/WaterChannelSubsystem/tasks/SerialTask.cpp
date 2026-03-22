#include "SerialTask.h"
#include <Arduino.h>

#define SEND_INTERVAL 500 // Invia i dati alla GUI ogni mezzo secondo

SerialTask::SerialTask(Context *pContext) {
    this->pContext = pContext;
    this->lastSendTime = 0;
}

void SerialTask::tick() {

    // --- 1. ASCOLTO (Usando MsgService del prof) ---
    if (MsgService.isMsgAvailable()) {
        Msg *msg = MsgService.receiveMsg(); // Prende il messaggio
        String content = msg->getContent(); // Estrae la stringa
        content.trim();                     // Pulisce spazi e \n

        if (content == "TAKEOFF") {
            pContext->setTakeOffCommand();
        } else if (content == "LAND") {
            pContext->setLandCommand();
        }

        delete msg;
    }

    // --- 2. PARLATO (Invio periodico) ---
    // Dato che questo task girerà velocemente per leggere i messaggi in tempo
    // reale, usiamo millis() per assicurarci di inviare lo stato solo ogni
    // 500ms.
    if (millis() - lastSendTime > SEND_INTERVAL) {
        lastSendTime = millis();

        String statusMsg = "";

        // A. Stato del Drone
        if (pContext->isTakingOff())
            statusMsg += "STATE:TAKING_OFF|";
        else if (pContext->isLanding())
            statusMsg += "STATE:LANDING|";
        else if (pContext->isOutside())
            statusMsg += "STATE:OUTSIDE|";
        else
            statusMsg += "STATE:IDLE_INSIDE|";

        // B. Stato dell'Hangar
        if (pContext->isAlarming())
            statusMsg += "HANGAR:ALARM|";
        else
            statusMsg += "HANGAR:NORMAL|";

        // C. Distanza
        if (pContext->isLanding() || pContext->isTakingOff()) {
            float dist = pContext->getCurrentDistance();
            statusMsg += "DIST:" + String(dist);
        } else {
            statusMsg += "DIST:---";
        }

        // Invia usando la classe del prof
        MsgService.sendMsg(statusMsg);
    }
}