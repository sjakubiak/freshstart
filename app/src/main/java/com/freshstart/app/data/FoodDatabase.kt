package com.freshstart.app.data

import com.freshstart.app.data.FoodColor.GREEN
import com.freshstart.app.data.FoodColor.RED
import com.freshstart.app.data.FoodColor.YELLOW

/** Common foods with typical calories per serving. Colors follow calorie density. */
object FoodDatabase {
    private fun f(name: String, serving: String, kcal: Int, color: FoodColor) =
        FoodItem(name, serving, kcal, color)

    val foods: List<FoodItem> = listOf(
        // Fruit
        f("Apple", "1 medium", 95, GREEN),
        f("Banana", "1 medium", 105, GREEN),
        f("Orange", "1 medium", 62, GREEN),
        f("Strawberries", "1 cup", 50, GREEN),
        f("Blueberries", "1 cup", 85, GREEN),
        f("Grapes", "1 cup", 104, GREEN),
        f("Watermelon", "2 cups diced", 90, GREEN),
        f("Pear", "1 medium", 100, GREEN),
        f("Pineapple", "1 cup", 82, GREEN),
        f("Mango", "1 cup", 99, GREEN),
        f("Avocado", "1/2 fruit", 120, YELLOW),
        f("Raisins", "1/4 cup", 120, RED),

        // Vegetables
        f("Broccoli", "1 cup", 31, GREEN),
        f("Carrots", "1 cup", 52, GREEN),
        f("Salad greens", "2 cups", 15, GREEN),
        f("Spinach", "2 cups raw", 14, GREEN),
        f("Tomato", "1 medium", 22, GREEN),
        f("Cucumber", "1 cup", 16, GREEN),
        f("Bell pepper", "1 medium", 30, GREEN),
        f("Green beans", "1 cup", 35, GREEN),
        f("Cauliflower", "1 cup", 27, GREEN),
        f("Zucchini", "1 cup", 20, GREEN),
        f("Mushrooms", "1 cup", 15, GREEN),
        f("Sweet potato", "1 medium baked", 112, GREEN),
        f("Potato", "1 medium baked", 160, GREEN),
        f("Corn", "1 ear", 90, GREEN),
        f("Peas", "1/2 cup", 60, GREEN),

        // Soups & dairy
        f("Vegetable soup", "1 cup", 80, GREEN),
        f("Chicken noodle soup", "1 cup", 90, GREEN),
        f("Skim milk", "1 cup", 85, GREEN),
        f("Non-fat Greek yogurt", "170 g cup", 100, GREEN),
        f("Unsweetened almond milk", "1 cup", 35, GREEN),
        f("Whole milk", "1 cup", 150, YELLOW),
        f("Low-fat cottage cheese", "1/2 cup", 90, YELLOW),
        f("Cheddar cheese", "1 oz slice", 115, RED),
        f("Mozzarella (part skim)", "1 oz", 72, YELLOW),
        f("Ice cream", "1/2 cup", 140, RED),

        // Grains
        f("Oatmeal (cooked)", "1 cup", 155, GREEN),
        f("Brown rice", "1 cup cooked", 215, YELLOW),
        f("White rice", "1 cup cooked", 205, YELLOW),
        f("Pasta", "1 cup cooked", 220, YELLOW),
        f("Whole wheat bread", "1 slice", 80, YELLOW),
        f("White bread", "1 slice", 75, YELLOW),
        f("Bagel", "1 medium", 270, YELLOW),
        f("Tortilla (flour)", "1 medium", 140, YELLOW),
        f("Quinoa", "1 cup cooked", 222, YELLOW),
        f("Cereal (bran flakes)", "1 cup", 130, YELLOW),
        f("Granola", "1/2 cup", 280, RED),
        f("Crackers", "6 crackers", 120, RED),
        f("Popcorn (air-popped)", "3 cups", 95, YELLOW),

        // Protein
        f("Egg", "1 large", 72, YELLOW),
        f("Egg whites", "3 large", 51, GREEN),
        f("Chicken breast", "4 oz cooked", 185, YELLOW),
        f("Turkey breast", "4 oz", 150, YELLOW),
        f("Salmon", "4 oz cooked", 235, YELLOW),
        f("Tuna (canned in water)", "1 can", 120, YELLOW),
        f("Shrimp", "4 oz", 110, YELLOW),
        f("White fish (cod, tilapia)", "4 oz", 110, YELLOW),
        f("Lean ground beef (93%)", "4 oz cooked", 200, YELLOW),
        f("Ground beef (80%)", "4 oz cooked", 290, RED),
        f("Steak", "4 oz cooked", 270, RED),
        f("Pork chop", "4 oz cooked", 230, YELLOW),
        f("Bacon", "2 slices", 90, RED),
        f("Sausage link", "1 link", 170, RED),
        f("Tofu (firm)", "1/2 cup", 90, YELLOW),
        f("Black beans", "1/2 cup", 115, YELLOW),
        f("Chickpeas", "1/2 cup", 135, YELLOW),
        f("Lentils", "1/2 cup cooked", 115, YELLOW),
        f("Hummus", "2 tbsp", 70, YELLOW),
        f("Protein shake", "1 scoop in water", 120, YELLOW),

        // Fats, nuts, sweets
        f("Peanut butter", "2 tbsp", 190, RED),
        f("Almonds", "1 oz (23 nuts)", 165, RED),
        f("Walnuts", "1 oz", 185, RED),
        f("Olive oil", "1 tbsp", 120, RED),
        f("Butter", "1 tbsp", 100, RED),
        f("Mayonnaise", "1 tbsp", 95, RED),
        f("Ranch dressing", "2 tbsp", 130, RED),
        f("Dark chocolate", "1 oz", 170, RED),
        f("Cookie", "1 medium", 150, RED),
        f("Donut", "1 glazed", 260, RED),
        f("Muffin", "1 medium", 400, RED),
        f("Potato chips", "1 oz", 150, RED),
        f("French fries", "medium serving", 365, RED),
        f("Candy bar", "1 bar", 250, RED),

        // Meals & takeout
        f("Pizza", "1 slice", 285, RED),
        f("Cheeseburger", "1 burger", 500, RED),
        f("Burrito", "1 large", 650, YELLOW),
        f("Chicken sandwich", "1 sandwich", 450, YELLOW),
        f("Turkey sandwich", "1 sandwich", 350, YELLOW),
        f("Sushi roll", "6 pieces", 250, YELLOW),
        f("Chicken stir-fry", "1 cup", 270, YELLOW),
        f("Spaghetti with meat sauce", "1.5 cups", 450, YELLOW),
        f("Mac and cheese", "1 cup", 400, RED),
        f("Fried chicken", "1 piece", 320, RED),
        f("Caesar salad", "1 bowl", 350, RED),
        f("Garden salad, light dressing", "1 bowl", 120, GREEN),

        // Drinks
        f("Coffee, black", "1 cup", 2, GREEN),
        f("Coffee with cream & sugar", "1 cup", 70, YELLOW),
        f("Latte", "12 oz", 190, YELLOW),
        f("Soda", "12 oz can", 140, YELLOW),
        f("Orange juice", "1 cup", 110, YELLOW),
        f("Beer", "12 oz", 155, YELLOW),
        f("Wine", "5 oz glass", 125, YELLOW),
        f("Sports drink", "20 oz", 140, YELLOW)
    )
}
