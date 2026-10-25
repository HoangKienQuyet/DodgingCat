# Dodging Cat

Dodging Cat is a single-screen JavaFX platform survival game. The player jumps between household furniture, collects food to build a high score, and avoids household hazards that become faster and more frequent as a run continues.

The game adapts the broad play loop of a high-score platform survival game while keeping its own cat character, household setting, Java implementation, and programmatically drawn artwork. It does not reuse Poor Bunny artwork or characters.

## Problem and objectives

Endless survival games need responsive movement, readable hazards, and a scoring goal that rewards player decisions. Dodging Cat explores those requirements in a small desktop game that also demonstrates Java object-oriented design, JavaFX event handling, animation, and testable game rules.

Project objectives:

- Provide a repeatable menu, gameplay, and game-over flow.
- Implement bounded left/right movement, jumping, gravity, and one-way platform landing.
- Let players move between three reachable furniture ledges and the floor.
- Reward collected normal and golden food while using survival time to scale difficulty.
- Spawn five distinct moving household hazards with bounded active object counts.
- Keep movement, collision, spawning, score, and difficulty rules testable without starting JavaFX.

## Gameplay

- Main menu with Start Game and Exit.
- Controls: A/D or Left/Right to move; Space, W, or Up to jump; Esc to pause/resume.
- The cat can jump from the floor and from furniture ledges. Platforms are one-way surfaces: the cat lands on them while descending and can jump through them from below.
- Normal food is worth 10 points; rare golden food is worth 50. Score comes from food collection, while survival time is displayed separately.
- The best score is shown on the menu, during play, and after a run. New records are saved to `.dodging-cat/highscore.txt` inside the user's home directory.
- Exactly one normal food item is active. Collecting it awards 10 points and immediately places one replacement on a reachable floor or platform position.
- Golden food is independent: it can appear as a temporary 50-point bonus, expires after seven seconds if uncollected, and never creates extra normal food.
- Falling flower pots, paper planes, swinging yo-yos, robot vacuums, and rolling balls are hazards. A collision ends the run.
- New hazards enter from outside the room. Falling pots keep a horizontal clearance from the cat at spawn time, and entrance overlap with an active hazard is rejected.
- Difficulty increases every 25 seconds. Hazard spawn intervals decrease from 2.6 seconds to a 0.9 second minimum; movement speed increases to a maximum 1.85 multiplier.
- At most five food items and five hazards can be active at once. This keeps ignored food and persistent hazards from growing the game collections without bound.
- Game Over displays the final food score and offers Play Again or Main Menu.
- Pause stops the active animation loop and exposes Resume and Main Menu actions without resetting the run.
- The room, cat, food, furniture, and hazards are original JavaFX Canvas drawings. The reusable navy/cream UI theme is packaged at `src/main/resources/com/dodgingcat/ui/dodging-cat.css`; no external artwork is required.
- Cat Style Shop, High Score, and Settings open styled informational screens until their dedicated milestones implement those features.

## Technology and requirements

- Java 25
- JavaFX 25
- Maven
- JUnit Jupiter 5

Build and run:

```shell
mvn clean test
mvn javafx:run
```

## Architecture

```text
src/
|-- main/java/com/dodgingcat/
|   |-- Main / Launcher
|   |-- game: GameManager, GameState, GameEngine, GameLoop, InputController
|   |-- entity: GameObject, Cat, Platform, Food types, Obstacle types
|   |-- io: HighScoreRepository
|   |-- system: CollisionDetector, ScoreManager, DifficultyManager,
|   |           PlatformLayout, SpawnManager
|   |-- ui: MainMenu, GameView, PauseView, GameOverView, FeatureInfoView,
|   |         UiKit, CozyArtwork, GameCanvasRenderer
|   `-- util: Constants
|-- main/resources/com/dodgingcat/ui/dodging-cat.css
`-- test/java/com/dodgingcat/: game and system unit tests
```

`GameManager` changes the application between MENU, PLAYING, PAUSED, and GAME_OVER. `GameLoop` uses JavaFX `AnimationTimer` to update the model and redraw the view. `GameEngine` coordinates a run. Player physics are in `Cat`; static furniture surfaces are described by `Platform` and arranged by `PlatformLayout`. `SpawnManager` creates food and hazards with an injected random generator. `GameCanvasRenderer` presents the game model on Canvas, while `UiKit` and the CSS stylesheet provide a shared presentation system for every screen.

## Object-oriented design

- `GameObject` provides shared rectangular bounds for the cat, food, and hazards. `CollisionDetector` checks those bounds once for the game systems.
- `Food` and `Obstacle` are abstractions. Normal and golden food return different point values; obstacle subtypes have different movement behavior and are updated through the shared `Obstacle` API.
- `Cat` owns player movement and landing physics. `GameView` owns drawing and HUD text. `ScoreManager` tracks collected-food points, and `DifficultyManager` derives pacing from elapsed time.
- `SpawnManager` accepts `RandomGenerator`, so tests can use seeded randomness.
- `HighScoreRepository` isolates local file access from game and presentation code. Missing or invalid files default safely to zero; save failures are logged without interrupting gameplay.
- The platform layout, active entity lists, and game loop are composed in `GameEngine`; game state transitions remain in `GameManager`.

The code reflects focused responsibilities and subtype-based extension. New food or obstacle types can share the current model abstractions, although spawning and JavaFX drawing still need registration for each concrete type. The current state coordinator is enum-based; the project does not claim a full State, Strategy, or Observer pattern implementation.

## Tests and verification

Run automated tests with `mvn clean test`. The current JUnit suite has 28 tests covering:

- Food-only scoring and rectangle collision.
- Cat bounds, jumping, floor landing, and furniture landing.
- Difficulty milestones and spawn timing.
- Food placement on reachable surfaces and active spawn limits.
- Game-engine movement, food collection, platform layout, one-life game-over behavior, and hazard collision.
- Paper Plane movement, Yoyo oscillation bounds, and safe falling-pot spawn clearance.
- Single-active normal-food replacement, one-time score awards, golden-food expiry, and valid food spawn placement.
- High-score save/load, missing/corrupt-file fallback, and invalid save input.

These are unit tests of the game model. They do not replace manual checking of keyboard input and screen transitions. No performance benchmark or long-session memory measurement is currently reported.

## Limitations and future work

- Sound, multiplayer, and unlockable cosmetics are not implemented.
- Spawn clearance prevents immediate unfair collisions, but fairness and balance still need hands-on playtesting.
- The survival arena uses three fixed furniture platforms; procedural platform layouts and multiple rooms are not implemented.
- Performance metrics and a full manual system-test record remain future work.

Possible next steps are playtesting and difficulty tuning, and documenting manual menu-to-game-over test results.

## Reference

- Adventure Islands, *Poor Bunny!* Google Play listing, describing a high-score platform game based on collecting food and avoiding traps: [play.google.com/store/apps/details?id=com.adventureislands.poorbunny](https://play.google.com/store/apps/details?id=com.adventureislands.poorbunny).
