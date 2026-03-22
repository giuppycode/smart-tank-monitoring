#ifndef DISPLAY_LCD_H
#define DISPLAY_LCD_H

#include <Arduino.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

class DisplayLcd
{
private:
  LiquidCrystal_I2C lcd;

  int columns;
  int rows;
  String currentMessage;

public:
  DisplayLcd(int i2c_address, int cols, int rows);

  void init();

  void showMessage(const String &message);
  void clear();
  void setBacklight(bool state);

  String getCurrentMessage() const;
};

#endif