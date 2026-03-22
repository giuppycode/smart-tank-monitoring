#ifndef __HANGAR_DOOR__
#define __HANGAR_DOOR__

#include <Arduino.h>
#include "ServoTimer2.h"

class HangarDoor
{
public:
    HangarDoor(int pin);

    void open();
    void close();
    void forceClose();
    bool isOpen();

private:
    int _pin;
    bool _isOpen;
    ServoTimer2 motor;

    // Funzione privata per gestire la conversione dell'angolo
    void setAngle(int angle);
};

#endif