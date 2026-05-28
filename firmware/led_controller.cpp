#include "led_controller.h"

void LedController::begin() {
    pinMode(LED_PIN, OUTPUT);
    turnOff(); // Inicia desligado
}

void LedController::turnOn() {
    digitalWrite(LED_PIN, HIGH);
}

void LedController::turnOff() {
    digitalWrite(LED_PIN, LOW);
}

void LedController::toggle() {
    int currentState = digitalRead(LED_PIN);
    digitalWrite(LED_PIN, !currentState);
}
