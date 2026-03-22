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