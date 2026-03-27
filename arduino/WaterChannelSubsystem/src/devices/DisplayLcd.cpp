#include "DisplayLcd.h"

DisplayLcd::DisplayLcd(int i2c_address, int cols, int rows)
    : lcd(i2c_address, cols, rows), columns(cols), rows(rows), currentLine1(""), currentLine2("")
{
}

void DisplayLcd::init()
{
    lcd.init();
    lcd.backlight();
    clear();
}

void DisplayLcd::showMessage(const String &line1)
{
    showMessage(line1, "");
}

void DisplayLcd::showMessage(const String &line1, const String &line2)
{
    bool line1Changed = line1 != currentLine1;
    bool line2Changed = line2 != currentLine2;

    if (line1Changed || line2Changed)
    {
        clear();
        lcd.setCursor(0, 0);
        lcd.print(line1);
        currentLine1 = line1;

        if (line2.length() > 0)
        {
            lcd.setCursor(0, 1);
            lcd.print(line2);
        }
        currentLine2 = line2;
    }
}

void DisplayLcd::clear()
{
    lcd.clear();
    currentLine1 = "";
    currentLine2 = "";
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

String DisplayLcd::getCurrentLine1() const
{
    return currentLine1;
}

String DisplayLcd::getCurrentLine2() const
{
    return currentLine2;
}