package com.plummy.vio.core.tool.catalog.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.tool.executor.ExecutionResult;
import com.plummy.vio.core.tool.ServerTool;
import com.plummy.vio.core.tool.value.argument.*;
import com.plummy.vio.core.tool.value.catalog.Identifier;
import com.plummy.vio.core.tool.value.catalog.Page;
import com.plummy.vio.core.tool.value.result.ResultValueJsonSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;

import java.util.*;
import java.util.stream.Collectors;

public class GetRecipesTool implements ServerTool {

    private static final int INGREDIENT_MEMBERS_SHOWN = 3;
    private static final int MAX_LIMIT = 50;
    private static final String BREWING_TYPE = "minecraft:brewing";

    private static final Map<String, SpecialRecipe> SPECIAL_RECIPES = Map.ofEntries(
            Map.entry("minecraft:crafting_special_firework_rocket", new SpecialRecipe(List.of(Items.FIREWORK_ROCKET), List.of(Items.PAPER, Items.GUNPOWDER, Items.FIREWORK_STAR), "Paper plus 1-3 gunpowder (flight duration) plus optional firework stars, shapeless.")),
            Map.entry("minecraft:crafting_special_firework_star", new SpecialRecipe(List.of(Items.FIREWORK_STAR), List.of(Items.GUNPOWDER), "Gunpowder plus at least one dye, optionally a shape modifier (fire charge, gold nugget, head, feather) and diamond (trail) or glowstone dust (twinkle).")),
            Map.entry("minecraft:crafting_special_firework_star_fade", new SpecialRecipe(List.of(Items.FIREWORK_STAR), List.of(Items.FIREWORK_STAR), "A firework star plus dyes adds a fade color.")),
            Map.entry("minecraft:crafting_special_tippedarrow", new SpecialRecipe(List.of(Items.TIPPED_ARROW), List.of(Items.ARROW, Items.LINGERING_POTION), "8 arrows around 1 lingering potion make 8 tipped arrows of that potion.")),
            Map.entry("minecraft:crafting_special_mapcloning", new SpecialRecipe(List.of(Items.FILLED_MAP), List.of(Items.FILLED_MAP, Items.MAP), "A filled map plus empty maps makes copies of it.")),
            Map.entry("minecraft:crafting_special_mapextending", new SpecialRecipe(List.of(Items.FILLED_MAP), List.of(Items.FILLED_MAP, Items.PAPER), "A filled map surrounded by 8 paper zooms the map out one level (cartography table also works).")),
            Map.entry("minecraft:crafting_special_bookcloning", new SpecialRecipe(List.of(Items.WRITTEN_BOOK), List.of(Items.WRITTEN_BOOK, Items.WRITABLE_BOOK), "A written book plus book and quills makes copies of it (originals and copies only).")),
            Map.entry("minecraft:crafting_special_armordye", new SpecialRecipe(List.of(), List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR, Items.WOLF_ARMOR), "A dyeable item plus dyes, shapeless, mixes the dye colors into it.")),
            Map.entry("minecraft:crafting_special_bannerduplicate", new SpecialRecipe(List.of(), List.of(), "A patterned banner plus a blank banner of the same color copies the pattern.")),
            Map.entry("minecraft:crafting_special_shielddecoration", new SpecialRecipe(List.of(Items.SHIELD), List.of(Items.SHIELD), "A shield plus a banner puts the banner's pattern on the shield.")),
            Map.entry("minecraft:crafting_special_repairitem", new SpecialRecipe(List.of(), List.of(), "Two damaged items of the same type combine into one with their durability plus 5%; enchantments are lost.")),
            Map.entry("minecraft:crafting_decorated_pot", new SpecialRecipe(List.of(Items.DECORATED_POT), List.of(Items.BRICK), "4 bricks or pottery sherds in a diamond shape; sherds set the faces.")),
            Map.entry("minecraft:crafting_special_shulkerboxcoloring", new SpecialRecipe(List.of(), List.of(), "A shulker box plus a dye recolors it and keeps its contents.")),
            Map.entry("minecraft:crafting_special_suspiciousstew", new SpecialRecipe(List.of(Items.SUSPICIOUS_STEW), List.of(Items.BOWL, Items.RED_MUSHROOM, Items.BROWN_MUSHROOM), "A bowl, a red and a brown mushroom and a flower; the flower picks the effect."))
    );

    @Override
    public String getName() {
        return "get_recipes";
    }

    @Override
    public String getDescription() {
        return "Recipes that produce or use an item, including smelting-type recipes (cook time, experience), smithing, stonecutting, brewing (type minecraft:brewing) and special recipes like fireworks or map cloning (a one-line description). Shaped recipes come as pattern + key; tag ingredients show the tag and its first members. known_by_player tells whether the requester has unlocked it.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("item", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Item id."));
        schema.put("direction", ArgumentDefinition.enumeration(Direction.class, "produces = recipes making the item, uses = recipes consuming it."));
        schema.put("recipe_type", new ArgumentDefinition(ArgumentType.IDENTIFIER, "Recipe type, e.g. minecraft:crafting, minecraft:smelting, minecraft:brewing; empty = all."));
        schema.put("page", ArgumentDefinition.page(MAX_LIMIT, 10, "recipes"));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.BASIC;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        MinecraftServer server = context.getServer();
        Item item = context.getEntry(Registries.ITEM, arguments.getIdentifier("item"), "item").value();
        Direction direction = arguments.getEnum("direction", Direction.class);
        Identifier recipeType = arguments.isEmpty("recipe_type") ? null : arguments.getIdentifier("recipe_type");
        Page page = arguments.getPage("page", MAX_LIMIT);
        ServerPlayer player = context.getPlayer();

        boolean brewingOnly = recipeType != null && recipeType.getId().toString().equals(BREWING_TYPE);
        RecipeType<?> type = recipeType == null || brewingOnly ? null : context.getEntry(Registries.RECIPE_TYPE, recipeType, "recipe type").value();

        List<JsonObject> recipes = new ArrayList<>();
        ItemStack stack = new ItemStack(item);
        RegistryAccess registryAccess = server.registryAccess();

        if (!brewingOnly) {
            List<RecipeHolder<?>> holders = server.getRecipeManager().getRecipes().stream()
                    .filter(holder -> type == null || holder.value().getType() == type)
                    .sorted(Comparator.comparing(holder -> holder.id().toString()))
                    .toList();

            for (RecipeHolder<?> holder : holders) {
                JsonObject json = describe(registryAccess, holder, item, stack, direction);

                if (json != null) {
                    json.addProperty("known_by_player", player.getRecipeBook().contains(holder));
                    recipes.add(json);
                }
            }
        }

        if (type == null) {
            recipes.addAll(getBrewing(server.potionBrewing(), item, direction));
        }

        JsonArray results = new JsonArray();
        page.apply(recipes).forEach(results::add);

        JsonObject json = new JsonObject();

        json.add("recipes", results);
        json.add("page", page.withTotal(recipes.size()).accept(ResultValueJsonSerializer.getInstance()));

        return ExecutionResult.success(json);
    }

    private static JsonObject describe(RegistryAccess registryAccess, RecipeHolder<?> holder, Item item, ItemStack stack, Direction direction) {
        Recipe<?> recipe = holder.value();
        String typeId = String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()));

        if (recipe.isSpecial()) {
            String serializerId = String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()));
            SpecialRecipe special = SPECIAL_RECIPES.get(serializerId);

            if (special == null || !(direction == Direction.PRODUCES ? special.produces() : special.uses()).contains(item)) {
                return null;
            }

            JsonObject json = new JsonObject();

            json.addProperty("id", holder.id().toString());
            json.addProperty("type", typeId);
            json.addProperty("description", special.description());

            return json;
        }

        ItemStack result = recipe.getResultItem(registryAccess);
        boolean matches = direction == Direction.PRODUCES
                ? result.is(item)
                : recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(stack)) || recipe instanceof SmithingRecipe smithing && (smithing.isTemplateIngredient(stack) || smithing.isBaseIngredient(stack) || smithing.isAdditionIngredient(stack));

        if (!matches) {
            return null;
        }

        JsonObject json = new JsonObject();

        json.addProperty("id", holder.id().toString());
        json.addProperty("type", typeId);

        switch (recipe) {
            case ShapedRecipe shaped -> addShaped(json, shaped);
            case AbstractCookingRecipe cooking -> {
                json.add("ingredient", describe(cooking.getIngredients().getFirst()));
                json.addProperty("cook_time_ticks", cooking.getCookingTime());
                json.addProperty("experience", cooking.getExperience());
            }
            case SmithingRecipe smithing -> addSmithing(json, registryAccess, smithing);
            default -> {
                JsonArray ingredients = new JsonArray();
                recipe.getIngredients().stream().filter(ingredient -> !ingredient.isEmpty()).map(GetRecipesTool::describe).forEach(ingredients::add);
                json.add("ingredients", ingredients);
            }
        }

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("item", String.valueOf(BuiltInRegistries.ITEM.getKey(result.getItem())));
        resultJson.addProperty("count", result.getCount());
        json.add("result", resultJson);

        return json;
    }

    private static void addShaped(JsonObject json, ShapedRecipe recipe) {
        Map<String, Character> keys = new LinkedHashMap<>();
        JsonObject key = new JsonObject();
        JsonArray pattern = new JsonArray();
        List<Ingredient> ingredients = recipe.getIngredients();

        for (int row = 0; row < recipe.getHeight(); row++) {
            StringBuilder line = new StringBuilder();

            for (int column = 0; column < recipe.getWidth(); column++) {
                Ingredient ingredient = ingredients.get(row * recipe.getWidth() + column);

                if (ingredient.isEmpty()) {
                    line.append(' ');
                    continue;
                }

                String description = describe(ingredient).getAsString();
                Character symbol = keys.get(description);

                if (symbol == null) {
                    symbol = pickSymbol(description, keys.values());
                    keys.put(description, symbol);
                    key.addProperty(String.valueOf(symbol), description);
                }

                line.append(symbol);
            }

            pattern.add(line.toString());
        }

        json.add("pattern", pattern);
        json.add("key", key);
    }

    private static char pickSymbol(String description, Collection<Character> used) {
        String path = description.substring(description.indexOf(':') + 1);

        if (!path.isEmpty()) {
            char preferred = Character.toUpperCase(path.charAt(0));

            if (Character.isLetter(preferred) && !used.contains(preferred)) {
                return preferred;
            }
        }

        for (char symbol = 'A'; symbol <= 'Z'; symbol++) {
            if (!used.contains(symbol)) {
                return symbol;
            }
        }

        return '?';
    }

    private static void addSmithing(JsonObject json, RegistryAccess registryAccess, SmithingRecipe recipe) {
        JsonElement encoded = Recipe.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registryAccess), recipe).result().orElse(null);

        if (encoded == null || !encoded.isJsonObject()) {
            return;
        }

        for (String part : List.of("template", "base", "addition")) {
            JsonElement value = encoded.getAsJsonObject().get(part);

            if (value != null) {
                json.add(part, describeEncodedIngredient(value));
            }
        }
    }

    private static JsonElement describeEncodedIngredient(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();

            if (object.has("item")) {
                return object.get("item");
            }

            if (object.has("tag")) {
                return describe(Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(object.get("tag").getAsString()))));
            }
        }

        return value;
    }

    private static JsonPrimitive describe(Ingredient ingredient) {
        String prefix = "one of";
        List<String> members = new ArrayList<>();
        Ingredient.Value[] values = ingredient.isCustom() ? new Ingredient.Value[0] : ingredient.getValues();

        if (values.length == 1 && values[0] instanceof Ingredient.ItemValue(ItemStack item)) {
            return new JsonPrimitive(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        }

        if (values.length == 1 && values[0] instanceof Ingredient.TagValue(TagKey<Item> tag)) {
            prefix = "#%s".formatted(tag.location());

            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                members.add(holder.unwrapKey().map(key -> key.location().toString()).orElse("unknown"));
            }
        } else {
            Arrays.stream(ingredient.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).distinct().forEach(members::add);

            if (members.size() == 1) {
                return new JsonPrimitive(members.getFirst());
            }
        }

        String shown = members.stream().limit(INGREDIENT_MEMBERS_SHOWN).collect(Collectors.joining(", "));

        return new JsonPrimitive("%s [%s%s]".formatted(prefix, shown, members.size() > INGREDIENT_MEMBERS_SHOWN ? ", ..." : ""));
    }

    private static List<JsonObject> getBrewing(PotionBrewing brewing, Item item, Direction direction) {
        List<Item> reagents = BuiltInRegistries.ITEM.stream()
                .filter(candidate -> brewing.isIngredient(new ItemStack(candidate)))
                .sorted(Comparator.comparing(candidate -> BuiltInRegistries.ITEM.getKey(candidate).toString()))
                .toList();

        if (direction == Direction.USES && !reagents.contains(item) && !isPotionContainer(item)) {
            return List.of();
        }

        List<Holder.Reference<Potion>> potions = BuiltInRegistries.POTION.holders()
                .sorted(Comparator.comparing(holder -> holder.key().location().toString()))
                .toList();

        List<JsonObject> recipes = new ArrayList<>();

        for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            for (Holder.Reference<Potion> potion : potions) {
                ItemStack input = PotionContents.createItemStack(container, potion);

                for (Item reagent : reagents) {
                    ItemStack reagentStack = new ItemStack(reagent);

                    if (!brewing.hasMix(input, reagentStack)) {
                        continue;
                    }

                    ItemStack output = brewing.mix(reagentStack, input);

                    if (output.isEmpty() || ItemStack.matches(output, input)) {
                        continue;
                    }

                    boolean matches = direction == Direction.PRODUCES ? output.is(item) : reagent == item || container == item;

                    if (matches) {
                        recipes.add(describeBrewing(input, reagent, output));
                    }
                }
            }
        }

        return recipes;
    }

    private static boolean isPotionContainer(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    private static JsonObject describeBrewing(ItemStack input, Item reagent, ItemStack output) {
        String reagentId = BuiltInRegistries.ITEM.getKey(reagent).toString();
        JsonObject json = new JsonObject();

        json.addProperty("id", "brewing:%s+%s".formatted(describePotionId(input), reagentId));
        json.addProperty("type", "brewing");
        json.add("input", describePotion(input));
        json.addProperty("ingredient", reagentId);
        json.add("result", describePotion(output));

        return json;
    }

    private static JsonObject describePotion(ItemStack stack) {
        JsonObject json = new JsonObject();

        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());

        Optional<Holder<Potion>> potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        potion.flatMap(Holder::unwrapKey).ifPresent(key -> json.addProperty("potion", key.location().toString()));

        json.addProperty("count", stack.getCount());

        return json;
    }

    private static String describePotionId(ItemStack stack) {
        String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion()
                .flatMap(Holder::unwrapKey)
                .map(key -> key.location().getPath())
                .orElse("none");

        return "%s/%s".formatted(item, potion);
    }

    private enum Direction {
        PRODUCES,
        USES
    }

    private record SpecialRecipe(List<Item> produces, List<Item> uses, String description) {
    }
}
