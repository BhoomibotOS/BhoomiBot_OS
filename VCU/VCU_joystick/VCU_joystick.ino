/*
 * VCU_joystick.ino  —  BhoomiBot OS  VCU / ESP32 firmware
 * -----------------------------------------------------------------------------
 * Replaces the Dabble-only demo (VCU_till_fixed_and_var_code_with_bluetooth.ino) with a firmware
 * that speaks the Android app's raw serial protocol (app side: vcu/VcuProtocol.kt), so the
 * operator phone can drive the robot from the JOYSTICK (or DIGITAL buttons) in Manual mode.
 *
 * The app (Settings -> Connection Mode) chooses the link: Bluetooth or WiFi Hotspot. This build
 * implements BLUETOOTH ONLY — the full WiFi stack pushed the sketch past the ESP32's flash limit
 * (123% of program storage in the combined build), so WiFi is commented out for later. The firmware
 * still parses the same protocol; just pair the phone over Bluetooth (classic SPP, opened by MAC).
 *
 * Motor wiring (unchanged from the previous firmware):
 *   DAC_PIN_RIGHT 25  -> right motor speed (0..255 analog / DAC output)
 *   DAC_PIN_LEFT  26  -> left  motor speed (0..255 analog / DAC output)
 *   REV_PIN      27   -> direction: LOW = forward, HIGH = reverse (shared by BOTH motors)
 *
 * Because both motors share one direction pin, steering is ARC steering: to turn, the inner wheel
 * is slowed by TURN_FACTOR. For a true pivot turn (one wheel forward, other reversed) you would
 * need a per-motor direction pin instead of the single REV_PIN.
 *
 * Serial protocol — one token per line, '\n'-terminated, sent by the app:
 *   F          forward
 *   B          reverse
 *   L          turn left  (arc)
 *   R          turn right (arc)
 *   S          stop
 *   E          emergency stop
 *   SPD<-100..100> set SIGNED speed: + = forward, - = reverse (magnitude 0..100)
 *   PTO1/PTO0  power-take-off on/off   (optional output, see PTO_PIN)
 *   LGT1/LGT0  work lights on/off      (optional output, see LIGHTS_PIN)
 *   HYD<0..100> hydraulic lift height as PWM duty % (0 = retracted/off; see HYD_PIN)
 *   HRN        horn — pulse once (one-shot, like GamePad.isSelectPressed(); see HORN_PIN)
 *   T          link-test probe from the app (ignored)
 */

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! In Arduino IDE: Tools -> Core Debug Level, or enable BT in sdkconfig.
#endif

// ----------------------------- Configuration -----------------------------
// Wi-Fi is DISABLED for now — the WiFi stack alone blew the flash budget (sketch was 123% of
// program storage). Kept here, commented, for when you move to a larger-part ESP32 or trim the
// build. The app's Settings -> Connection Mode "WiFi Hotspot" option simply won't connect until
// this is re-enabled and flashed (and #include <WiFi.h> is restored).
// #define AP_SSID      "ESP32_Robot"
// #define AP_PASSWORD  "bhoomi123"   // WPA2 requires >= 8 characters
// #define TCP_PORT     8888           // must match the app's WiFi Port (default 8888)

// Bluetooth (used when app Connection Mode = Bluetooth). Pair this device, then enter its MAC
// address in the app's Bluetooth MAC field (Settings -> Connection).
#define BT_DEVICE_NAME "ESP32_Robot"

// Motor / direction outputs.
#define DAC_PIN_RIGHT 25
#define DAC_PIN_LEFT  26
#define REV_PIN       27

// Optional auxiliary outputs. PTO -> GPIO 4, Lights -> GPIO 16, Hydraulic -> GPIO 14,
// Horn -> GPIO 17 (safe digital/PWM outputs on ESP32). Drive a relay / MOSFET from here;
// set to -1 to disable.
#define PTO_PIN    4    // PTO1/PTO0 toggles this pin (HIGH/LOW)
#define LIGHTS_PIN 16   // LGT1/LGT0 toggles this pin (HIGH/LOW)
#define HYD_PIN    14   // HYD<0..100> drives this pin with analogWrite (PWM duty = height%)
#define HORN_PIN   17   // HRN pulses this pin HIGH for HORN_MS

// Tuning.
// MIN_PWM floor matches the proven Dabble reference (map(y,1,7,30,255)): the motors reliably
// start turning at ~30/255, so the floor is 30 rather than 0.
#define MIN_PWM      30    // min analog value so the motor actually turns when SPD > 0
#define MAX_PWM      255
#define TURN_FACTOR  0.5f  // inner-wheel fraction removed while turning (0 = straight, 1 = pivot).
                           // 0.5 slows the inner wheel to half speed for a clear arc; tune to taste.
#define HORN_MS      300   // horn honk length per HRN press (one-shot pulse)
#define WATCHDOG_MS  0     // 0 = disabled. If > 0, stops the motors after this many ms with no
                           // command. Leave 0 for DIGITAL (tap-to-latch) mode, which holds speed
                           // between taps; enable only for a dead-man on the joystick stream.

// ----------------------------- Globals -----------------------------
BluetoothSerial SerialBT;

// Drive state is LATCHED (it survives between commands), matching the app's digital mode where a
// single tap raises the speed and it stays until STOP / E-STOP.
enum Dir { STOP, FWD, REV, LEFT, RIGHT, ESTOP };
Dir  g_dir   = STOP;
int  g_speed = 0;     // SIGNED percent: -100 (full reverse) .. +100 (full forward), 0 = stopped
bool g_pto   = false;
bool g_lights = false;
int  g_hyd    = 0;     // hydraulic duty 0..100 (%)
unsigned long hornUntil = 0;   // non-blocking horn timer

String btBuf;          // receive buffer for Bluetooth
unsigned long lastCmdMs = 0;

// ----------------------------- Setup -----------------------------
void setup() {
  Serial.begin(115200);

  pinMode(REV_PIN, OUTPUT);
  digitalWrite(REV_PIN, LOW);
  dacWrite(DAC_PIN_LEFT, 0);
  dacWrite(DAC_PIN_RIGHT, 0);
  if (PTO_PIN    >= 0) pinMode(PTO_PIN, OUTPUT);
  if (LIGHTS_PIN >= 0) pinMode(LIGHTS_PIN, OUTPUT);
  if (HYD_PIN    >= 0) pinMode(HYD_PIN, OUTPUT);
  if (HORN_PIN   >= 0) { pinMode(HORN_PIN, OUTPUT); digitalWrite(HORN_PIN, LOW); }

  // --- Bluetooth SPP (the only transport in this build) ---
  SerialBT.begin(BT_DEVICE_NAME);
  Serial.println("Bluetooth ready — pair and connect as '" BT_DEVICE_NAME "'");
  Serial.println("VCU joystick firmware ready (Bluetooth only; Wi-Fi disabled to save flash).");
}

// ----------------------------- Loop -----------------------------
void loop() {
  // Bluetooth: read any pending bytes.
  if (SerialBT.available()) pump(SerialBT, btBuf);

  // Optional dead-man watchdog.
  if (WATCHDOG_MS > 0 && g_dir != STOP && g_dir != ESTOP &&
      (millis() - lastCmdMs) > (unsigned long)WATCHDOG_MS) {
    g_dir = STOP;
    applyDrive();
    Serial.println("WATCHDOG: timeout, stopped");
  }

  // Horn: pulse for HORN_MS, then release (non-blocking, like GamePad.isSelectPressed()).
  digitalWrite(HORN_PIN, (millis() < hornUntil) ? HIGH : LOW);

  delay(10);
}

// Read available bytes into buf; on each newline, handle the completed command line.
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

// Parse one command line from the app and update the latched drive state.
void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;

  // Multi-character commands are handled FIRST: their leading letter collides with a single-char
  // command ("SPD" shares 'S' with STOP, "LGT" shares 'L' with LEFT). If the single-char switch ran
  // first it would swallow them as STOP/LEFT via the matching case + break, and the speed/light
  // value would never be applied (this was the bug that kept PIN25/PIN26 pinned at 0).
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
    hornUntil = millis() + HORN_MS;        // one-shot, like isSelectPressed()
  } else {
    // Single-character drive commands.
    switch (cmd[0]) {
      case 'F': g_dir = FWD;   break;
      case 'B': g_dir = REV;   break;
      case 'L': g_dir = LEFT;  break;
      case 'R': g_dir = RIGHT; break;
      case 'S': g_dir = STOP;  break;
      case 'E': g_dir = ESTOP; break;
      case 'T': /* link-test probe from the app — ignore */ break;
      default:
        Serial.print("UNK: "); Serial.println(cmd);
        break;
    }
  }

  lastCmdMs = millis();
  applyDrive();

  // Debug echo — prints every parsed command so you can confirm the app's F/SPD tokens
  // are arriving and how they map to direction + speed. Open the Serial Monitor at 115200.
  // Comment these out again once you're happy, to keep the monitor quiet.
  Serial.print("CMD "); Serial.print(cmd);
  Serial.print(" dir="); Serial.print((int)g_dir);
  Serial.print(" spd=");  Serial.print(g_speed);
  Serial.print(" hyd=");  Serial.println(g_hyd);
}

// Translate (steering direction, signed speed) into motor outputs.
// g_speed is SIGNED: >0 forward, <0 reverse, 0 stopped. REV_PIN (27) follows the sign.
// Steering (LEFT/RIGHT) slows the INNER wheel by TURN_FACTOR so the robot arcs while keeping speed.
void applyDrive() {
  int  left = 0, right = 0;
  bool reverse = false;

  if (g_dir == STOP || g_dir == ESTOP || g_speed == 0) {
    left = right = 0;
    reverse = false;
  } else {
    int mag = abs(g_speed);
    int pwm = constrain(map(mag, 1, 100, MIN_PWM, MAX_PWM), 0, MAX_PWM);
    reverse = (g_speed < 0);
    // Both motors driven at the commanded magnitude.
    left = right = pwm;
    // Steering: cut the inner wheel. LEFT turn slows the LEFT motor (pin 26);
    // RIGHT turn slows the RIGHT motor (pin 25). TURN_FACTOR is the fraction removed
    // (0 = straight, 1 = inner wheel fully stopped / pivot turn).
    if (g_dir == LEFT)  left  = (int)(pwm * (1.0f - TURN_FACTOR));
    if (g_dir == RIGHT) right = (int)(pwm * (1.0f - TURN_FACTOR));
  }

  digitalWrite(REV_PIN, reverse ? HIGH : LOW);
  dacWrite(DAC_PIN_LEFT,  left);
  dacWrite(DAC_PIN_RIGHT, right);
}

/*
 * Wi-Fi (future) — to re-enable, restore `#include <WiFi.h>`, uncomment the AP_SSID/AP_PASSWORD/
 * TCP_PORT #defines, add back `WiFiServer server(TCP_PORT); WiFiClient client;` globals, and the
 * softAP/begin lines in setup() + the accept/read block in loop() that were removed to save flash.
 *
 * To JOIN the phone's own hotspot instead of creating one:
 *   WiFi.mode(WIFI_STA);
 *   WiFi.begin("PHONE_HOTSPOT_SSID", "PHONE_HOTSPOT_PASSWORD");
 *   while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print('.'); }
 *   server.begin();
 *   Serial.println(WiFi.localIP());   // enter THIS IP in the app's WiFi Host field
 */
