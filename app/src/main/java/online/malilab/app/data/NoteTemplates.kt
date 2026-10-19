package online.malilab.app.data

/** A starting point for a note. Templates only suggest a shape; every block can be changed or deleted. */
data class NoteTemplate(
    val id: String,
    val name: String,
    val blurb: String,
    val collection: String,
    val title: String,
    val build: () -> List<Block>,
)

private fun h(text: String) = Block(type = "heading", text = text, level = 2)
private fun tip(text: String) = Block(type = "callout", tone = "tip", text = text)
private fun key(text: String) = Block(type = "callout", tone = "key", text = text)
private fun p(text: String = "") = Block(text = text)

val NOTE_TEMPLATES = listOf(
    NoteTemplate("blank", "Blank note", "Start with an empty page.", "", "") { listOf(p()) },
    NoteTemplate("savings", "Savings plan", "Set a goal, then test how to reach it.", "Financial goals", "My savings plan") {
        listOf(
            h("What am I saving for?"), tip("Name the goal in one sentence. Why does it matter to you?"), p(),
            h("How much, and by when?"), tip("Write the amount in shillings and the date you need it."), p(),
            h("What can I put aside?"), tip("List what you can save each month. Be honest, not hopeful."), p(),
            h("What the numbers say"), key("Open the calculator and insert your projection here."), p(),
            h("My decision"), tip("After looking at the numbers, what will you do, and starting when?"), p(),
        )
    },
    NoteTemplate("retirement", "Retirement plan", "How much, how long, and how it lasts.", "Financial goals", "My retirement plan") {
        listOf(
            h("The life I want later"), tip("Where will you live? What will it cost to live there each month?"), p(),
            h("Today"), tip("Your age, what you have saved, what you can add each month."), p(),
            h("What the numbers say"), key("Use the calculator to project savings, then test how long withdrawals will last."), p(),
            h("What could go wrong"), tip("Lower returns, higher prices, an emergency. Try each in the calculator."), p(),
            h("My decision"), p(),
        )
    },
    NoteTemplate("budget", "Household budget", "Where the money comes from and goes.", "Budget & cash flow", "My monthly budget") {
        listOf(
            h("Money in"), tip("Salary, business, side income. Use what arrives in a normal month."), p(),
            h("Money out"), tip("Rent, food, transport, school fees, airtime. Include irregular costs divided by 12."), p(),
            h("What is left"), key("Subtract money out from money in. Is the gap what you expected?"), p(),
            h("One change to try"), p(),
        )
    },
    NoteTemplate("debt", "Debt payoff", "Plan how to clear what you owe.", "Budget & cash flow", "My debt payoff plan") {
        listOf(
            h("What I owe"), tip("For each loan: who, the balance, the interest rate, the monthly payment."), p(),
            h("What I can pay"), p(),
            h("What the numbers say"), key("Use the calculator to compare paying the minimum with paying extra."), p(),
            h("My plan"), p(),
        )
    },
    NoteTemplate("research", "Investment research", "Look before you put money in.", "Investment research", "Investment research") {
        listOf(
            h("What is it?"), tip("Describe the investment in your own words, and who offers it."), p(),
            h("How does it make money?"), p(),
            h("What are the risks?"), tip("What could make you lose money, or make it hard to get out?"), p(),
            h("What the numbers say"), key("Project it in the calculator and insert the result. Mark rates as assumptions."), p(),
            h("Sources"), tip("Where did you get your facts? Add links or names."), p(),
            h("My conclusion"), p(),
        )
    },
    NoteTemplate("business", "Business idea", "Test an idea before you spend on it.", "Calculations & experiments", "Business idea") {
        listOf(
            h("The idea"), p(),
            h("Who will pay, and why?"), p(),
            h("Costs to start"), p(),
            h("Costs each month"), p(),
            h("What the numbers say"), key("Use the calculator to see how long until the money comes back."), p(),
            h("Next step"), p(),
        )
    },
)

fun templateById(id: String): NoteTemplate = NOTE_TEMPLATES.firstOrNull { it.id == id } ?: NOTE_TEMPLATES.first()
