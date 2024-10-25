# Scotland Yard Game - University of Bristol Coursework

This repository contains my implementation of the *Scotland Yard* board game in Java, developed as part of my University of Bristol coursework. The primary structure of the game was laid out by UoB, and my main task was to develop the `GameStateFactory` to manage and update game states efficiently. The project emphasizes the use of  design patterns, such as the *Observer* and *Visitor* patterns, to handle real-time game interactions and state updates.

## Features
- **GameStateFactory**: Manages the state of the game, handling player moves and transitions as the game progresses.
- **Observer Design Pattern**: Enables real-time updates to various components, ensuring synchronization across the game board and player positions.
- **Visitor Design Pattern**: Supports different actions for each player by enabling specific logic within `GameStateFactory`, which applies the appropriate updates based on the context.
- **Object-Oriented Design**: Uses Java’s OOP principles to structure and modularize code for reusability.

## Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/AravRaja/ScotlandYardFinal.git
   cd ScotlandYardFinal```markdown
2. **Compile and run the game (requires Java 8 or above):**
   ```bash
   javac -d bin src/*.java
   java -cp bin Main```markdown
## Gameplay Instructions
1. **Choose Your Role**: Play as either a detective or *Mr. X*.
2. **Game Objective**:
   - Detectives aim to capture *Mr. X* within a set number of moves.
   - *Mr. X* seeks to evade capture by navigating strategically around the board.
3. **Turn-Based Gameplay**: Each player moves based on  tickets, and the game board updates according to player movements.
4. **Game State Updates**: `GameStateFactory`  tracks and updates the game state with every move, handling changes in player positions and available actions.
