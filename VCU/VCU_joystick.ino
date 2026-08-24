/*
 * VCU_joystick.ino  —  BhoomiBot OS  VCU / ESP32 firmware
 * -----------------------------------------------------------------------------
 * FINAL PRODUCTION VERSION: Comprehensive Movement, Turning, and Safety Logic.
 */

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled!
#endif

// ----------------------------- Calibration Macros -----------------------------
#define DRIVE_CALIB_L_UP    2.2f
#define DRIVE_CALIB_L_DOWN  2.2f
#define DRIVE_CALIB_R_UP    2.22f
#define DRIVE_CALIB_R_DOWN  2.22f

#define MIN_PWM_L    35
#define MIN_PWM_R    35
#define MAX_PWM_L    196
#define MAX_PWM_R    196
#define SLEW_RATE_L  5
#define SLEW_RATE_R  5

// --- Pin Mapping ---
#define SPEED_PIN_R    34  // D0 from Right Flux Sensor
#define SPEED_PIN_L    35  // D0 from Left Flux Sensor
#define REV_PIN_RIGHT  18
#define REV_PIN_LEFT   19
#define REV_PIN        27

#define BT_DEVICE_NAME "ESP32_Robot11"
#define DAC_PIN_RIGHT 25
#define DAC_PIN_LEFT  26
#define PTO_PIN       4
#define LIGHTS_PIN    16
#define HYD_PIN       14
#define HORN_PIN      17
#define STATUS_LED    2
#define BRAKE_PIN_L   5
#define BRAKE_PIN_R   33
#define HORN_MS       300
#define WATCHDOG_MS   0

// BRAKE LOGIC
#define BRAKE_ON      HIGH
#define BRAKE_OFF     LOW

// ----------------------------- Globals -----------------------------
BluetoothSerial SerialBT;
enum Dir { STOP, FWD, REV, LEFT, RIGHT, ESTOP };
Dir  g_dir   = STOP;
int  g_speed = 0;
int  g_turnIntensity = 0;
bool g_brakes = false;
bool g_pto = false, g_lights = false;
int  g_hyd = 0;
unsigned long hornUntil = 0, lastRpmMs = 0, lastCmdMs = 0, ledOffMs = 0;
int curDacL = 0, curDacR = 0;
String btBuf;

// Speed Sensor pulse counters (volatile for ISR safety)
volatile unsigned long pulseCountL = 0;
volatile unsigned long pulseCountR = 0;

// Interrupt Service Routines (ISRs) for high-speed pulse counting
void IRAM_ATTR countPulseL() { pulseCountL++; }
void IRAM_ATTR countPulseR() { pulseCountR++; }

void setup() {
  Serial.begin(115200);

  pinMode(REV_PIN, OUTPUT); digitalWrite(REV_PIN, LOW);
  pinMode(REV_PIN_LEFT, OUTPUT); pinMode(REV_PIN_RIGHT, OUTPUT);
  digitalWrite(REV_PIN_LEFT, LOW); digitalWrite(REV_PIN_RIGHT, LOW);

  dacWrite(DAC_PIN_LEFT, 0); dacWrite(DAC_PIN_RIGHT, 0);

  pinMode(PTO_PIN, OUTPUT); pinMode(LIGHTS_PIN, OUTPUT);
  pinMode(HYD_PIN, OUTPUT); pinMode(HORN_PIN, OUTPUT);
  pinMode(STATUS_LED, OUTPUT);

  // Initialize Brakes to RELEASED at startup
  pinMode(BRAKE_PIN_L, OUTPUT); pinMode(BRAKE_PIN_R, OUTPUT);
  digitalWrite(BRAKE_PIN_L, BRAKE_OFF);
  digitalWrite(BRAKE_PIN_R, BRAKE_OFF);

  // Setup Flux Sensors with Interrupts for precise RPM
  pinMode(SPEED_PIN_L, INPUT);
  pinMode(SPEED_PIN_R, INPUT);
  attachInterrupt(digitalPinToInterrupt(SPEED_PIN_L), countPulseL, RISING);
  attachInterrupt(digitalPinToInterrupt(SPEED_PIN_R), countPulseR, RISING);

  SerialBT.begin(BT_DEVICE_NAME);
  Serial.println("VCU Ready.");
}

void loop() {
  if (SerialBT.available()) pump(SerialBT, btBuf);
  applyDrive();
  digitalWrite(HORN_PIN, (millis() < hornUntil) ? HIGH : LOW);
  digitalWrite(STATUS_LED, (millis() < ledOffMs) ? HIGH : LOW);
  sendRpmTelemetry();
  delay(1);
}

void pump(Stream &s, String &buf) {
  while (s.available()) {
    char ch = s.read();
    if (ch == '\n') { handleCommand(buf); buf = ""; }
    else if (ch != '\r') { buf += ch; }
  }
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;
  Serial.print("CMD: "); Serial.println(cmd);
  ledOffMs = millis() + 50;

  static int s_prevSpeed = 0;
  static unsigned long s_speedUpdateTime = 0;

  if (cmd.startsWith("SPD")) {
    int newSpeed = constrain(cmd.substring(3).toInt(), -100, 100);
    // SPEED TRAP: Block increases during sync
    if (g_turnIntensity > 0 && abs(newSpeed) > abs(g_speed)) { }
    else {
        s_prevSpeed = g_speed;
        s_speedUpdateTime = millis();
        g_speed = newSpeed;
    }

  } else if (cmd == "HRN") {
    hornUntil = millis() + HORN_MS;
  } else {
    Dir oldDir = g_dir;
    char c = cmd[0];

    // TRANSITION SHIELD
    if ((c == 'L' || c == 'R') && (millis() - s_speedUpdateTime < 150)) {
        if (abs(g_speed) > abs(s_prevSpeed)) { g_speed = s_prevSpeed; }
    }

    switch (c) {
      case 'F': g_turnIntensity = 0; g_dir = FWD; g_brakes = false; break;
      case 'B': g_turnIntensity = 0; g_dir = REV; g_brakes = false; break;
      case 'L':
        if (oldDir == LEFT) g_turnIntensity = min(g_turnIntensity + 1, 4);
        else g_turnIntensity = 1;
        g_dir = LEFT;
        g_brakes = false; // Release mechanical brakes on turn intent
        break;
      case 'R':
        if (oldDir == RIGHT) g_turnIntensity = min(g_turnIntensity + 1, 4);
        else g_turnIntensity = 1;
        g_dir = RIGHT;
        g_brakes = false; // Release mechanical brakes on turn intent
        break;
      case 'S': g_turnIntensity = 0; g_dir = STOP; break; // STOP does not release ESTOP brakes
      case 'E': g_turnIntensity = 0; g_dir = ESTOP; g_brakes = true; break;
    }
  }
  lastCmdMs = millis();
}

void applyDrive() {
  static Dir lastDir = STOP;
  static int lastIntensity = 0;
  static bool lastRevL = false, lastRevR = false;

  int targetL = 0, targetR = 0;
  bool revL = false, revR = false;

  int effectiveSpeed = abs(g_speed);
  bool isIdleTurn = (effectiveSpeed == 0 && g_turnIntensity > 0);
  int idleMag = 0;

  // Handle Idle-to-Pivot Mapping
  if (isIdleTurn) {
      if (g_turnIntensity == 1) idleMag = 2;
      else if (g_turnIntensity == 2) idleMag = 4;
      else if (g_turnIntensity == 3) idleMag = 6;
      else idleMag = 8;
      effectiveSpeed = idleMag;
  }

  if (g_dir == STOP || g_dir == ESTOP) {
    targetL = targetR = 0;
  } else {
    bool movingRev = (g_speed < 0);
    int magL = effectiveSpeed, magR = effectiveSpeed;

    if (g_turnIntensity > 0) {
      if (g_dir == LEFT) {
        if (isIdleTurn) { magL = -idleMag; magR = idleMag; }
        else {
          if (g_turnIntensity == 1) magL = max(0, effectiveSpeed - 2);
          else if (g_turnIntensity == 2) magL = -6;
          else if (g_turnIntensity == 3) magL = -8;
          else magL = -10;
        }
      } else if (g_dir == RIGHT) {
        if (isIdleTurn) { magR = -idleMag; magL = idleMag; }
        else {
          if (g_turnIntensity == 1) magR = max(0, effectiveSpeed - 2);
          else if (g_turnIntensity == 2) magR = -6;
          else if (g_turnIntensity == 3) magR = -8;
          else magR = -10;
        }
      }
    }

    float calL = (magL < 0) ? DRIVE_CALIB_L_DOWN : DRIVE_CALIB_L_UP;
    revL = (magL < 0) ? !movingRev : movingRev;
    targetL = (magL == 0) ? 0 : constrain(map(abs(magL), 1, 100, MIN_PWM_L, MAX_PWM_L) * calL, 0, MAX_PWM_L);

    float calR = (magR < 0) ? DRIVE_CALIB_R_DOWN : DRIVE_CALIB_R_UP;
    revR = (magR < 0) ? !movingRev : movingRev;
    targetR = (magR == 0) ? 0 : constrain(map(abs(magR), 1, 100, MIN_PWM_R, MAX_PWM_R) * calR, 0, MAX_PWM_R);

    // SELECTIVE COLD START
    bool intensityChanged = (g_turnIntensity != lastIntensity);
    bool dirChanged = (g_dir != lastDir);

    if (dirChanged || intensityChanged) {
        if (revL != lastRevL || (g_dir == LEFT && intensityChanged) || (lastDir == LEFT && g_turnIntensity == 0)) {
            dacWrite(DAC_PIN_LEFT, 0); curDacL = 0;
        }
        if (revR != lastRevR || (g_dir == RIGHT && intensityChanged) || (lastDir == RIGHT && g_turnIntensity == 0)) {
            dacWrite(DAC_PIN_RIGHT, 0); curDacR = 0;
        }
        delay(15);
    }

    if (curDacL == 0 && targetL > 0) curDacL = 80;
    if (curDacR == 0 && targetR > 0) curDacR = 80;
  }

  // Speed Ramping
  if (g_dir == STOP || g_dir == ESTOP) {
    curDacL = curDacR = 0;
  } else {
    if (curDacL < targetL) curDacL = min(curDacL + SLEW_RATE_L, targetL);
    else if (curDacL > targetL) curDacL = max(curDacL - (SLEW_RATE_L * 2), targetL);

    if (curDacR < targetR) curDacR = min(curDacR + SLEW_RATE_R, targetR);
    else if (curDacR > targetR) curDacR = max(curDacR - (SLEW_RATE_R * 2), targetR);
  }

  // PHYSICAL OUTPUTS
  digitalWrite(REV_PIN_LEFT, revL ? HIGH : LOW);
  digitalWrite(REV_PIN_RIGHT, revR ? HIGH : LOW);
  digitalWrite(BRAKE_PIN_L, g_brakes ? BRAKE_ON : BRAKE_OFF);
  digitalWrite(BRAKE_PIN_R, g_brakes ? BRAKE_ON : BRAKE_OFF);

  dacWrite(DAC_PIN_LEFT, curDacL);
  delayMicroseconds(50);
  dacWrite(DAC_PIN_RIGHT, curDacR);

  lastDir = g_dir; lastIntensity = g_turnIntensity;
  lastRevL = revL; lastRevR = revR;
}

/**
 * Calculates real-world speed from Flux Sensor pulses and sends to app.
 */
void sendRpmTelemetry() {
  if (millis() - lastRpmMs > 300) {
    // 1. Snapshot the counts and reset them immediately to keep timing accurate
    noInterrupts();
    unsigned long pL = pulseCountL; pulseCountL = 0;
    unsigned long pR = pulseCountR; pulseCountR = 0;
    interrupts();

    // 2. Send raw pulse data (App can convert to RPM/KPH based on wheel size)
    SerialBT.print("RPM "); SerialBT.print(pL); SerialBT.print(","); SerialBT.println(pR);
    lastRpmMs = millis();
  }
}
