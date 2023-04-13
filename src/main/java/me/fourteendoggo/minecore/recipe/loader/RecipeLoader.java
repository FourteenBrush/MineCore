package me.fourteendoggo.minecore.recipe.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import me.fourteendoggo.minecore.recipe.impl.*;
import me.fourteendoggo.minecore.recipe.json.CompoundArray;
import me.fourteendoggo.minecore.recipe.json.WrappedJson;
import me.fourteendoggo.minecore.util.Utility;
import net.minestom.server.MinecraftServer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.*;
import org.jetbrains.annotations.Blocking;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO: free this object after all recipes are loaded
@SuppressWarnings("ConstantConditions")
public class RecipeLoader {
    private static final DeclareRecipesPacket.Ingredient EMPTY_INGREDIENT = new DeclareRecipesPacket.Ingredient(Collections.emptyList());

    // all strings for those collections are without the #minecraft: prefix
    private final Map<String, List<ItemStack>> tags = new HashMap<>();
    private final Set<String> transitiveLoadedTags = new HashSet<>();

    /**
     * Loads all recipes from the recipes folder and registers them with the server
     * @throws IOException if an IO exception occurs
     */
    @Blocking
    public Map<String, Recipe> loadAll() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        Map<String, Recipe> recipes = new HashMap<>();
        loadTags();

        File[] recipeFiles = new File("recipes").listFiles();
        for (File file : recipeFiles) {
            executor.execute(() -> {
                try (Reader reader = Files.newBufferedReader(file.toPath())) {
                    Recipe recipe = loadRecipe(reader, file.getName());
                    if (recipe == null) return; // TODO implement those special types

                    recipes.put(recipe.getRecipeId(), recipe);
                    MinecraftServer.getRecipeManager().addRecipe(recipe);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // free those
        tags.clear();
        transitiveLoadedTags.clear();
        return recipes;
    }

    /**
     * Loads all tag files (files which represent a bunch of item types) from the 'items' folder, to later use them in recipes
     * @throws IOException if an IO exception occurs
     */
    private void loadTags() throws IOException {
        DirectoryStream<Path> dir = Files.newDirectoryStream(Path.of("items"), entry -> {
            String tagName = entry.getFileName().toString().replace(".json", "");
            return !transitiveLoadedTags.contains(tagName);
        });
        Utility.loopResource(dir, this::loadTag);
    }

    /**
     * Reads a tag file from the items folder and returns a list of all the items in the tag. <br>
     * This also recursively loads all tags that are referenced by this tag.
     * @param path the path to the tag file
     * @return a list of all the items in the tag
     * @throws IOException if an IO exception occurs
     */
    private List<ItemStack> loadTag(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            WrappedJson fullJson = WrappedJson.of(reader);
            JsonArray materialOrTagEntries = fullJson.getScalarArray("values");
            List<ItemStack> tagContents = new ArrayList<>();

            for (JsonElement element : materialOrTagEntries) {
                String namespaceId = element.getAsString();
                if (!namespaceId.startsWith("#")) { // normal material entry (minecraft:x)
                    ItemStack stack = ItemStack.of(Material.fromNamespaceId(namespaceId));
                    tagContents.add(stack);
                    continue;
                }
                // reference to other tag (#minecraft:logs), load 'em if absent
                String otherTagName = namespaceId.replace("#minecraft:", "");
                List<ItemStack> otherTag = tags.get(otherTagName);
                if (otherTag == null) {
                    otherTag = loadTag(Path.of("items", otherTagName + ".json"));
                    tagContents.addAll(otherTag);
                    transitiveLoadedTags.add(otherTagName);
                }
            }
            String tagName = path.getFileName().toString().replace(".json", "");
            tags.put(tagName, tagContents);
            return tagContents;
        }
    }

    /**
     * Loads a recipe from the specified reader, figuring out what type of recipe it is and loading it accordingly
     * @param reader the reader to read the recipe from
     * @param fileName the name of the file the recipe is in
     * @return the loaded recipe
     */
    private Recipe loadRecipe(Reader reader, String fileName) {
        WrappedJson fullJson = WrappedJson.of(reader);

        String type = fullJson.getString("type");
        String recipeId = fileName.replace(".json", "");
        String group = fullJson.getString("group", "");

        return switch (type) {
            case "minecraft:crafting_shaped" -> loadShapedRecipe(fullJson, recipeId, group);
            case "minecraft:crafting_shapeless" -> loadShapelessRecipe(fullJson, recipeId, group);
            case "minecraft:smelting" -> loadCommonBaseRecipe(fullJson, recipeId, group, SmeltingRecipeImpl::new);
            case "minecraft:blasting" -> loadCommonBaseRecipe(fullJson, recipeId, group, BlastingRecipeImpl::new);
            case "minecraft:smoking" -> loadCommonBaseRecipe(fullJson, recipeId, group, SmokingRecipeImpl::new);
            case "minecraft:campfire_cooking" -> loadCommonBaseRecipe(fullJson, recipeId, group, CampfireCookingRecipeImpl::new);
            case "minecraft:stonecutting" -> loadStonecutterRecipe(fullJson, recipeId, group);
            case "minecraft:smithing" -> loadSmithingRecipe(fullJson, recipeId);
            default -> {
                if (type.contains("special")) {
                    yield null;
                }
                throw new IllegalStateException();
            }
        };
    }

    // SHAPED RECIPE

    private ShapedRecipe loadShapedRecipe(WrappedJson fullJson, String recipeId, String group) {
        Pattern pattern = readPattern(fullJson);
        ItemStack result = readItemStack(fullJson.get("result"), "item");

        return pattern.asShapedRecipe(recipeId, group, result);
    }

    /**
     * Reads the pattern from the json object, and combines it with the matching item for each character
     * @param fullJson the full json object, representing the recipe
     * @return a list of ingredients
     * @see Pattern
     */
    private Pattern readPattern(WrappedJson fullJson) {
        List<DeclareRecipesPacket.Ingredient> ingredients = new ArrayList<>();

        Char2ObjectMap<List<ItemStack>> recipeKeys = readRecipeKeys(fullJson);
        JsonArray grid = fullJson.getScalarArray("pattern");
        int height = 0;

        for (JsonElement element : grid) {
            String line = element.getAsString();
            for (char item : line.toCharArray()) {
                if (item == ' ') {
                    ingredients.add(EMPTY_INGREDIENT);
                    continue;
                }
                List<ItemStack> stacks = recipeKeys.get(item);
                DeclareRecipesPacket.Ingredient ingredient = new DeclareRecipesPacket.Ingredient(stacks);
                ingredients.add(ingredient);
            }
            height++;
        }

        return new Pattern(ingredients, ingredients.size() / height, height);
    }

    private Char2ObjectMap<List<ItemStack>> readRecipeKeys(WrappedJson fullJson) {
        Char2ObjectMap<List<ItemStack>> keys = new Char2ObjectArrayMap<>();

        fullJson.getMap("key").forEach((itemString, value) -> {
            switch (value) {
                case JsonObject item -> {
                    List<ItemStack> itemStacks = readItemOrTag(WrappedJson.of(item), false);
                    keys.put(itemString.charAt(0), itemStacks);
                }
                case JsonArray array -> {
                    for (JsonElement elem : array) {
                        List<ItemStack> itemStacks = readItemOrTag(WrappedJson.of(elem), false);
                        keys.put(itemString.charAt(0), itemStacks);
                    }
                }
                default -> throw new IllegalStateException();
            }
        });
        return keys;
    }

    // SHAPELESS RECIPE

    private ShapelessRecipe loadShapelessRecipe(WrappedJson fullJson, String recipeId, String group) {
        List<DeclareRecipesPacket.Ingredient> ingredients = readIngredients(fullJson);
        ItemStack result = readItemStack(fullJson.get("result"), "item");

        return new ShapelessRecipeImpl(recipeId, group, ingredients, result);
    }

    private List<DeclareRecipesPacket.Ingredient> readIngredients(WrappedJson fullJson) {
        List<DeclareRecipesPacket.Ingredient> ingredients = new ArrayList<>();
        CompoundArray array = fullJson.getCompoundArray("ingredients");

        for (WrappedJson item : array) {
            List<ItemStack> stacks = readItemOrTag(item, true);
            DeclareRecipesPacket.Ingredient ingredient = new DeclareRecipesPacket.Ingredient(stacks);
            ingredients.add(ingredient);
        }
        return ingredients;
    }

    // RECIPE WITH COMMON BASE

    private Recipe loadCommonBaseRecipe(WrappedJson fullJson, String recipeId, String group, CommonBaseRecipeMapper mapper) {
        DeclareRecipesPacket.Ingredient ingredient = readCommonBaseIngredient(fullJson);
        ItemStack result = readItemStack(fullJson, "result");
        float experience = fullJson.getFloat("experience");
        int cookingTime = fullJson.getInt("cookingTime");

        return mapper.map(recipeId, group, ingredient, result, experience, cookingTime);
    }

    private DeclareRecipesPacket.Ingredient readCommonBaseIngredient(WrappedJson fullJson) {
        return switch (fullJson.getRaw("ingredient")) {
            case JsonObject obj -> {
                List<ItemStack> stacks = readItemOrTag(WrappedJson.of(obj), false);
                yield new DeclareRecipesPacket.Ingredient(stacks);
            }
            case JsonArray array -> {
                List<ItemStack> stacks = new ArrayList<>();
                for (JsonElement element : array) {
                    ItemStack stack = readItemStack(WrappedJson.of(element), "item");
                    stacks.add(stack);
                }
                yield new DeclareRecipesPacket.Ingredient(stacks);
            }
            default -> throw new IllegalStateException();
        };
    }

    // STONECUTTER RECIPE

    // TODO: fix DeclareRecipesPacket$DeclaredStonecutterRecipe#type returning "stonecutter" instead of "stonecutting", minestom bug?
    private StonecutterRecipe loadStonecutterRecipe(WrappedJson fullJson, String recipeId, String group) {
        WrappedJson obj = fullJson.get("ingredient");
        List<ItemStack> stacks = readItemOrTag(obj, false);
        DeclareRecipesPacket.Ingredient ingredient = new DeclareRecipesPacket.Ingredient(stacks);
        ItemStack result = readItemStack(fullJson, "result");

        return new StonecutterRecipeImpl(recipeId, group, ingredient, result);
    }

    // SMITHING RECIPE

    private SmithingRecipe loadSmithingRecipe(WrappedJson fullJson, String recipeId) {
        DeclareRecipesPacket.Ingredient base = readIngredient(fullJson, "base");
        DeclareRecipesPacket.Ingredient addition = readIngredient(fullJson, "addition");
        ItemStack result = readItemStack(fullJson.get("result"), "item");

        return new SmithingRecipeImpl(recipeId, base, addition, result);
    }

    private DeclareRecipesPacket.Ingredient readIngredient(WrappedJson fullJson, String memberName) {
        WrappedJson obj = fullJson.get(memberName);
        ItemStack stack = readItemStack(obj, "item");
        return new DeclareRecipesPacket.Ingredient(Collections.singletonList(stack));
    }

    // COMMON STUFF

    /**
     * Tries to read the "item" key from the entered object and converts it to an ItemStack list with 1 element <br>
     * If this succeeds, and the item retrieved is part of a group, it is cached for later calls of this method <br>
     * If instead the "item" key does not exist, it assumes the "tag" key exists and then looks up the contents of this tag
     * from the cache as described above
     * @param obj the object to read from
     * @param readCount whether to read the "count" tag to determine the item amount, or fall back to 1
     * @return a list of items, which must not be empty
     */
    private List<ItemStack> readItemOrTag(WrappedJson obj, boolean readCount) {
        String namespaceId = obj.getString("item");
        if (namespaceId == null) {
            String tagName = obj.getString("tag").replace("minecraft:", "");
            return tags.get(tagName);
        }
        Material material = Material.fromNamespaceId(namespaceId);
        int count = readCount ? obj.getInt("count", 1) : 1;

        return Collections.singletonList(ItemStack.of(material, count));
    }

    private ItemStack readItemStack(WrappedJson obj, String memberName) {
        Material material = Material.fromNamespaceId(obj.getString(memberName));
        return ItemStack.of(material);
    }

    /**
     * Represents a pattern like
     * <pre>
     *     "pattern": [
     *     "###",
     *     "###",
     *     " X "
     *   ]
     * </pre>
     * @param ingredients the ingredients in the pattern, max 9
     * @param width the width of the pattern
     * @param height the height of the pattern
     */
    private record Pattern(List<DeclareRecipesPacket.Ingredient> ingredients, int width, int height) {
        public ShapedRecipe asShapedRecipe(String recipeId, String group, ItemStack result) {
            return new ShapedRecipeImpl(recipeId, width, height, group, ingredients, result);
        }
    }

    @FunctionalInterface
    private interface CommonBaseRecipeMapper {
        Recipe map(String recipeId, String group, DeclareRecipesPacket.Ingredient ingredient, ItemStack result, float experience, int cookingTime);
    }
}
