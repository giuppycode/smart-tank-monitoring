#include "ValveMotor.h"

ValveMotor::ValveMotor(int pin)
{
    _pin = pin;
    // Il motore non viene "attaccato" subito per risparmiare corrente
}

void ValveMotor::setAngle(int angle)
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

void ValveMotor::open()
{
    motor.attach(_pin);
    setAngle(0);
    delay(1000);
    motor.detach();
}

void ValveMotor::half()
{
    motor.attach(_pin);
    setAngle(45);
    delay(1000);
    motor.detach();
}

void ValveMotor::close()
{
    motor.attach(_pin);
    setAngle(90);
    delay(1000);
    motor.detach();
}

void ValveMotor::manuallySetAngle(int angle)
{
    motor.attach(_pin);
    setAngle(angle);
    delay(1000);
    motor.detach();
}
