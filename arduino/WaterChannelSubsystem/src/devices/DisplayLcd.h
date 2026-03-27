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
  String currentLine1;
  String currentLine2;

public:
  DisplayLcd(int i2c_address, int cols, int rows);

  void init();

  void showMessage(const String &line1);
  void showMessage(const String &line1, const String &line2);
  void clear();
  void setBacklight(bool state);

  String getCurrentLine1() const;
  String getCurrentLine2() const;
};

#endif