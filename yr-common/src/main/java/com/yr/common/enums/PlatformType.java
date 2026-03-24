package com.yr.common.enums;

public enum PlatformType {
    MGMT("mgmt"),
    DESKTOP("desktop");

    private String name;

    PlatformType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
