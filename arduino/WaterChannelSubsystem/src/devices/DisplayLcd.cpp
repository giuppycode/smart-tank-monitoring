#include "DisplayLcd.h"

DisplayLcd::DisplayLcd(int i2c_address, int cols, int rows)
    : lcd(i2c_address, cols, rows), columns(cols), rows(rows), currentMessage("")
{
}

void DisplayLcd::init()
{
    lcd.init();
    lcd.backlight();
    clear();
}

void DisplayLcd::showMessage(const String &message)
{
    if (message != currentMessage)
    {
        clear();
        lcd.setCursor(0, 0);
        lcd.print(message);
        currentMessage = message;
    }
}

void DisplayLcd::showPercentage(int percentage)
{
    lcd.setCursor(0, 1);
    lcd.print("Valve: ");
    lcd.print(percentage);
    lcd.print("%   ");
}

void DisplayLcd::showModeAndPercentage(const String &mode, int percentage)
{
    clear();
    lcd.setCursor(0, 0);
    lcd.print(mode);
    lcd.setCursor(0, 1);
    lcd.print("Valve: ");
    lcd.print(percentage);
    lcd.print("%   ");
    currentMessage = mode;
}

void DisplayLcd::clear()
{
    lcd.clear();
    currentMessage = "";
}

void DisplayLcd::setBacklight(bool state)
{
    if (state)
    {
        lcd.backlight();
    }
    else
    {
        lcd.noBacklight();
    }
}

String DisplayLcd::getCurrentMessage() const
{
    return currentMessage;
}