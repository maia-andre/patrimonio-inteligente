#ifndef LED_CONTROLLER_H
#define LED_CONTROLLER_H

#include <Arduino.h>

// Pino do LED onboard do ESP32 DevKit V1 geralmente é o 2
#define LED_PIN 2

class LedController {
public:
    // Inicializa o pino do LED
    void begin();
    
    // Liga o LED
    void turnOn();
    
    // Desliga o LED
    void turnOff();
    
    // Inverte o estado atual do LED (útil para piscar)
    void toggle();
};

#endif // LED_CONTROLLER_H
