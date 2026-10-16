package com.example.procurement.domain;

/** Business urgency classification for purchase orders, feeding DMN approval routing. */
public enum Urgency {
    NORMAL,
    HIGH,
    URGENT
}
