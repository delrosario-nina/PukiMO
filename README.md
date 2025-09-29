[PukiMO]
CREATORS
Del Rosario, Nina Claudia E.
Hernia, Christian Joseph G.


LANGUAGE OVERVIEW
PUKIMO Safari Zone Edition is a dynamically typed, Pokémon-themed domain-specific language (DSL) designed to simulate a Safari Zone adventure. Players explore the safari zone, encounter wild Pokémon, throw Safari Balls, and manage a temporary Safari team. Unlike the traditional Pokémon experience, this DSL emphasizes exploration, random encounters, and chance-based catching mechanics, creating a narrative, adventure-like experience.

Main Characteristics:
    1. Simple object-oriented style with lightweight syntax.
    2. Built-in types for SafariZone, Team, and Pokemon.
    3. Declarative DSL-style commands (explore, throwBall, filter) for expressing game-like behavior.
    4. Support for attributes like nature, behavior, friendliness, and shiny.
    5. Human-readable syntax with comments (:>) for clarity and fun.


KEYWORDS
  - if: Introduces a conditional block. Executes code if a condition is true.
  - else: Defines the alternative branch of an if statement.
  - explore: Special loop construct for iterating over Safari Zone turns or balls.
  - run: Exits the current loop or exploration early.
  - define: Declares a user-defined function.
  - print: Outputs text or data to the console.
  - throwBall: Attempts to catch a Pokémon inside an encounter.
  - true: Boolean literal representing truth.
  - false: Boolean literal representing falsehood.
  - SafariZone: Built-in type for managing Safari Zone state (balls, turns, Pokémon).
  - Team: Built-in type for managing the player’s caught Pokémon.
  - Pokemon: Built-in type representing individual Pokémon entities.

1. Built-in Properties & Methods
   These are predefined attributes and functions available on core objects (SafariZone, Team, Pokemon). They are not reserved words, but form part of the standard library.

2. SafariZone
   Properties:
   balls → number of Safari Balls available.
   turns → number of turns left.
   pokemon → collection of Pokémon in the zone.

   Methods:
   refillBalls(amount) → adds more Safari Balls.
   refillTurns(amount) → adds more turns.

3. Team
   Properties:
   pokemon → collection of Pokémon in the team.

   Methods:
   add(pokemon) → adds a Pokémon to the team.
   all() → lists all Pokémon in the team.
   find(name) → finds a Pokémon by name.
   info(name, only=property) → retrieves detailed info about a Pokémon.
   has(name) → checks if a Pokémon is in the team.
   length() → returns number of Pokémon in the team.
   random() → returns a random Pokémon from the team.

4. Pokemon
   Properties:
   level → numeric level of the Pokémon.
   shiny → boolean shiny status.
   nature → string nature value.
   behavior → string describing Pokémon behavior.
   friendliness → numeric friendliness value.
   caught → boolean whether caught.

5. Collection Methods (for both Zone and Team Pokémon)
   add(name) → adds a Pokémon.
   remove(name) → removes a Pokémon.
   all() → returns all Pokémon in the collection.
   find(name) → finds a specific Pokémon.
   random() → returns a random Pokémon.
   filter(criteria) → filters Pokémon based on attributes (e.g., shiny=true, nature="Timid").


OPERATORS
1. Arithmetic Operators
    + → addition or string concatenation.
    - → subtraction.
    * → multiplication.
      / → division.
      % → modulo (remainder).

2. Comparison Operators
   < → less than.
   > → greater than.
   == → equal to.
   != → not equal to.
   >= → greater than or equal to.
   <= → less than or equal to.

3. Logical Operators
   AND → logical conjunction.
   OR → logical disjunction.
   NOT → logical negation.

4. Assignment Operators
   = → assigns a value to a variable or property.

5. Access / Chaining Operators
   -> → calling methods
   . → access a property of an object


LITERALS
1. Numbers
   Only integers are supported (no floats or decimals).
   Used for counts, levels, turns, friendliness, etc.

   Examples:
   myZone = SafariZone(10, 20);

2. Strings
   Enclosed in double quotes " " for names, properties, or messages.

   Examples:
   print("Welcome to the Safari Zone!");
   myZone.pokemon->add("Charmander");

3. Booleans
   Pokémon-flavored boolean literals: true or false.
   Used for shiny, caught, fainted, etc.

   Examples:
   pikachu = myTeam.pokemon->find(“pikachu”);
   pikachu.shiny = true;
   pikachu.caught = false;

4. Null
   Represent the absence of a value.

   Example:
   encounter = null;

5. Arrays
   Enclosed in square brackets [ ] and can store multiple Pokémon or values.

   Examples:
   myTeamList = ["Pikachu", "Bulbasaur", "Charmander"];


IDENTIFIERS
Rules for valid identifiers:
1. Must start with a letter (A-Z or a-z) or an underscore (_).
2. Can contain letters, digits (0-9), and underscores (_).
3. Cannot contain spaces or other special characters.
4. Cannot be a reserved keyword (e.g.,SafariZone, throwball, run, shiny, etc.).
5. Case-sensitive: Ash and ash are treated as different identifiers.


Recommended Naming Style:
1. camelCase for variables and functions.
2. PascalCase for object-like structures or constants

Example:
pikachuLevel = 15
BulbasaurStats = [100, 80, 90]
moveName = "Thunderbolt"

COMMENTS
1. :> - single line comments are written like this
   :> this is a comment
2. /* - multi-line comments are written like this
   /* this is a multiline
   comment */
3. Nested comments are not supported


SYNTAX STYLE
1. Whitespace: Not significant, but indentation is recommended for readability.
2. Statement termination: Semicolons ; are required at the end of every statement.
   e.g.
   print("You caught Pikachu!");
3. Blocks: Use curly braces { } for grouping multiple statements.
   e.g.
   explore(myZone) {
   encounter = myZone.pokemon->random();
   print("A wild " + encounter + " appeared!");

          tryCatchEncounter(encounter);
   }
4. Instance method chaining: Use -> for calling object methods.
   e.g.
   encounter = myZone.pokemon->random();
5. Use . for class property access.
   e.g.
   myZone = SafariZone(10,10);
   print(myZone.balls);
6. Line breaks: Statements can be split across multiple lines for readability, but the semicolon must remain at the end.

SAMPLE CODE

:> Initialize Safari Zone with 10 Safari Balls and 20 turns
myZone = SafariZone(10, 20);
myTeam = Team();

:> Welcome messages
print("Welcome to the Safari Zone!");
print("You have " + myZone.balls + " Safari Balls.");
print("You can explore for " + myZone.turns + " turns.");

:> Add Pokémon to the Safari Zone
myZone.pokemon->add("Squirtle");
myZone.pokemon->add("Charmander");
myZone.pokemon->add("Bulbasaur");
myZone.pokemon->remove("Squirtle");

:> Display Pokémon in the zone
print("Pokémon roaming the zone:");
print(myZone.pokemon->all());
print("Finding Bulbasaur:");
print(myZone.pokemon->find("Bulbasaur"));

:> helper
define tryCatchEncounter(pokemon) {
throwBall(pokemon) {
myTeam->add(pokemon);
pokemon.caught = true;
print(pokemon + " was caught!");
}
else {
pokemon.caught = false;
print(pokemon + " escaped!");
}
    :> Reduce Safari Balls automatically
    myZone.balls = myZone.balls - 1;
    :> Stop exploring if out of balls
    if myZone.balls == 0 {
        print("You're out of Safari Balls!");
        run;
    }
}

:> exploration loop structure
explore(myZone) {
encounter = myZone.pokemon->random();
print("A wild " + encounter + " appeared!");
    tryCatchEncounter(encounter);
}

:> exploration loop with explicit turns
explore(myZone.turns) {
encounter = myZone.pokemon->random();
print("A wild " + encounter + " appeared!");
    tryCatchEncounter(encounter);
}

:> Team inspection
print("Team Pokémon info:");
print(myTeam.pokemon->all());

print("Pikachu info:");
print(myTeam.pokemon->info("Pikachu"));
myTeam->has("Pikachu")
myTeam->length()
myTeam->random()
print("Pikachu shiny status:");
print(myTeam.pokemon->info("Pikachu", only="shiny"));

/* Pokémon properties
pikachu.level = 5;
pikachu.shiny = true;
pikachu.nature = "Timid";
pikachu.behavior = "Curious";
pikachu.friendliness = 70;
pikachu.caught = false;
*/

:> Safari Zone property manipulation
myZone->refillBalls(10);
print("You now have " + myZone.balls + " Safari Balls!");

myZone->refillTurns(5);
print("You can explore for " + myZone.turns + " more turns!");

:> Filtering
print("Filtering Timid Pokémon in the zone:");
print(myZone.pokemon->filter(nature="Timid"));

print("Filtering shiny Pokémon in team:");
print(myTeam.pokemon->filter(shiny=true));

DESIGN RATIONALE
1. Single Trainer
    - The language assumes one implicit Trainer. This removes boilerplate and keeps code simple, like in the games (no need to declare a trainer each time).
2. Pokémon-flavored literals and booleans
    - Properties like shiny, nature, behavior, friendliness, and caught are built-in. This keeps the language close to the Pokémon world instead of generic programming terms.
3. Arrow-based method chaining (->)
    - Inspired by Laravel Eloquent ORM, looks cleaner to chain when compared to (.).
4. Static/Class-level access (.)
    - This cleanly separates global utilities from instance methods, while keeping the syntax consistent with property access.
5. English-like syntax and keyword
    - Core actions (e.g., throwBall, release, team, explore, run) are chosen to feel like Pokémon gameplay commands, making the language readable and fun.
6. Statements use semicolons
    - All statements end with ;. This makes it easier to track the end of line and to parse.
7. Minimalist control flow
    - Minimized to keep design simple and give the safari zone experience
8. Comments and readability
    - Fun comment syntax enhances engagement without affecting parsing.
9. Focus on clarity and accessibility
    - The syntax is designed to be easy to read for Pokémon fans, while also structured enough to be parsed like a real language.

