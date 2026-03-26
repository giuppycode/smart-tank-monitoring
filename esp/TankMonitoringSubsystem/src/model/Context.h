#ifndef __CONTEXT__
#define __CONTEXT__

class Context
{

public:
  Context();

  void setCurrentDistance(float dist);
  float getCurrentDistance();
  void setConnected(bool conn);
  bool isConnected();

private:
  float currentDistance;
  bool connected;
};

#endif