package no.beint.brev;

/** A Peppol process/profile identifier. */
public record ProcessId(String value) {
    public ProcessId {
        Values.nonBlank(value, "process ID", 500);
    }

    @Override
    public String toString() {
        return value;
    }
}
