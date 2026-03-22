#include "HangarDoor.h"

HangarDoor::HangarDoor(int pin)
{
    _pin = pin;
    _isOpen = false;
    // Il motore non viene "attaccato" subito per risparmiare corrente
}

void HangarDoor::setAngle(int angle)
{
    if (angle > 180)
    {
        angle = 180;
    }
    else if (angle < 0)
    {
        angle = 0;
    }

    // updated values: min is 544, max 2400 (see ServoTimer2 doc)
    float coeff = (2400.0 - 544.0) / 180.0;
    motor.write(544 + angle * coeff);
}

void HangarDoor::open()
{
    if (!_isOpen)
    {
        motor.attach(_pin); // Accendi il motore
        setAngle(0);
        delay(1000);
        _isOpen = true;
    }
}

void HangarDoor::close()
{
    if (_isOpen)
    {
        motor.attach(_pin);
        setAngle(90); // 0 gradi per chiudere
        delay(1000);
        motor.detach(); // Spegni il motore per risparmiare corrente
        _isOpen = false;
    }
}

bool HangarDoor::isOpen()
{
    return _isOpen;
}

void HangarDoor::forceClose() {
    motor.attach(_pin);
    setAngle(90); // 0 gradi per chiudere
    delay(1000);
    motor.detach(); // Spegni il motore per risparmiare corrente
    _isOpen = false;
}