package ch.devcon5.sonar.plugins.mutationanalysis.standalone;

import java.util.List;

/**
 * Simple helper to not rely on external dependencies to create Json Structures.
 */
public final class Json {
    private Json() {
    }

    public static String prop(String key, Number value) {
        return "\"" + key + "\":" + value;
    }
    public static String prop(String key, Boolean value) {
        return "\"" + key + "\":" + value;
    }
    public static String prop(String key, String value) {
        return "\"" + key + "\":\"" + value + "\"";
    }
    public static String propObj(String key, Object value) {
        return "\"" + key + "\":" + value;
    }

    public static String obj(String... properties) {
        return "{" + String.join(",", properties) + "}";
    }

    public static String append(String objOrArr, String... properties){
        return objOrArr.substring(0, objOrArr.length() - 1)
                + ","
                + String.join(",", properties)
                + objOrArr.charAt(objOrArr.length()-1);
    }

    public static String arr(String... elements) {
        return "[" + String.join(",", elements) + "]";
    }

    public static String arr(List<String> elements) {
        return arr(elements.toArray(new String[0]));
    }

}
