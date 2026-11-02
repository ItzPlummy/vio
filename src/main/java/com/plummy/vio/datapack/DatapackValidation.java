package com.plummy.vio.datapack;

import java.util.List;

public record DatapackValidation(List<String> errors, List<String> warnings) {

    public static DatapackValidation error(String error) {
        return new DatapackValidation(List.of(error), List.of());
    }

    public static DatapackValidation warnings(List<String> warnings) {
        return new DatapackValidation(List.of(), List.copyOf(warnings));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
