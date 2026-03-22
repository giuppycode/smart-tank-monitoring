#ifndef __CONTEXT__
#define __CONTEXT__

class Context
{

public:
  Context();

  /* Ho bisogno di queste perchè voglio rendere SerialTask completamnente indipendente dal modello dei device
  quindi siccome devo displayare la distanza anche in SerialTask per poterla inviare alla GUI,
  ho bisogno di un modo per far arrivare la distanza letta dal ProximitySensor a SerialTask senza dover
  includere il ProximitySensor stesso in SerialTask (per mantenere l'indipendenza).
    Quindi Context funge da "ponte" per trasportare questa informazione. */
  void setCurrentDistance(float dist);
  float getCurrentDistance();

  // MainHangarTask setters
  void setIdleInside();
  void setTakingOff();
  void setOutside();
  void setLanding();

  //  AlarmTask setters
  void setPreAlarm();
  void clearPreAlarm();
  void setAlarm();
  void clearAlarm();

  bool isPreAlarming();
  bool isAlarming();
  bool isTakingOff();
  bool isLanding();
  bool isOutside();

  // SerialTask setters/getters
  void setTakeOffCommand();
  void setLandCommand();
  void clearCommands();

  bool isTakeOffCommanded();
  bool isLandCommanded();

private:
  float currentDistance;
  bool preAlarm;
  bool alarm;
  bool takingOff;
  bool landing;
  bool outside;
  bool takeOffCommand;
  bool landCommand;
};

#endif