package com.r.bazaar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public class Backup {

    @JsonProperty("date")
    private LocalDateTime date;

    public Backup(LocalDateTime date) {
        this.date = date;
    }

    public Backup() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Backup backup = (Backup) o;
        return Objects.equals(date, backup.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }
}