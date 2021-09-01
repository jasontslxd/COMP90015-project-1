package util;

import java.nio.charset.StandardCharsets;

public class UTFEncoder {

    private UTFEncoder() {};

    public static String utfEncoder(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        String output = new String(bytes, StandardCharsets.UTF_8);

        return output;
    }
}
