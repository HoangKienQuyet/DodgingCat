Dodging Cat
A fast-paced 2D Platformer survival game developed entirely in pure Java (Java Swing and Graphics2D). The game applies an Object-Oriented Programming (OOP) architecture, separating features into independent modules, and supports both Single Player (1 Player) and Co-op (2 Players) modes on the same machine.

Key Features
Diverse Game Modes: Supports 1 Player and 2 Players (Co-op). In 2-player mode, if either player touches a hazard, it triggers a Game Over.

Physics System: Smoothly tuned movement, gravity, and jump forces providing solid and precise control mechanics.

Extended Graphical User Interface (GUI): Uses CardLayout for seamless transitions between the Main Menu, Map Selection, and Gameplay screens. Supports automatic frame scaling without distorting graphics or misaligning mouse coordinates when resizing the window.

4 Exclusive Maps:

Green Forest

Desert Dunes

Graveyard

Snowy Winter
(Each map features unique backgrounds, platform designs, and hazards).

Dynamic Hazard Mechanics: Dangers spawn randomly vertically (falling) or horizontally (flying across), accompanied by an early warning system (!). Includes a special hazard (Rotating Weight) with a limited lifespan.

Progressive Difficulty System: The game automatically increases hazard speed and reduces the spawn interval every 5 seconds, accompanied by a "SPEED UP!" notification.

Score & Collectibles System: Collect Fish to increase your score. The fish will automatically respawn on a random platform approximately 1 second after being collected.
ControlsActionPlayer 1 (Gray Cat)Player 2 (Orange Dog)Move LeftALeft ArrowMove RightDRight ArrowJumpWUp ArrowPause: Press the ESC key during gameplay to open the Pause Menu (Resume, Restart, Main Menu, Quit).Project StructureThe project is modularized following OOP principles for easy management and scalability:PlaintextDodgingCat/
├── assests/ # Directory containing all image resources (sprites, backgrounds, ui...)
│ ├── backgrounds/
│ ├── hazards/
│ ├── platforms/
│ ├── player/
│ │ ├── cat1/ # Movement sprites for Player 1
│ │ └── cat2/ # Movement sprites for Player 2
│ └── ui/ # Background image for the UI (ui.png)
└── src/ # Directory containing Java source code
├── AssetLoader.java # Utility class to load images from the local drive
├── DodgingCatApp.java # Main class, configures the JFrame window and CardLayout
├── Fish.java # Fish collectible logic and respawn system
├── GamePanel.java # Main Game Loop, Graphics2D rendering, and collision handling
├── GameState.java # Enum declaration for game states (MENU, PLAYING, PAUSED, GAMEOVER...)
├── Hazard.java # Hazard object, manages warnings and flight/fall trajectories
├── MainMenuPanel.java # Initial startup UI screen design
├── MapConfig.java # Data structure storing configurations and assets for each map
├── Platform.java # Renders and manages coordinates for platforms and the ground
└── Player.java # Loads character sprites, handles physics (jumping, falling), and animation frames
Installation & ExecutionEnsure that the Java Development Kit (JDK 8 or higher) is installed on your machine.Clone or download the source code.Make sure the assests directory is at the same level as your working directory when running the application to prevent image loading errors.Open the project in an IDE such as IntelliJ IDEA, Eclipse, or Visual Studio Code.Compile and run the DodgingCatApp.java file.
