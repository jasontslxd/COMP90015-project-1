package util;

import com.google.gson.*;

import java.util.*;

public class JsonHandler {
    private static Gson gson = null;

    public JsonHandler() {
        if (gson == null) {
            gson = new Gson();
        }
    }

    public JsonObject stringToJson(String jsonString) {
        return JsonParser.parseString(jsonString).getAsJsonObject();
    }

    public String jsonToString(JsonObject jsonObject) {
        return jsonObject.toString();
    }

    public JsonElement getMessageValue(JsonObject jsonObject, String key) throws KeyNotFoundException {
        if (!jsonObject.has(key)) {
            throw new KeyNotFoundException("JSON object did not have the key: ".concat(key));
        }
        return jsonObject.get(key);
    }

    public JsonElement getMessageValue(String objectString, String key) throws KeyNotFoundException {
        return getMessageValue(stringToJson(objectString), key);
    }

    public String constructJsonMessage(Map<String, Object> map) {
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

    public HashMap<String, Object> deconstructJsonMessage(JsonObject jsonData) {
        HashMap<String, Object> newMessage = new HashMap<>();
        for (String key : jsonData.keySet()) {
            try {
                JsonElement value = jsonData.get(key);

                // protocol only allows string or array values
                if (value.isJsonPrimitive()) {
                    newMessage.put(key, jsonData.get(key).getAsString());
                }
                else {
                    newMessage.put(key, value.getAsJsonArray());
                }
            }
            catch (ClassCastException | IllegalStateException e) {
                // possibly log error?
            }
        }
        return newMessage;
    }

    public static void main(String[] args) throws KeyNotFoundException{
        JsonHandler handler = new JsonHandler();
        String fromJson = "{\"type\":\"newidentity\"}";
        JsonObject object = handler.stringToJson(fromJson);
        System.out.println(handler.getMessageValue(object,"type"));

        HashMap<String, Object> toJson = new HashMap<>();
        toJson.put("type", "roomlists");
        toJson.put("roomid", new String[]{"aaron", "adel", "chao", "guest1"});
        ArrayList<HashMap<String, Object>> arr= new ArrayList<>();
        HashMap<String, Object> one = new HashMap<>();
        one.put("roomid", "MainHall");
        one.put("count", 5);
        HashMap<String, Object> two = new HashMap<>();
        two.put("roomid", "comp90015");
        two.put("count", 7);
        HashMap<String, Object> three = new HashMap<>();
        three.put("roomid", "FridayNight");
        three.put("count", 4);
        arr.add(one);
        arr.add(two);
        arr.add(three);
        toJson.put("identities", arr);
        String out = handler.constructJsonMessage(toJson);
        System.out.println(out);
        System.out.println(handler.getMessageValue(out, "identities"));
        System.out.println(handler.getMessageValue(out, "roomid"));
        System.out.println(handler.deconstructJsonMessage(handler.stringToJson(out)));
    }
}
