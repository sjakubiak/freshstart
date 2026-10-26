package com.freshstart.app.data

data class Lesson(val title: String, val body: String)

/** One short lesson a day, cycling through the list. */
object Lessons {
    val all: List<Lesson> = listOf(
        Lesson(
            "Find your why",
            "Write down the real reason you want to lose weight. Not a number, but what it will let you do or feel. Playing with your kids without getting winded? Feeling confident at a wedding? When motivation dips, your why is what keeps you going. Put it somewhere you'll see it every day."
        ),
        Lesson(
            "Calorie density, explained",
            "Some foods pack a lot of calories into a small amount (oil, nuts, chocolate). Others give you a big, filling plate for very few (vegetables, fruit, soup). Green foods are low density, yellow are medium, red are high. No food is banned. You're just learning to fill up on more green and to be mindful with red."
        ),
        Lesson(
            "Progress, not perfection",
            "One big meal doesn't undo a week of good choices, just like one salad doesn't undo a week of takeout. Weight loss comes from your average behavior over time. If today went off track, log it honestly and make the next meal a good one. That's the whole trick."
        ),
        Lesson(
            "Why logging works",
            "People who track what they eat lose about twice as much weight as those who don't. Not because logging is magic, but because it builds awareness. You can't change what you don't notice. Even rough logs count. Aim for honest, not exact."
        ),
        Lesson(
            "Scale weight bounces around",
            "Your weight can swing 1 to 4 pounds from day to day because of water, salt, carbs and digestion. That isn't fat. Weigh yourself at the same time each morning, after the bathroom and before eating, and watch the trend line over weeks rather than any single day."
        ),
        Lesson(
            "Volume eating",
            "Start meals with a big salad, a broth-based soup, or a plate of vegetables. These green foods fill your stomach so you naturally eat less of the denser stuff afterward, without feeling deprived."
        ),
        Lesson(
            "Hunger or craving?",
            "Physical hunger builds slowly, and any food would satisfy it. Emotional cravings hit suddenly and want something specific, usually sweet, salty or crunchy. Next time you want a snack, pause and ask: would an apple do? If not, it might be a craving. Try waiting 10 minutes."
        ),
        Lesson(
            "Drink water first",
            "Thirst is easy to mistake for hunger. Try a glass of water before each meal and when a craving strikes. Staying hydrated also helps with energy and steadies those daily scale swings."
        ),
        Lesson(
            "Protein keeps you full",
            "Protein (chicken, fish, eggs, beans, Greek yogurt, tofu) is the most filling nutrient and helps you keep muscle while losing fat. Try to include a palm-sized portion at each meal."
        ),
        Lesson(
            "The all-or-nothing trap",
            "\"I already ate a cookie, so the day is ruined\" is one of the most common thoughts behind overeating. It's a thinking trap. A cookie is about 150 calories. Ten more cookies is 1,500. Stopping at one is a win, not a failure."
        ),
        Lesson(
            "Eat slowly",
            "It takes about 20 minutes for your brain to register that you're full. Put your fork down between bites, chew thoroughly, and try to make meals last at least 15 minutes. You'll often find you're satisfied with less."
        ),
        Lesson(
            "Sleep matters",
            "Short sleep raises the hormones that make you hungry and lowers the ones that make you feel full. People who sleep under 6 hours tend to eat more the next day. Aim for 7 to 9 hours. It's one of the easiest wins for weight loss."
        ),
        Lesson(
            "Plan for tough moments",
            "Think about when you usually go off track: late at night, at work parties, when stressed. Make an if-then plan: \"If I want a snack after 9pm, then I'll have tea and a yogurt.\" Deciding in advance is far easier than deciding in the moment."
        ),
        Lesson(
            "Move more, every day",
            "You don't need intense workouts. Walking is one of the best tools for weight loss and mood. Try adding a 10-minute walk after meals, taking stairs, or parking further away. It all adds up."
        ),
        Lesson(
            "Liquid calories",
            "Soda, juice, fancy coffees and alcohol add calories without making you feel full. Swapping one daily soda for sparkling water can save about 50,000 calories a year. That's roughly 14 pounds."
        ),
        Lesson(
            "Shrink your plate",
            "People eat more from bigger plates and bowls without noticing. Use smaller dishes, serve food in the kitchen instead of family style, and put snacks in a bowl rather than eating from the bag."
        ),
        Lesson(
            "Make the easy choice the healthy one",
            "Your environment shapes what you eat more than willpower does. Keep fruit on the counter and washed vegetables at eye level in the fridge. Put treats out of sight, or don't keep them at home."
        ),
        Lesson(
            "Mindful eating",
            "Eating while scrolling or watching TV makes it easy to overeat because your brain isn't paying attention. Try eating at least one meal a day with no screens. Notice the taste, texture and how full you feel."
        ),
        Lesson(
            "Stress and food",
            "Stress raises cortisol, which drives cravings for comfort food. Build a list of things that calm you that aren't food: a walk, a call with a friend, music, a shower, deep breathing. Reach for the list first."
        ),
        Lesson(
            "Celebrate non-scale victories",
            "Clothes fitting better, more energy, better sleep, climbing stairs easily. These wins matter as much as the number on the scale, and they often show up first. Notice them and give yourself credit."
        ),
        Lesson(
            "Weekend wobble",
            "Many people are on track Monday to Friday and then eat back the week's deficit on Saturday and Sunday. You don't have to be strict on weekends, but keep logging. Awareness alone keeps things in check."
        ),
        Lesson(
            "Eating out",
            "Restaurant meals are often 1,000+ calories. Look at the menu ahead of time, start with a salad or soup, ask for sauces on the side, and box half your meal before you start eating."
        ),
        Lesson(
            "Fiber is your friend",
            "Fiber from vegetables, fruit, beans and whole grains slows digestion and keeps you full longer. Most people get about half of what they need. Add one extra serving of vegetables or beans each day."
        ),
        Lesson(
            "Plateaus are normal",
            "Almost everyone hits weeks where the scale doesn't move. Your body is adjusting. Stay consistent, double check your logging and portions, and add a bit more movement. The trend usually starts moving again."
        ),
        Lesson(
            "Be kind to yourself",
            "Talking to yourself harshly after a slip makes you more likely to give up. Talk to yourself like you would a good friend: \"That happens. What's one thing I can do better next time?\" Self-compassion is linked to better long-term results."
        ),
        Lesson(
            "Prep ahead",
            "When you're hungry and nothing is ready, takeout wins. Spend an hour on the weekend cooking a batch of protein, chopping vegetables, and portioning snacks. Future you will thank you."
        ),
        Lesson(
            "Smart swaps",
            "Small swaps add up: Greek yogurt for sour cream, cauliflower rice for half your rice, mustard for mayo, air-popped popcorn for chips, a bowl of berries for dessert."
        ),
        Lesson(
            "Habit stacking",
            "Attach a new habit to something you already do. \"After I pour my morning coffee, I'll log my breakfast.\" \"After I brush my teeth at night, the kitchen is closed.\" Linking habits makes them stick."
        ),
        Lesson(
            "Think long term",
            "Losing 1 pound a week doesn't sound like much, but it's 50 pounds in a year. The habits that get you there are the same ones that keep the weight off. Choose changes you could imagine living with for good."
        ),
        Lesson(
            "Look how far you've come",
            "Take a moment to look at your progress chart and your logs. Every entry is proof that you're showing up for yourself. Keep going. You're building a new normal, one day at a time."
        )
    )

    fun forDay(dayNumber: Long): Int = (((dayNumber % all.size) + all.size) % all.size).toInt()
}
