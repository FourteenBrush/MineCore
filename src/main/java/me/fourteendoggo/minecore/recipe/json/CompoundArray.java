package me.fourteendoggo.minecore.recipe.json;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompoundArray implements Iterable<WrappedJson> {
    private final List<WrappedJson> elements;

    public CompoundArray(List<JsonElement> elements) {
        this.elements = new ArrayList<>(elements.size());
        for (JsonElement element : elements) {
            WrappedJson wrapped = WrappedJson.of(element);
            this.elements.add(wrapped);
        }
    }

    @NotNull
    @Override
    public Iterator<WrappedJson> iterator() {
        return elements.iterator();
    }
}
