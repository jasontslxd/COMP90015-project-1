package util;

import com.google.gson.*;

import java.util.*;

public class JsonHandler {
    private static final Gson gson = new Gson();

    private JsonHandler() { }

    public static JsonObject stringToJson(String jsonString) {
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    public static String jsonToString(JsonObject jsonObject) {
        return jsonObject.toString() + "\n";
    }

    public static JsonElement getMessageValue(JsonObject jsonObject, String key) throws KeyNotFoundException {
        if (!jsonObject.has(key)) {
            throw new KeyNotFoundException("JSON object did not have the key: ".concat(key));
        }
        return jsonObject.get(key);
    }

    public static JsonElement getMessageValue(String objectString, String key) throws KeyNotFoundException {
        return getMessageValue(stringToJson(objectString), key);
    }

    public static String constructJsonMessage(Map<String, Object> map) {
        JsonObject newMessage = new JsonObject();
        for (String key : map.keySet()) {
            Object value = map.get(key);

            // protocol only allows string or array values
            if (value instanceof String) {
                newMessage.addProperty(key, (String) value);
            } else {
                JsonElement element = gson.toJsonTree(value);
                newMessage.add(key, element.getAsJsonArray());
            }
        }
        return jsonToString(newMessage);
    }
}
