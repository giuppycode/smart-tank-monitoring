#include "Context.h"

Context::Context()
{
  automatic = false;
  manual = false;
  unconnected = false;
  potValue = 0;
  targetValveFromDBS = 0;
  dbsControlActive = false;
  currentDistance = 0.0;
}

void Context::setAutomatic()
{
  automatic = true;
  manual = false;
  unconnected = false;
}

void Context::setManual()
{
  manual = true;
  automatic = false;
  unconnected = false;
}

void Context::setUnconnected()
{
  unconnected = true;
  automatic = false;
  manual = false;
}
void Context::setPotValue(float value)
{
  potValue = value;
}
void Context::setCurrentDistance(float dist)
{
  currentDistance = dist;
}

bool Context::isAutomatic()
{
  return automatic;
}

bool Context::isManual()
{
  return manual;
}

bool Context::isUnconnected()
{
  return unconnected;
}

float Context::getPotValue()
{
  return potValue;
}

void Context::setTargetValveFromDBS(float value)
{
  targetValveFromDBS = value;
}

float Context::getTargetValveFromDBS()
{
  return targetValveFromDBS;
}

void Context::setDBSControlActive(bool active)
{
  dbsControlActive = active;
}

bool Context::isDBSControlActive()
{
  return dbsControlActive;
}

float Context::getCurrentDistance()
{
  return currentDistance;
}