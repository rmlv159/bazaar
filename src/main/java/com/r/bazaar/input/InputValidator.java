package com.r.bazaar.input;

import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    public void runForSequencedTypes(ArrayList<String> sequencedTypes) throws IllegalArgumentException {
        for (String t : sequencedTypes) {
            if (!Decoder.SQL_TYPE_MAP.containsKey(t.toUpperCase())) {
                throw new IllegalArgumentException("SQL data type is not mapped");
            }
        }
    }
}
