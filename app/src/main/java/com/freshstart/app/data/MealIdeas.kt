package com.freshstart.app.data

import com.freshstart.app.data.FoodColor.GREEN
import com.freshstart.app.data.FoodColor.RED
import com.freshstart.app.data.FoodColor.YELLOW
import com.freshstart.app.data.Meal.BREAKFAST
import com.freshstart.app.data.Meal.DINNER
import com.freshstart.app.data.Meal.LUNCH
import com.freshstart.app.data.Meal.SNACK
import java.time.LocalDate
import kotlin.math.abs

/**
 * A ready-made healthy meal. [ingredients] are short generic names used to match
 * against the foods in your kitchen. [color] is the meal's main Noom color.
 */
data class MealIdea(
    val name: String,
    val meal: Meal,
    val calories: Int,
    val color: FoodColor,
    val ingredients: List<String>,
    val howTo: String,
    val fromAi: Boolean = false
)

object MealIdeas {
    private fun m(name: String, meal: Meal, kcal: Int, color: FoodColor, ingredients: String, howTo: String) =
        MealIdea(name, meal, kcal, color, ingredients.split(",").map { it.trim() }, howTo)

    val all: List<MealIdea> = listOf(
        // Breakfast
        m("Veggie egg scramble with toast", BREAKFAST, 330, YELLOW, "eggs, spinach, tomato, bread",
            "Scramble 2 eggs with a handful of spinach and chopped tomato. Serve with 1 slice of whole wheat toast."),
        m("Greek yogurt berry bowl", BREAKFAST, 280, GREEN, "greek yogurt, berries, granola",
            "Top a cup of non-fat Greek yogurt with a cup of berries and 2 tbsp granola."),
        m("Overnight oats with banana", BREAKFAST, 340, GREEN, "oats, milk, banana, cinnamon",
            "Mix 1/2 cup oats with 3/4 cup milk overnight. Top with sliced banana and cinnamon."),
        m("Peanut butter banana toast", BREAKFAST, 350, YELLOW, "bread, peanut butter, banana",
            "Spread 1 tbsp peanut butter on a slice of whole wheat toast and top with half a sliced banana."),
        m("Egg white veggie omelette", BREAKFAST, 220, GREEN, "eggs, peppers, onion, mushrooms",
            "Cook 4 egg whites (or 1 egg + 2 whites) with peppers, onion and mushrooms."),
        m("Cottage cheese and fruit", BREAKFAST, 240, YELLOW, "cottage cheese, pineapple, peaches",
            "A cup of low-fat cottage cheese with a cup of chopped fruit."),
        m("Breakfast burrito", BREAKFAST, 400, YELLOW, "eggs, tortilla, black beans, salsa, cheese",
            "Wrap 2 scrambled eggs, 1/4 cup black beans, salsa and a sprinkle of cheese in a tortilla."),
        m("Smoothie: berry protein", BREAKFAST, 300, GREEN, "berries, banana, milk, protein powder, spinach",
            "Blend frozen berries, half a banana, a scoop of protein, a handful of spinach and milk."),
        m("Avocado toast with egg", BREAKFAST, 360, YELLOW, "bread, avocado, eggs",
            "Mash 1/4 avocado on whole wheat toast and top with a fried or poached egg."),
        m("Oatmeal with apple and cinnamon", BREAKFAST, 290, GREEN, "oats, apple, cinnamon, milk",
            "Cook 1/2 cup oats with milk or water, stir in diced apple and cinnamon."),

        // Lunch
        m("Grilled chicken salad", LUNCH, 380, GREEN, "chicken, lettuce, tomato, cucumber, dressing",
            "Big bowl of greens, tomato and cucumber topped with 4 oz grilled chicken and 2 tbsp light dressing."),
        m("Turkey and veggie wrap", LUNCH, 410, YELLOW, "tortilla, turkey, lettuce, tomato, mustard",
            "Wrap 4 oz turkey, lettuce, tomato and mustard in a whole wheat tortilla. Side of baby carrots."),
        m("Tuna salad lettuce cups", LUNCH, 300, YELLOW, "tuna, greek yogurt, celery, lettuce",
            "Mix a can of tuna with 2 tbsp Greek yogurt and chopped celery. Spoon into lettuce leaves."),
        m("Lentil vegetable soup", LUNCH, 320, GREEN, "lentils, carrots, celery, onion, tomato",
            "Two cups of lentil and vegetable soup. Make a big pot for the week."),
        m("Chicken rice bowl", LUNCH, 480, YELLOW, "chicken, rice, broccoli, soy sauce",
            "4 oz chicken over 3/4 cup rice with a cup of steamed broccoli and a splash of soy sauce."),
        m("Black bean quinoa bowl", LUNCH, 450, YELLOW, "quinoa, black beans, corn, salsa, peppers",
            "3/4 cup quinoa with 1/2 cup black beans, corn, peppers and salsa."),
        m("Turkey sandwich and fruit", LUNCH, 420, YELLOW, "bread, turkey, lettuce, tomato, apple",
            "Turkey sandwich on whole wheat with lettuce and tomato, plus an apple."),
        m("Egg salad on greens", LUNCH, 340, YELLOW, "eggs, greek yogurt, lettuce, cucumber",
            "Chop 2 hard-boiled eggs with Greek yogurt and mustard. Serve on a big bed of greens."),
        m("Hummus veggie plate", LUNCH, 360, YELLOW, "hummus, pita, carrots, cucumber, peppers",
            "1/4 cup hummus with a small pita and lots of sliced vegetables."),
        m("Leftover chili", LUNCH, 400, YELLOW, "ground turkey, beans, tomato, onion",
            "One and a half cups of turkey and bean chili."),

        // Dinner
        m("Baked salmon with veg and potato", DINNER, 520, YELLOW, "salmon, broccoli, potato, lemon",
            "Bake 4 oz salmon with lemon. Serve with roasted broccoli and a small potato."),
        m("Chicken stir-fry", DINNER, 480, YELLOW, "chicken, peppers, broccoli, onion, rice, soy sauce",
            "Stir-fry 4 oz chicken with lots of vegetables in 1 tsp oil. Serve with 1/2 cup rice."),
        m("Turkey chili", DINNER, 450, YELLOW, "ground turkey, beans, tomato, onion, peppers",
            "Brown lean ground turkey with onion and peppers, add beans, tomatoes and chili spices."),
        m("Spaghetti with meat sauce and salad", DINNER, 560, YELLOW, "pasta, ground beef, tomato sauce, lettuce",
            "1 cup pasta with lean meat sauce and a side salad. Fill half the plate with salad."),
        m("Sheet-pan chicken and vegetables", DINNER, 460, GREEN, "chicken, zucchini, peppers, onion, potato",
            "Roast chicken breast and chopped vegetables on one tray with herbs and 1 tbsp oil."),
        m("Shrimp tacos with slaw", DINNER, 470, YELLOW, "shrimp, tortilla, cabbage, lime, salsa",
            "2 small tortillas with grilled shrimp, cabbage slaw and salsa."),
        m("Lean burger lettuce wrap with sweet potato", DINNER, 510, YELLOW, "ground beef, lettuce, tomato, sweet potato",
            "A lean beef patty wrapped in lettuce with tomato, plus baked sweet potato wedges."),
        m("Veggie omelette and salad", DINNER, 380, GREEN, "eggs, spinach, mushrooms, cheese, lettuce",
            "3-egg omelette with spinach, mushrooms and a little cheese, plus a side salad."),
        m("Baked cod with rice and green beans", DINNER, 440, YELLOW, "fish, rice, green beans, lemon",
            "Bake 5 oz cod with lemon and herbs. Serve with 1/2 cup rice and green beans."),
        m("Pork chop with apples and veg", DINNER, 500, YELLOW, "pork, apple, carrots, green beans",
            "Pan-cook a lean pork chop, top with sauteed apple slices, serve with vegetables."),
        m("Tofu vegetable curry", DINNER, 480, YELLOW, "tofu, coconut milk, peppers, spinach, rice",
            "Simmer tofu and vegetables in light coconut milk and curry paste. Serve with 1/2 cup rice."),

        // Snacks
        m("Apple with peanut butter", SNACK, 190, YELLOW, "apple, peanut butter",
            "A sliced apple with 1 tbsp peanut butter."),
        m("Carrots and hummus", SNACK, 130, GREEN, "carrots, hummus", "A cup of baby carrots with 2 tbsp hummus."),
        m("Greek yogurt cup", SNACK, 120, GREEN, "greek yogurt", "A single-serve non-fat Greek yogurt."),
        m("Handful of almonds", SNACK, 165, RED, "almonds", "About 23 almonds (1 oz). Count them out!"),
        m("Air-popped popcorn", SNACK, 100, YELLOW, "popcorn", "3 cups of air-popped popcorn with a pinch of salt."),
        m("Hard-boiled eggs", SNACK, 145, YELLOW, "eggs", "Two hard-boiled eggs with salt and pepper."),
        m("Berries", SNACK, 70, GREEN, "berries", "A big bowl of fresh or frozen berries."),
        m("String cheese and grapes", SNACK, 160, YELLOW, "cheese, grapes", "One string cheese and a cup of grapes."),
        m("Cucumber and cottage cheese", SNACK, 110, GREEN, "cucumber, cottage cheese",
            "Sliced cucumber with 1/2 cup low-fat cottage cheese."),
        m("Dark chocolate square and tea", SNACK, 90, RED, "dark chocolate", "One or two squares of dark chocolate with a cup of tea.")
    )

    /** Share of the day's calories for each meal. */
    fun mealShare(meal: Meal) = when (meal) {
        BREAKFAST -> 0.25
        LUNCH -> 0.30
        DINNER -> 0.35
        SNACK -> 0.10
    }

    fun pantryMatches(idea: MealIdea, pantry: Collection<String>): List<String> =
        idea.ingredients.filter { ing -> pantry.any { p -> matches(ing, p) } }

    private fun matches(ingredient: String, pantryItem: String): Boolean {
        val a = ingredient.lowercase().trim()
        val b = pantryItem.lowercase().trim().removeSuffix("s")
        if (b.length < 3) return a == b
        return a.contains(b) || b.contains(a.removeSuffix("s"))
    }

    /**
     * Ideas for a meal, best first: close to the calorie target and using what's in your kitchen.
     * The order shifts a little each day so the plan doesn't repeat.
     */
    fun ranked(meal: Meal, targetCalories: Int, pantry: Collection<String>, date: LocalDate, extra: List<MealIdea> = emptyList()): List<MealIdea> {
        val day = date.toEpochDay()
        return (extra + all).filter { it.meal == meal }.sortedBy { idea ->
            val fit = abs(idea.calories - targetCalories).toDouble() / targetCalories.coerceAtLeast(1)
            val have = if (pantry.isEmpty()) 0.0 else pantryMatches(idea, pantry).size.toDouble() / idea.ingredients.size
            val variety = ((idea.name.hashCode().toLong() + day * 31).mod(7L)) / 20.0
            val over = if (idea.calories > targetCalories * 1.25) 0.5 else 0.0
            fit + over - have * 0.6 + variety - (if (idea.fromAi) 0.1 else 0.0)
        }
    }
}
