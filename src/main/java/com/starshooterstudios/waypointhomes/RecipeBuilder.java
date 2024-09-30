package com.starshooterstudios.waypointhomes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeBuilder {
    private final ItemStack item;
    private RecipeType recipeType;
    private final NamespacedKey key;
    private final List<Material> items = new ArrayList<>();

    private Shape shape;

    private RecipeBuilder(ItemStack item, NamespacedKey key) {
        this.item = item;
        this.key = key;
        recipeType = RecipeType.SHAPELESS;
    }

    public RecipeBuilder shape(String line1, String line2, String line3) {
        recipeType = RecipeType.SHAPED;
        shape = new Shape(line1, line2, line3);
        return this;
    }

    public RecipeBuilder addItem(char c, Material item) {
        if (recipeType != RecipeType.SHAPED) {
            throw new IllegalArgumentException("Recipe is not Shaped, a char must not be applied with a material");
        }
        items.add(item);
        shape.item(c, item);
        return this;
    }

    public static RecipeBuilder builder(ItemStack item, NamespacedKey key) {
        return new RecipeBuilder(item, key);
    }

    private Recipe build() {
        if (recipeType.equals(RecipeType.SHAPELESS)) {
            ShapelessRecipe recipe = new ShapelessRecipe(key, item);
            for (Material item : items) recipe.addIngredient(item);
            return recipe;
        }

        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape(shape.line1, shape.line2, shape.line3);
        for (char c : shape.chars.keySet()) recipe.setIngredient(c, shape.item(c));
        return recipe;
    }

    public void buildAndRegister() {
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(build());
    }

    public enum RecipeType {
        SHAPED,
        SHAPELESS
    }

    private static class Shape {
        private final String line1;
        private final String line2;
        private final String line3;

        public Shape(String line1, String line2, String line3) {
            this.line1 = line1;
            this.line2 = line2;
            this.line3 = line3;
        }

        public void item(char c, Material item) {
            chars.put(c, item);
        }

        public Material item(char c) {
            return chars.get(c);
        }

        private final Map<Character, Material> chars = new HashMap<>();
    }
}
