#include "Context.h"

Context::Context()
{
  takingOff = false;
  landing = false;
  alarm = false;
  preAlarm = false;
  outside = false;
  currentDistance = 0.0;
  takeOffCommand = false;
  landCommand = false;
}

// --- SERIAL TASK SETTER ---

void Context::setTakeOffCommand()
{
  takeOffCommand = true;
}

void Context::setLandCommand()
{
  landCommand = true;
}

void Context::clearCommands()
{
  takeOffCommand = false;
  landCommand = false;
}

// --- SETTERS ---

void Context::setCurrentDistance(float dist)
{
  currentDistance = dist;
}

void Context::setIdleInside()
{
  takingOff = false;
  landing = false;
  outside = false;
}

void Context::setTakingOff()
{
  takingOff = true;
  landing = false;
  outside = false;
}

void Context::setOutside()
{
  takingOff = false;
  landing = false;
  outside = true;
}

void Context::setLanding()
{
  takingOff = false;
  landing = true;
  outside = true;
}

// --- ALARM SETTERS ---

void Context::setAlarm()
{
  alarm = true;
}

void Context::setPreAlarm()
{
  preAlarm = true;
}

void Context::clearPreAlarm()
{
  preAlarm = false;
}

void Context::clearAlarm()
{
  alarm = false;
}

// --- GETTERS ---

float Context::getCurrentDistance()
{
  return currentDistance;
}

bool Context::isPreAlarming()
{
  return preAlarm;
}

bool Context::isAlarming()
{
  return alarm;
}

bool Context::isTakingOff()
{
  return takingOff;
}

bool Context::isLanding()
{
  return landing;
}

bool Context::isOutside()
{
  return outside;
}

bool Context::isTakeOffCommanded()
{
  return takeOffCommand;
}

bool Context::isLandCommanded()
{
  return landCommand;
}