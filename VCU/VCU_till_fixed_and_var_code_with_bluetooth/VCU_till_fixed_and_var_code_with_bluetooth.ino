#define CUSTOM_SETTINGS
#define INCLUDE_GAMEPAD_MODULE
#include <DabbleESP32.h>

#define DAC_PIN_RIGHT 25 //right motor
#define DAC_PIN_LEFT 26 //left
#define REV_PIN 27

bool cruiseMode = false;
float cruiseY_Default = 1.75;
float cruiseY = cruiseY_Default;   // Cruise speed level

void setup()
{
  Serial.begin(115200);

  pinMode(REV_PIN, OUTPUT);
  digitalWrite(REV_PIN, LOW);

  Dabble.begin("ESP32_ROBOT");
}

void loop()
{
  Dabble.processInput();

  // Increase Cruise Speed
  if (GamePad.isTrianglePressed())
  {
    cruiseY += 0.25;   // Smooth increment

    if (cruiseY > 7.0)
      cruiseY = cruiseY_Default;

    Serial.print("Cruise Level=");
    Serial.println(cruiseY);

    delay(300);
  }

  // Start Cruise Mode
  if (GamePad.isStartPressed())
  {
    cruiseMode = true;

    Serial.print("CRUISE STARTED Level=");
    Serial.println(cruiseY);

    delay(300);
  }

  // Stop Cruise Mode
  if (GamePad.isSelectPressed())
  {
    cruiseMode = false;
    cruiseY=cruiseY_Default;

    Serial.println("CRUISE STOPPED");

    delay(300);
  }

  float y;

  if (cruiseMode)
  {
    y = cruiseY;   // Fixed cruise speed
  }
  else
  {
    y = GamePad.getYaxisData();   // Normal joystick control
  }

  if (y > 1)
  {
    digitalWrite(REV_PIN, LOW);

    int speedValue = map(y, 1, 7, 30, 255);
    dacWrite(DAC_PIN_RIGHT, speedValue);
    dacWrite(DAC_PIN_LEFT, speedValue);

    Serial.print("FORWARD Speed=");
    Serial.println(speedValue);
  }
  else if (y < -1)
  {
    digitalWrite(REV_PIN, HIGH);

    int speedValue = map(abs(y), 1, 7, 30, 255);
    dacWrite(DAC_PIN_RIGHT, speedValue);
    dacWrite(DAC_PIN_LEFT, speedValue);

    Serial.print("REVERSE Speed=");
    Serial.println(speedValue);
  }
  else
  {
    digitalWrite(REV_PIN, LOW);

    dacWrite(DAC_PIN_RIGHT, 0);
    dacWrite(DAC_PIN_LEFT, 0);

    Serial.println("STOP");
  }

  delay(50);
}