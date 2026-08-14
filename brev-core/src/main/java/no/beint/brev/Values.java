package no.beint.brev;

import java.util.Objects;

final class Values {
    private Values() {
    }

    static String nonBlank(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (!value.equals(value.strip())) {
            throw new IllegalArgumentException(name + " must not have surrounding whitespace");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(name + " exceeds " + maximumLength + " characters");
        }
        return value;
    }

    static String upperAsciiCode(String value, String name, int minimumLength, int maximumLength) {
        nonBlank(value, name, maximumLength);
        if (value.length() < minimumLength) {
            throw new IllegalArgumentException(name + " is shorter than " + minimumLength + " characters");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean letter = character >= 'A' && character <= 'Z';
            boolean digit = character >= '0' && character <= '9';
            if (!letter && !digit) {
                throw new IllegalArgumentException(name + " must contain only uppercase ASCII letters and digits");
            }
        }
        return value;
    }
}
