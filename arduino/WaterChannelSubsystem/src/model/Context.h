#ifndef __CONTEXT__
#define __CONTEXT__

class Context
{

public:
  Context();

  void setAutomatic();
  bool isAutomatic();

  void setManual();
  bool isManual();

  void setUnconnected();
  bool isUnconnected();

  void setPotValue(float value);
  float getPotValue();

  void setValvePercent(int percent);
  int getValvePercent();

  void setCurrentDistance(float dist);
  float getCurrentDistance();

private:
  bool automatic;
  bool manual;
  bool unconnected;
  float potValue;
  int valvePercent;
  float currentDistance;
};

#endif