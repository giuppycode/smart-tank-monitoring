#include "TempSensor.h"

// Inizializza le variabili e l'oggetto dht specificando il tipo (DHT11)
TempSensor::TempSensor(int pin) : pin(pin), dht(pin, DHT11)
{
    // È meglio non fare operazioni hardware (come i pinMode o dht.begin)
    // direttamente nel costruttore in C++ per Arduino.
    // Lo faremo nel metodo begin().
}

void TempSensor::begin()
{
    // Avvia la comunicazione con il DHT11
    dht.begin();
}

float TempSensor::getTemperature()
{
    // La libreria legge i dati digitali e fa la conversione in automatico
    float temperatureC = dht.readTemperature();

    // Buona pratica: controlla se la lettura è fallita (restituisce NaN - Not a Number)
    if (isnan(temperatureC))
    {
        Serial.println("Errore nella lettura del sensore DHT!");
        return -999.0; // Restituisce un valore di errore evidente
    }
    else
    {

        Serial.print("Temperatura letta: ");
        Serial.print(temperatureC);
        Serial.println(" °C");
    }
    return temperatureC;
}