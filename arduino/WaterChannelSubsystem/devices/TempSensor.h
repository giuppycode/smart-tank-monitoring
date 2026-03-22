#ifndef TEMP_SENSOR_H
#define TEMP_SENSOR_H

#include <Arduino.h>
#include <DHT.h> // Includi la libreria per il DHT

class TempSensor
{
private:
    int pin;
    DHT dht; // Dichiariamo l'oggetto dht

public:
    // Il costruttore ora ha bisogno di sapere il pin (il tipo di default è DHT11)
    TempSensor(int pin);

    // Metodo necessario per avviare il sensore nel setup()
    void begin();

    float getTemperature();
};

#endif