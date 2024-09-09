# Scotland Yard AI - MrX AI Implementation

Welcome to the **Scotland Yard AI** repository, an implementation of a custom AI for the game Scotland Yard. This project focuses on building an AI that plays the role of MrX, using advanced algorithms to outmaneuver detectives on the game board. Whether you are a technical recruiter or an HR professional, this readme aims to provide an overview of the project and its key highlights.

## Table of Contents

- [Project Overview](#project-overview)
- [Features](#features)
- [How It Works](#how-it-works)
  - [Game Algorithms](#game-algorithms)
  - [Code Structure](#code-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Running the AI](#running-the-ai)
- [Technologies Used](#technologies-used)
- [Contributions](#contributions)
- [License](#license)

---

## Project Overview

The Scotland Yard game is a turn-based strategy game where a group of detectives works to locate and apprehend MrX, who tries to evade capture. This repository houses an AI agent for **MrX**, which intelligently evaluates possible moves to stay undetected by detectives, applying advanced algorithms like **Breadth-First Search (BFS)** and **MiniMax** for decision-making.

This project demonstrates core competencies in:
- **Game theory and AI**: Implementing algorithms to anticipate and counter opponent moves.
- **Java development**: Applying Java’s robust libraries and design patterns.
- **Problem-solving**: Optimizing decision-making strategies under game constraints.

## Features

- **Breadth-First Search (BFS)**: Used to determine the shortest distance between MrX and the detectives.
- **MiniMax Algorithm**: Implements decision-making with an evaluation function to optimize MrX's moves, while considering possible detective actions.
- **Game State Evaluation**: A scoring function that assesses board states based on detective proximity and available moves for MrX.
- **AI Strategy Refinements**: Features like ticket management and path planning to reduce chances of capture.

## How It Works

### Game Algorithms

1. **Breadth-First Search (BFS)**: Detectives use BFS to calculate their proximity to MrX, ensuring a strategic approach to cornering him. This helps determine the risk of MrX’s movements and prioritizes high-risk moves.
   
2. **MiniMax Algorithm**: MrX’s AI uses MiniMax with a depth of 1 (due to the time-sensitive nature of real-time play). The AI evaluates moves based on a scoring function:
   - Positive scores for available escape routes.
   - Negative scores for proximity to detectives.

3. **Game State Evaluation**: The AI assesses the game state by:
   - Evaluating distances between MrX and detectives.
   - Checking available routes for MrX and determining high-risk areas.
   - Calculating potential threats by inspecting adjacent detective positions.

### Code Structure

- **MyAI.java**: The main AI logic is implemented here, including the MiniMax decision-making, BFS search, and move evaluations.
- **myGameState.java**: Models the game state, defining the available moves and ensuring compliance with Scotland Yard’s game rules.
- **bfsNode.java**: Helper class to represent nodes in the BFS tree traversal used by the detectives.
- **Main.java**: The entry point for launching the AI within the game’s user interface.

## Getting Started

### Prerequisites

To run the AI, you’ll need:

- **Java 8+**: The AI is developed in Java, so ensure you have a Java Development Kit (JDK) installed.
- **Maven**: This project uses Maven to manage dependencies and build the project.

### Running the AI

1. Clone the repository:
    ```bash
    git clone https://github.com/yourusername/scotland-yard-ai.git
    cd scotland-yard-ai
    ```

2. Build the project:
    ```bash
    ./mvnw clean install
    ```

3. Run the AI:
    ```bash
    java -cp target/your-built-jar-file.jar uk.ac.bris.cs.scotlandyard.ui.ai.Main
    ```

The AI will run and simulate a game where MrX is controlled by the AI agent.

## Technologies Used

- **Java**: The core programming language used for the AI implementation.
- **Maven**: Dependency management and project build automation.
- **Guava**: Used for Immutable collections and additional utilities.
- **Fugue (Atlassian)**: Functional programming utilities for handling complex logic.

By combining efficient algorithms with strategic game thinking, this project showcases my ability to build AI solutions that deal with real-time decisions, multi-agent environments, and optimization challenges.
