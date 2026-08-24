# Implementation Plan - BhoomiBot VCU Optimization

Optimize the VCU firmware for E-Rickshaw motor controllers using a relay-based drive system, multi-stage braking with dedicated pins, and prepared GPIOs for TFT integration.

## GPIO Mapping Table

| GPIO | Category | Function | Signal Type | Logic / Detail |
| :--- | :--- | :--- | :--- | :--- |
| **25** | **Throttle** | Right Motor DAC | Analog Out | 0.8V–3.3V |
| **26** | **Throttle** | Left Motor DAC | Analog Out | 0.8V–3.3V |
| **18** | **Direction** | Right Reverse Relay | Digital Out | HIGH = Reverse |
| **19** | **Direction** | Left Reverse Relay | Digital Out | HIGH = Reverse |
| **2** | **Braking** | **Left Low Brake** Relay | Digital Out | Stage 1 (Taps 1-2) |
| **5** | **Braking** | **Right Low Brake** Relay| Digital Out | Stage 1 (Taps 1-2) |
| **12** | **Braking** | **Left High Brake** Relay | Digital Out | Stage 2 (Taps 3+) |
| **13** | **Braking** | **Right High Brake** Relay| Digital Out | Stage 2 (Taps 3+) |
| **32** | **Sensors** | Right Motor Feedback | Analog In | Controller RPM |
| **33** | **Sensors** | Left Motor Feedback | Analog In | Controller RPM |
| **35** | **Sensors** | Right Flux Sensor | Digital In | High Speed RPM (4 Magnets) |
| **36** | **Sensors** | Left Flux Sensor | Digital In | High Speed RPM (4 Magnets) |
| **34** | **Sensors** | Battery Voltage | Analog In | Voltage Divider |
| **17** | **Auxiliary** | Horn | Digital Out | Active HIGH |
| **16** | **Auxiliary** | Lights | Digital Out | Active HIGH |
| **4** | **Auxiliary** | PTO | Digital Out | High Power Relay |
| **14** | **Auxiliary** | Hydraulics (HYD) | PWM Out | Speed Control |
| **22** | **TFT Display**| SPI Clock (SCK) | SPI | Clock |
| **23** | **TFT Display**| SPI Data (MOSI) | SPI | Data |
| **15** | **TFT Display**| Chip Select (CS) | Digital Out | Selection |
| **21** | **TFT Display**| Data / Command (DC) | Digital Out | Mode |
| **27** | **TFT Display**| Reset (RST) | Digital Out | Hardware Reset |

## Proposed Code Changes

### VCU_joystick.ino
- Update Pin Definitions to match the table above.
- Implement **Slew Rate Ramping** for DAC outputs to prevent 1V sudden-jump errors.
- Implement **Pivot Turning** using independent reverse relays (Pins 18, 19).
- Implement **Advanced Multi-Stage Braking**:
    - **1-2 Taps on 'E' (Low Brake):** Activates **GPIO 2 & 5**. Throttles (25 & 26) go to 0 immediately.
    - **3+ Taps on 'E' (High Brake):** Activates **GPIO 12 & 13**. All other drive/brake signals are cut for maximum safety.
- Update Telemetry to send raw averaged values for RPM and Battery.
- Implement **Communication Watchdog (Safety Stop)**:
    - Set `WATCHDOG_MS` to **500ms**.
    - **Current Mode:** Physical link check ONLY (`hasClient()`).
    - **Future Mode (Commented):** Data timeout including Heartbeats (`H`).
    - Automatically engages **Low Brake Relays (2, 5)** and cuts DAC power to 0 if link is lost.
- Implement **Dual RPM Measurement**:
    - Analog feedback from controllers on **Pins 32, 33**.
    - Digital interrupt counting for wheel magnets on **Pins 35, 36**.
- Ensure all relays are initialized to **LOW** in `setup()`.

## Verification Plan

### Manual Verification
- **Serial Monitor:** Verify command parsing and DAC target values.
- **Relay Test:** Listen for relay clicks on pins 18, 19, 2, 5, 12, 13 during specific joystick/command inputs.
- **Braking Verification:** 
    - Press 'E' once/twice: Check Pins 2 & 5 voltage.
    - Press 'E' three+ times: Check Pins 12 & 13 voltage.
- **Voltage Ramp Test:** Measure DAC output on 25/26 to ensure soft-start behavior.
- **RPM Test:** Compare MOT feedback vs FLX calculation on the TFT dashboard.
- **BT Telemetry:** Verify `TEL` string format in a Bluetooth terminal.
