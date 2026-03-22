#include "Pir.h"
#include "Arduino.h"

Pir::Pir(int pin)
{
  this->pin = pin;
  pinMode(pin, INPUT);
}

void Pir::sync()
{
  updateSyncTime(millis());
}

bool Pir::isDetected()
{
  if (digitalRead(pin) == HIGH)
  {
    detected = true;
  }
  else
  {
    detected = false;
  }

  return detected;
}

void Pir::calibrate()
{
  delay(10000);
}

void Pir::updateSyncTime(long time)
{
  lastTimeSync = time;
}

long Pir::getLastSyncTime()
{
  return lastTimeSync;
}
