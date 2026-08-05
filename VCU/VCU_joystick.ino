/*
 * VCU_joystick.ino  —  BhoomiBot OS  VCU / ESP32 firmware
 * -----------------------------------------------------------------------------
 * UPDATED: Added pins 18 & 19 for isolated Reverse control via PC817 Optocouplers.
 */

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled!
#endif

// ----------------------------- Calibration Macros -----------------------------
#define DRIVE_CALIB_UP    2.2f
#define DRIVE_CALIB_DOWN  2.2f
#define RPM_CALIB_L       1.0f
#define RPM_CALIB_R       1.0f

// --- Pins ---
#define FEEDBACK_PIN_L 32
#define FEEDBACK_PIN_R 35
#define RPM_DEADZONE   80

// --- NEW: Reverse Control Pins (via PC817 Optocouplers) ---
#define REV_PIN_LEFT   18
#define REV_PIN_RIGHT  19

// ----------------------------- Configuration -----------------------------
#define BT_DEVICE_NAME "ESP32_Robot"
#define DAC_PIN_RIGHT 25
#define DAC_PIN_LEFT  26
#define REV_PIN       27   // Legacy shared reverse pin
#define PTO_PIN       4
#define LIGHTS_PIN    16
#define HYD_PIN       14
#define HORN_PIN      17

#define MIN_PWM      30
#define MAX_PWM      255
#define TURN_FACTOR  0.5f
#define HORN_MS      300
#define WATCHDOG_MS  0

// ----------------------------- Globals -----------------------------
BluetoothSerial SerialBT;

enum Dir { STOP, FWD, REV, LEFT, RIGHT, ESTOP };
Dir  g_dir   = STOP;
int  g_speed = 0;
bool g_pto   = false;
bool g_lights = false;
int  g_hyd    = 0;
unsigned long hornUntil = 0;
unsigned long lastRpmMs = 0;
String btBuf;
unsigned long lastCmdMs = 0;

// ----------------------------- Setup -----------------------------
void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n\n--- BhoomiBot VCU Booting ---");

  pinMode(REV_PIN, OUTPUT);
  digitalWrite(REV_PIN, LOW);

  // Initialize NEW Reverse Pins for Optocouplers
  pinMode(REV_PIN_LEFT, OUTPUT);
  pinMode(REV_PIN_RIGHT, OUTPUT);
  digitalWrite(REV_PIN_LEFT, LOW);
  digitalWrite(REV_PIN_RIGHT, LOW);

  dacWrite(DAC_PIN_LEFT, 0);
  dacWrite(DAC_PIN_RIGHT, 0);

  pinMode(FEEDBACK_PIN_L, INPUT);
  pinMode(FEEDBACK_PIN_R, INPUT);

  if (PTO_PIN >= 0) pinMode(PTO_PIN, OUTPUT);
  if (LIGHTS_PIN >= 0) pinMode(LIGHTS_PIN, OUTPUT);
  if (HYD_PIN >= 0) pinMode(HYD_PIN, OUTPUT);
  if (HORN_PIN >= 0) { pinMode(HORN_PIN, OUTPUT); digitalWrite(HORN_PIN, LOW); }

  if (!SerialBT.begin(BT_DEVICE_NAME)) {
    Serial.println("An error occurred initializing Bluetooth");
  } else {
    Serial.println("Bluetooth Initialized: " BT_DEVICE_NAME);
  }

  Serial.println("VCU Ready.");
}

// ----------------------------- Loop -----------------------------
void loop() {
  if (SerialBT.available()) pump(SerialBT, btBuf);

  if (WATCHDOG_MS > 0 && g_dir != STOP && g_dir != ESTOP && (millis() - lastCmdMs) > (unsigned long)WATCHDOG_MS) {
    g_dir = STOP;
    applyDrive();
  }

  digitalWrite(HORN_PIN, (millis() < hornUntil) ? HIGH : LOW);
  sendRpmTelemetry();
  delay(10);
}

void pump(Stream &s, String &buf) {
  while (s.available()) {
    char ch = s.read();
    if (ch == '\n') {
      handleCommand(buf);
      buf = "";
    } else if (ch != '\r') {
      buf += ch;
    }
  }
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;
  Serial.print("CMD: "); Serial.println(cmd);

  if (cmd.startsWith("SPD")) {
    g_speed = constrain(cmd.substring(3).toInt(), -100, 100);
  } else if (cmd.startsWith("PTO")) {
    g_pto = (cmd.length() >= 4 && cmd[3] == '1');
    if (PTO_PIN >= 0) digitalWrite(PTO_PIN, g_pto ? HIGH : LOW);
  } else if (cmd.startsWith("LGT")) {
    g_lights = (cmd.length() >= 4 && cmd[3] == '1');
    if (LIGHTS_PIN >= 0) digitalWrite(LIGHTS_PIN, g_lights ? HIGH : LOW);
  } else if (cmd.startsWith("HYD")) {
    g_hyd = constrain(cmd.substring(3).toInt(), 0, 100);
    if (HYD_PIN >= 0) analogWrite(HYD_PIN, map(g_hyd, 0, 100, 0, 255));
  } else if (cmd == "HRN") {
    hornUntil = millis() + HORN_MS;
  } else {
    switch (cmd[0]) {
      case 'F': g_dir = FWD;   break;
      case 'B': g_dir = REV;   break;
      case 'L': g_dir = LEFT;  break;
      case 'R': g_dir = RIGHT; break;
      case 'S': g_dir = STOP;  break;
      case 'E': g_dir = ESTOP; break;
    }
  }

  lastCmdMs = millis();
  applyDrive();
}

void applyDrive() {
  int left = 0, right = 0;
  bool reverse = false;

  if (g_dir == STOP || g_dir == ESTOP || g_speed == 0) {
    left = right = 0;
    reverse = false;
  } else {
    int mag = abs(g_speed);
    float dirCalib = (g_speed > 0) ? DRIVE_CALIB_UP : DRIVE_CALIB_DOWN;
    int pwm = constrain(map(mag, 1, 100, MIN_PWM, MAX_PWM) * dirCalib, 0, MAX_PWM);
    reverse = (g_speed < 0);
    left = right = pwm;
    if (g_dir == LEFT)  left  = (int)(pwm * (1.0f - TURN_FACTOR));
    if (g_dir == RIGHT) right = (int)(pwm * (1.0f - TURN_FACTOR));
  }

  // 1. Shared Direction Pin
  digitalWrite(REV_PIN, reverse ? HIGH : LOW);

  // 2. Individual Reverse Pins for PC817 Optocouplers
  digitalWrite(REV_PIN_LEFT, reverse ? HIGH : LOW);
  digitalWrite(REV_PIN_RIGHT, reverse ? HIGH : LOW);

  dacWrite(DAC_PIN_LEFT,  left);
  dacWrite(DAC_PIN_RIGHT, right);
}

void sendRpmTelemetry() {
  if (millis() - lastRpmMs > 300) {
    long sumL = 0, sumR = 0;
    for(int i=0; i<8; i++) {
        sumL += analogRead(FEEDBACK_PIN_L);
        sumR += analogRead(FEEDBACK_PIN_R);
    }
    int dispL = (sumL/8 < RPM_DEADZONE) ? 0 : (int)(sumL/8 * RPM_CALIB_L);
    int dispR = (sumR/8 < RPM_DEADZONE) ? 0 : (int)(sumR/8 * RPM_CALIB_R);
    SerialBT.print("RPM "); SerialBT.print(dispL); SerialBT.print(","); SerialBT.println(dispR);
    lastRpmMs = millis();
  }
}
