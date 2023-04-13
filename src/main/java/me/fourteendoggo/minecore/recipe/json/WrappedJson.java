package me.fourteendoggo.minecore.recipe.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.Collections;
import java.util.Map;

public class WrappedJson {
    private final JsonObject json;

    private WrappedJson(JsonObject json) {
        this.json = json;
    }

    public static WrappedJson of(JsonElement element) {
        return of(element.getAsJsonObject());
    }

    public static WrappedJson of(JsonObject obj) {
        return new WrappedJson(obj);
    }

    public static WrappedJson of(Reader reader) {
        JsonElement element = JsonParser.parseReader(reader);
        return of(element);
    }

    public WrappedJson get(String memberName) {
        JsonElement element = json.get(memberName);
        return of(element);
    }

    public JsonElement getRaw(String memberName) {
        return json.get(memberName);
    }

    public Map<String, JsonElement> getMap(String memberName) {
        JsonElement map = json.get(memberName);
        if (map == null) return Collections.emptyMap();
        return map.getAsJsonObject().asMap();
    }

    public CompoundArray getCompoundArray(String memberName) {
        JsonElement element = json.get(memberName);
        return new CompoundArray(element.getAsJsonArray().asList());
    }

    public JsonArray getScalarArray(String memberName) {
        JsonElement element = json.get(memberName);
        return element != null ? element.getAsJsonArray() : new JsonArray(0);
    }

    public String getString(String memberName) {
        return getString(memberName, null);
    }

    public String getString(String memberName, String defaultValue) {
        JsonElement element = json.get(memberName);
        return element != null ? element.getAsString() : defaultValue;
    }

    public int getInt(String memberName) {
        return getInt(memberName, 0);
    }

    public int getInt(String memberName, int defaultValue) {
        JsonElement element = json.get(memberName);
        return element != null ? element.getAsInt() : defaultValue;
    }

    public float getFloat(String memberName) {
        return getFloat(memberName, 0);
    }

    public float getFloat(String memberName, float defaultValue) {
        JsonElement element = json.get(memberName);
        return element != null ? element.getAsFloat() : defaultValue;
    }
}
