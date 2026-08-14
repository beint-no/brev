package no.beint.brev.billing.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/** Small buffered UTF-8 encoder specialized for XML 1.0 output. */
public final class Utf8XmlOutput {
    private static final int BUFFER_SIZE = 8192;

    private final OutputStream destination;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int position;

    public Utf8XmlOutput(OutputStream destination) {
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    public void declaration() throws IOException {
        raw("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    }

    public void start(String name) throws IOException {
        raw("<");
        raw(name);
        raw(">");
    }

    public void start(String name, String attributeName, String attributeValue) throws IOException {
        raw("<");
        raw(name);
        raw(" ");
        raw(attributeName);
        raw("=\"");
        escaped(attributeValue, true);
        raw("\">");
    }

    public void end(String name) throws IOException {
        raw("</");
        raw(name);
        raw(">");
    }

    public void element(String name, String value) throws IOException {
        start(name);
        escaped(value, false);
        end(name);
    }

    public void element(String name, String attributeName, String attributeValue, String value) throws IOException {
        start(name, attributeName, attributeValue);
        escaped(value, false);
        end(name);
    }

    public void raw(String ascii) throws IOException {
        Objects.requireNonNull(ascii, "ascii");
        for (int index = 0; index < ascii.length(); index++) {
            char character = ascii.charAt(index);
            if (character > 0x7f) {
                throw new IllegalArgumentException("raw XML must be ASCII");
            }
            writeByte(character);
        }
    }

    public void finish() throws IOException {
        drain();
    }

    private void escaped(String value, boolean attribute) throws IOException {
        Objects.requireNonNull(value, "XML value");
        for (int index = 0; index < value.length();) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> raw("&amp;");
                case '<' -> raw("&lt;");
                case '>' -> raw("&gt;");
                case '"' -> {
                    if (attribute) {
                        raw("&quot;");
                    } else {
                        writeByte(character);
                    }
                }
                default -> {
                    int codePoint;
                    if (Character.isHighSurrogate(character)) {
                        if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                            throw new IllegalArgumentException("unpaired high surrogate in XML value");
                        }
                        codePoint = Character.toCodePoint(character, value.charAt(index + 1));
                        index++;
                    } else if (Character.isLowSurrogate(character)) {
                        throw new IllegalArgumentException("unpaired low surrogate in XML value");
                    } else {
                        codePoint = character;
                    }
                    requireXmlCharacter(codePoint);
                    writeCodePoint(codePoint);
                }
            }
            index++;
        }
    }

    private static void requireXmlCharacter(int codePoint) {
        boolean allowedControl = codePoint == 0x9 || codePoint == 0xa || codePoint == 0xd;
        boolean basic = codePoint >= 0x20 && codePoint <= 0xd7ff;
        boolean upperBasic = codePoint >= 0xe000 && codePoint <= 0xfffd;
        boolean supplementary = codePoint >= 0x10000 && codePoint <= 0x10ffff;
        if (!allowedControl && !basic && !upperBasic && !supplementary) {
            throw new IllegalArgumentException("character U+" + Integer.toHexString(codePoint) + " is not legal in XML 1.0");
        }
    }

    private void writeCodePoint(int codePoint) throws IOException {
        if (codePoint <= 0x7f) {
            writeByte(codePoint);
        } else if (codePoint <= 0x7ff) {
            writeByte(0xc0 | codePoint >> 6);
            writeByte(0x80 | codePoint & 0x3f);
        } else if (codePoint <= 0xffff) {
            writeByte(0xe0 | codePoint >> 12);
            writeByte(0x80 | codePoint >> 6 & 0x3f);
            writeByte(0x80 | codePoint & 0x3f);
        } else {
            writeByte(0xf0 | codePoint >> 18);
            writeByte(0x80 | codePoint >> 12 & 0x3f);
            writeByte(0x80 | codePoint >> 6 & 0x3f);
            writeByte(0x80 | codePoint & 0x3f);
        }
    }

    private void writeByte(int value) throws IOException {
        if (position == buffer.length) {
            drain();
        }
        buffer[position++] = (byte) value;
    }

    private void drain() throws IOException {
        if (position != 0) {
            destination.write(buffer, 0, position);
            position = 0;
        }
    }
}
