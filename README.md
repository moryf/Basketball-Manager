## BasketSim: A Tactical Basketball Simulation Game

BasketSim is an application developed as a school assignment for a Master's course at the University of Belgrade, Faculty of Organizational Sciences. It leverages the Clojure programming language to create a deep, statistically driven basketball simulation, allowing users to draft and manage their own teams.

# Who's it for

If you've ever felt that you could manage a basketball team, that's where BasketSim comes in. It's your chance to prove your basketball knowledge by drafting a team of real world players and managing them through a complete game simulation. You make the calls from the draft to the end of quarter adjustments that can turn the tide of the game and watch your strategic decisions play out, possession by possession, on the court.

# How the Engine Works
This section elaborates on the statistical models used by BasketSim, explaining the methodology behind how a game is simulated. The core idea is that every possession is a series of probabilistic events determined by the real world advanced stats of the players on the court. The engine layers these probabilities to create a dynamic and unpredictable gameplay experience.

Here's a breakdown of how a single possession is resolved:

* Finisher Selection: The simulation first decides which player will "finish" the possession. This doesn't just mean shooting, it's the player who will make the decisive action. This is determined by a random selection based on each player's Usage Rate (USG%). A player with a higher USG% is a dominant force on offense, so they are statistically more likely to be the one to end the play.

* Outcome Decision: Once a finisher is selected, the simulation doesn't assume they will shoot. A real possession can end in multiple ways. To model this, the engine makes another random choice based on two key stats that define a player's tendencies:

Turnover Percentage (TOV%): The probability that the player's aggressiveness or poor decision-making leads to a turnover, immediately ending the possession.

Free Throw Attempt Rate (FTr): The probability that the player's attack on the basket results in them drawing a shooting foul from the defense.

If neither of these outcomes is chosen, the possession proceeds to the most common outcome: a shot attempt. This creates a realistic flow where not every play results in a field goal attempt.

* The Defensive Model: If a shot is attempted, the simulation compare the matchup on the court. The shooter vs. the defender guarding them, the player at the same position on the opposing team

The shooter's Offensive Box Plus Minus (OBPM) is compared to the defender's Defensive Box Plus Minus (DBPM).

The difference between these two ratings creates a bonus or penalty, which is applied directly to the shooter's field goal percentage for that specific shot. A positive difference results in a higher chance to score, while a negative difference creates a tougher shot. This system ensures that a great offensive player will get a significant boost against a weak defender, and even the best shooters will struggle against a lockdown defender.

* The Fatigue Factor: Before the final shot probability is calculated, the shooter's current stamina is checked. Every player on the court expends energy on each possession.

As a player's stamina drops below certain thresholds (75%, 50%, 25%), a progressively larger penalty is applied to their shooting percentage.

This mechanic makes bench management critical. A tired player is significantly less likely to make a shot, forcing the manager to decide whether to push their star or rest them for crucial moments.

* Shot Resolution: The final, modified shot percentage is used to determine if the shot is a make or a miss. The outcome of the shot triggers further events:

On a Miss: A rebound is simulated. All 10 players on the court have a chance to grab the board, weighted by their individual Offensive (ORB%) or Defensive (DRB%) Rebound Percentages. This is a critical moment, as a defensive rebound results in a change of possession, while an offensive rebound gives the attacking team a precious second chance to score.

On a Make: An assist is simulated. This is a two-step process to model how assists truly work. First, the shooter's individual stats are checked to determine if their made basket was likely to be assisted. If it was, a final random selection is made among their four teammates based on their Assist Percentage (AST%) to see who gets credit for the pass.

This multi-layered approach ensures that each possession is a unique event, influenced by player tendencies, fatigue, and individual matchups.

# Technology & Architecture
BasketSim is built entirely in Clojure, embracing a functional programming paradigm to manage the complexity of the simulation.

Immutable State Management: The core of the application is a single, immutable game-state map. No data is ever changed in place. Instead, every action (a possession, a substitution) is a pure function that takes the current state as input and returns a new, updated state. This prevents a whole class of common bugs related to unexpected state changes, making the complex simulation logic surprisingly manageable and predictable. This is managed primarily through Clojure's powerful loop/recur construct, which provides efficient loops without mutable variables.

Data Driven Design: The application is designed around the transformation of data. It begins by loading a comprehensive player database from a CSV file using the clojure.data.csv library. This raw data is then transformed into a clean vector of player maps. Throughout the simulation, these maps are passed between functions, updated, and manipulated using Clojure's rich library of sequence functions (map, filter, reduce, comp, etc.). For example, a single line of code can map over a list of players, compose a function to extract a nested stat, and reduce the results to a team total.

* Separation of Concerns: The application is clearly divided into distinct phases, with each function having a single, clear responsibility.

* Data Loading: The load-players function is responsible for all file I/O and data parsing.

* Drafting: The run-draft function manages the entire interactive draft loop, a self-contained module that produces the final teams.

Gameplay: The run-game and run-quarter functions manage the high-level flow of the game, while the resolve- functions handle the specific, granular logic for each type of play (shots, fouls, turnovers). This separation makes the code easier to read, test, and extend.

# What can improve
Currenty, there are several aspects of the game tht could improve and make the game even more comprehensive and realistic.

* Substitution and Timeout system: Currently the players are only able to make subtitutions at the ends of quarters. If the game were to be more realistic, the players would need to be able to pause the game at any moment and make substitutions, and even call timeouts to rest their players and make any changes necessary.

* No And-1: The foul mechanic doesn't allow for players to be able to score and recieve a foul. So the result of a possesion is either a shot attempt or 2 free throws.

* Position penalties: The players maps don't include their positions on the court. So we can put Jokic at the point guard position and Curry at the Center and the game would have no issues with that. There would need to be a penalty for players playing out of position.

Closing Notes
Building BasketSim has been a fantastic experience and a great way to learn Clojure. Initially, the functional, immutable approach felt unfamiliar, but as the project grew in complexity, the benefits became incredibly clear. The ability to reason about the game's state without worrying about hidden side effects made debugging and adding new features (like the defensive model and fatigue system) a surprisingly smooth process.



Resources
Player statistics sourced from www.basketball-reference.com

This project was inspired by the book Clojure for the Brave and True.