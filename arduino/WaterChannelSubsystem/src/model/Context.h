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

  void setTargetValveFromDBS(float value);
  float getTargetValveFromDBS();
  void setDBSControlActive(bool active);
  bool isDBSControlActive();

  void setCurrentDistance(float dist);
  float getCurrentDistance();

private:
  bool automatic;
  bool manual;
  bool unconnected;
  float potValue;
  float targetValveFromDBS;
  bool dbsControlActive;
  float currentDistance;
};

#endif