package com.medibed.tracker.model;

import com.fasterxml.jackson.annotation.JsonCreator;

/** Constant names match the frontend ("Government"/"Private") and the MySQL ENUM exactly. */
public enum HospitalType {
    Government, Private;

    @JsonCreator
    public static HospitalType from(String value) {
        if (value == null) {
            return null;
        }
        for (HospitalType t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("type must be Government or Private");
    }
}
