package az.bakuelectronics.automation.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String[] readStringArray(String resourcePath, String propertyName) {
        String json = readResource(resourcePath);
        String propertyPattern = "\\\"" + Pattern.quote(propertyName) + "\\\"\\s*:\\s*\\[";
        Matcher propertyMatcher = Pattern.compile(propertyPattern).matcher(json);
        if (!propertyMatcher.find()) {
            throw new IllegalArgumentException("JSON array property not found: " + propertyName);
        }

        int arrayEnd = findArrayEnd(json, propertyMatcher.end());
        String arrayContent = json.substring(propertyMatcher.end(), arrayEnd).trim();
        if (arrayContent.isEmpty()) {
            return new String[0];
        }

        List<String> values = new ArrayList<>();
        int index = 0;
        while (index < arrayContent.length()) {
            if (arrayContent.charAt(index) != '\"') {
                throw new IllegalArgumentException("Expected a JSON string in property: " + propertyName);
            }
            ParseResult parsed = parseString(arrayContent, index + 1);
            values.add(parsed.value());
            index = parsed.nextIndex();

            while (index < arrayContent.length() && Character.isWhitespace(arrayContent.charAt(index))) {
                index++;
            }
            if (index == arrayContent.length()) {
                break;
            }
            if (arrayContent.charAt(index) != ',') {
                throw new IllegalArgumentException("Expected ',' between JSON array values");
            }
            index++;
            while (index < arrayContent.length() && Character.isWhitespace(arrayContent.charAt(index))) {
                index++;
            }
        }
        return values.toArray(String[]::new);
    }

    private static String readResource(String resourcePath) {
        try (InputStream input = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Test data resource not found: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read test data resource: " + resourcePath, exception);
        }
    }

    private static int findArrayEnd(String json, int start) {
        boolean escaped = false;
        boolean inString = false;
        for (int index = start; index < json.length(); index++) {
            char character = json.charAt(index);
            if (character == '\"' && !escaped) {
                inString = !inString;
            }
            if (character == ']' && !inString) {
                return index;
            }
            escaped = character == '\\' && !escaped;
            if (character != '\\') {
                escaped = false;
            }
        }
        throw new IllegalArgumentException("Unterminated JSON array");
    }

    private static ParseResult parseString(String text, int start) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\"' && !escaped) {
                return new ParseResult(value.toString(), index + 1);
            }
            if (escaped) {
                if (character == '"' || character == '\\' || character == '/') {
                    value.append(character);
                } else if (character == 'b') {
                    value.append('\b');
                } else if (character == 'f') {
                    value.append('\f');
                } else if (character == 'n') {
                    value.append('\n');
                } else if (character == 'r') {
                    value.append('\r');
                } else if (character == 't') {
                    value.append('\t');
                } else {
                    throw new IllegalArgumentException("Unsupported JSON escape sequence");
                }
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                value.append(character);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private record ParseResult(String value, int nextIndex) {
    }
}
