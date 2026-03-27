#include "SerialTask.h"
#include <Arduino.h>

#define SEND_INTERVAL 500 // Invia i dati alla GUI ogni mezzo secondo

SerialTask::SerialTask(Context *pContext)
{
    this->pContext = pContext;
    this->lastSendTime = 0;
}

void SerialTask::tick()
{

    // --- 1. ASCOLTO (Usando MsgService del prof) ---
    if (MsgService.isMsgAvailable())
    {
        Msg *msg = MsgService.receiveMsg(); // Prende il messaggio
        String content = msg->getContent(); // Estrae la stringa
        content.trim();                     // Pulisce spazi e \n

        if (content.startsWith("AUTOMATIC"))
            pContext->setAutomatic();
        else if (content.startsWith("MANUAL"))
            pContext->setManual();
        else if (content.startsWith("UNCONNECTED"))
            pContext->setUnconnected();

        delete msg;
    }

    // --- 2. PARLATO (Invio periodico) ---
    // Dato che questo task girerà velocemente per leggere i messaggi in tempo
    // reale, usiamo millis() per assicurarci di inviare lo stato solo ogni
    // 500ms.
    if (millis() - lastSendTime > SEND_INTERVAL)
    {
        lastSendTime = millis();

        String statusMsg = "";

        if (pContext->isManual())
            statusMsg += "MANUAL";
        else if (pContext->isAutomatic())
            statusMsg += "AUTOMATIC";
        else if (pContext->isUnconnected())
            statusMsg += "UNCONNECTED";
        else
            statusMsg += "UNKNOWN";

        float valve = pContext->getPotValue();
        statusMsg += ",VALVE:" + String(int(valve));

        MsgService.sendMsg(statusMsg);
    }
}