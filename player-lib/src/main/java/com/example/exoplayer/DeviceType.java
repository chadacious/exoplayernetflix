package com.example.exoplayer;

public enum DeviceType {
    PHONE("P-");

    private final String identityValue;

    private DeviceType(String str) {
        this.identityValue = str;
    }

    public static DeviceType determine() {
        return PHONE;
    }

    public String getIdentityValue() {
        return this.identityValue;
    }
}
