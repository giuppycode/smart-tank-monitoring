#include "ValveMotor.h"
#include <Arduino.h>

ValveMotor::ValveMotor(int pin)
{
    _pin = pin;
    currentAngle = -1;
    currentPercent = 0;
}

bool ValveMotor::isAttached()
{
    return motor.attached();
}

void ValveMotor::safeAttach()
{
    if (!motor.attached())
    {
        motor.attach(_pin);
    }
}

void ValveMotor::safeDetach()
{
    if (motor.attached())
    {
        motor.detach();
    }
}

void ValveMotor::setAngle(int angle)
{
    if (angle > 90)
    {
        angle = 90;
    }
    else if (angle < 0)
    {
        angle = 0;
    }

    if (angle != currentAngle)
    {
        safeAttach();
        float coeff = (2400.0 - 544.0) / 180.0;
        motor.write(544 + angle * coeff);
        currentAngle = angle;
        currentPercent = map(angle, 0, 90, 100, 0);
        delay(500);
        safeDetach();
    }
}

void ValveMotor::open()
{
    setAngle(90);
}

void ValveMotor::half()
{
    setAngle(45);
}

void ValveMotor::close()
{
    setAngle(0);
}

void ValveMotor::manuallySetAngle(int angle)
{
    setAngle(angle);
}

int ValveMotor::getOpeningPercent()
{
    return currentPercent;
}

int ValveMotor::getCurrentAngle()
{
    return currentAngle;
}
