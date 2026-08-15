package no.beint.brev;

/** A UN/ECE Recommendation 20 unit code. */
public record UnitCode(String value) {
    public static final UnitCode EACH = new UnitCode("C62");
    public static final UnitCode EA = new UnitCode("EA");
    public static final UnitCode HOUR = new UnitCode("HUR");

    public UnitCode {
        Values.upperAsciiCode(value, "unit code", 1, 3);
    }

    @Override
    public String toString() {
        return value;
    }
}
