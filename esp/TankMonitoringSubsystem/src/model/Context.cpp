#include "Context.h"

Context::Context()
{
  currentDistance = 0.0;
  connected = false;
}

void Context::setCurrentDistance(float dist)
{
  currentDistance = dist;
}

float Context::getCurrentDistance()
{
  return currentDistance;
}

void Context::setConnected(bool conn)
{
  this->connected = conn;
}

bool Context::isConnected()
{
  return connected;
}