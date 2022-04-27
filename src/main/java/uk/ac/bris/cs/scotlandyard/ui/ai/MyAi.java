package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.*;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import static com.google.common.primitives.Ints.max;
import static com.google.common.primitives.Ints.min;

@SuppressWarnings("UnstableApiUsage")
public class MyAi implements Ai {
	private Map<Move, Integer> moveEvalMap = new HashMap<>();

	@Nonnull @Override public String name() { return "MrX AI 1"; }

	public ImmutableMap<ScotlandYard.Ticket, Integer> getPieceTickets(Board board, Piece piece) {
		Board.TicketBoard ticketBoard = board.getPlayerTickets(piece).orElse(null);
		Map<ScotlandYard.Ticket, Integer> ticketsMap = new HashMap<>();
		List<ScotlandYard.Ticket> ticketTypes = List.of(
				ScotlandYard.Ticket.TAXI,
				ScotlandYard.Ticket.BUS,
				ScotlandYard.Ticket.UNDERGROUND,
				ScotlandYard.Ticket.SECRET,
				ScotlandYard.Ticket.DOUBLE);

		for (ScotlandYard.Ticket ticketType : ticketTypes) {
			assert ticketBoard != null;
			ticketsMap.put(ticketType, ticketBoard.getCount(ticketType));
		}

		return ImmutableMap.copyOf(ticketsMap);
	}

	public List<Player> boardPlayers(Board board, Integer mrXLocation) {
		List<Piece> gamePieces = board.getPlayers().asList();
		List<Player> Players = new ArrayList<>();

		int Location;
		for (Piece piece : gamePieces) {
			if (piece.isDetective()) {
				Location = board.getDetectiveLocation((Piece.Detective) piece).orElse(0);
			} else {
				Location = mrXLocation;
			}

			Players.add(new Player(piece, getPieceTickets(board, piece), Location));
		}
		return Players;
	}

	/**
	 * @param graph A ValueGraph with the nodes and the edge values between them.
	 * @param mrX MrX's Location
	 * @param detectives List of detectives
	 * @param bfsDepth The distance from MrX to check for a detective (Starting at mrX current location)_
	 * @return The shortest distance from a detective to MrX
	 * */
	public Integer BFS(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
					   Player mrX,
					   List<Player> detectives,
					   int bfsDepth) {
		Queue<bfsNode> nodeTreeQueue = new LinkedList<>();
		nodeTreeQueue.add(new bfsNode(mrX, mrX.location(), List.of(mrX.location()), 0));
		List<Integer> detectivesLocations = detectives.stream().map(Player::location).toList();

		while (!nodeTreeQueue.isEmpty()) {
			bfsNode currentNode = nodeTreeQueue.poll(); // Pick the next bfsNode from the queue

			/*
			 This is a Breadth-First-Search + We are utilizing queues.
			 This allows us to return the depth (distance) without checking the rest of the queue,
			 since its elements with greater depth are only processed after all the elements of the prior depth.
			 The returned depth is less than or equal to the rest of the elements in the queue.
			*/
			if (detectivesLocations.contains(currentNode.getNode())) {
				List<Integer> pathToMrX = new ArrayList<>(currentNode.getNodesTraversed());
				Collections.reverse(pathToMrX);
				Player closeDetective = detectives.stream().filter(detective -> detective.location() == currentNode.getNode()).toList().get(0);
				ImmutableSet<ScotlandYard.Transport> transportsSet1 = graph.edgeValue(closeDetective.location(), pathToMrX.get(1)).orElse(ImmutableSet.of());

				if (closeDetective.location() == mrX.location()) {
					return currentNode.getDepth();
				}

				for (ScotlandYard.Transport edge1:  transportsSet1) {
					// If detective has the required ticket for first move towards mrX
					if (closeDetective.has(edge1.requiredTicket())) {
						Player detDuplicate = closeDetective;
						detDuplicate = detDuplicate.at(pathToMrX.get(1));
						detDuplicate = detDuplicate.use(edge1.requiredTicket());
						if (detDuplicate.location() == mrX.location()) {
							return currentNode.getDepth();
						} else {
							ImmutableSet<ScotlandYard.Transport> transportsSet2 = graph.edgeValue(detDuplicate.location(), pathToMrX.get(2)).orElse(ImmutableSet.of());
							for (ScotlandYard.Transport edge2 : transportsSet2) {
								if (detDuplicate.has(edge2.requiredTicket())) {
									detDuplicate = detDuplicate.at(pathToMrX.get(2));
									detDuplicate = detDuplicate.use(edge2.requiredTicket());
									if (detDuplicate.location() == mrX.location()) {
										return currentNode.getDepth();
									}
								}
							}
						}
					}
				}
			}

			// Check that current is less than desired depth to not surpass the assigned depth limit
			else if (currentNode.getDepth() < bfsDepth) {
				List<Integer> adjNodes = new ArrayList<>(graph.adjacentNodes(currentNode.getNode()).stream().toList());
				adjNodes.removeAll(currentNode.getNodesTraversed()); // Remove the nodes used to reach current position to disallow going backwards.

				// Add adjacent nodes to the queue
				for (Integer adjNode : adjNodes) {
					List<Integer> nodePath = new ArrayList<>(List.copyOf(currentNode.getNodesTraversed()));
					nodePath.add(adjNode);
					bfsNode node = new bfsNode(currentNode.getExplorer(), adjNode, nodePath,currentNode.getDepth() + 1);
					nodeTreeQueue.add(node);
				}
			}
		}
		return (bfsDepth);
	}

	/**
	 * Evaluate the given gameState using:<br>
	 * 1) the Breadth-First-Search (BFS) algorithm to check the proximity of detectives to MrX.<br>
	 * 2) The nodes adjacent to MrX. <br>
	 * @param gameState The gameState to be evaluated.
	 * @return an Integer representing the gameState score.
	 */
	public Integer evalState(myGameState gameState) {
		int score = 0;
		int mrXLocation = gameState.getMrX().location();
		int distanceToMrX = BFS(gameState.getSetup().graph, gameState.getMrX(), gameState.getDetectives(), 2);
		List<Integer> detectivesLocations = gameState.getDetectives().stream().map(Player::location).toList();
		List<Integer> adjacentNodes = new ArrayList<>(gameState.getSetup().graph.adjacentNodes(mrXLocation));

		// Decrease score by 10 for every adjacent detective adjacent to MrX
		for (Integer detLocation : detectivesLocations) {
			if (adjacentNodes.contains(detLocation)) {
				score -= 10;
			}
		}
		adjacentNodes.removeAll(detectivesLocations); // Remove nodes occupied by detectives
		score += adjacentNodes.size(); 				  // Add 1 to the score per empty node surrounding MrX
		score += (-1000 + (distanceToMrX * 250)); 	  // Scoring the gameState based on the closest distance from MrX to a detective
		return score;
	}

	public Integer miniMax(myGameState gameState, Integer depth, boolean maximizeMrX, final List<Move> moves, List<Player> remainingDetectives) {
		if (depth == 0) {
			return evalState(gameState);
		}

		// MrX moves
		if (maximizeMrX) {
			int maxEval = -1000000;
			List<Move> movesList = moves; // Create a copy of the list of moves so that the original list is unaltered

			// Remove Secret & Double Moves if no detective is at most {bfsDepth} moves away
			int bfsDepth = 2;
			Integer distanceToMrX = BFS(gameState.getSetup().graph, gameState.getMrX(), gameState.getDetectives(), bfsDepth);
			if (distanceToMrX >= bfsDepth + 1) {
				movesList = movesList.parallelStream()
						.filter(move -> !((List<ScotlandYard.Ticket>) move.tickets()).contains(ScotlandYard.Ticket.DOUBLE)
								&& ((List<ScotlandYard.Ticket>) move.tickets()).contains(ScotlandYard.Ticket.SECRET))
						.toList();
			}

			// Evaluate mrX's moves and update moveEvalMap
			for (Move move : movesList) {
				myGameState childState = gameState.advance(move);
				int eval = miniMax(childState, depth - 1, false, childState.getAvailableMoves().asList(), childState.getRemainingPlayers());
				maxEval = max(maxEval, eval);
				// Reducing Double moves' score so that single ticket moves that result in the same destination or an equally good destination are preferred
				if (((List<ScotlandYard.Ticket>) move.tickets()).stream().anyMatch(ticket -> (ticket == ScotlandYard.Ticket.DOUBLE) || (ticket == ScotlandYard.Ticket.SECRET))) {
					eval -= 5;
				}
				this.moveEvalMap.put(move, eval); // Add the move and its evaluation to the map
			}

			return maxEval;
		}
		else { // Detectives move
			AtomicInteger minEval = new AtomicInteger(1000000); // We're using AtomicIntegers for threading

			remainingDetectives.parallelStream().forEach(detective -> {
				List<Move> detectiveMoves = moves.stream()
						.filter(move -> move.commencedBy() == detective.piece())
						.toList();

				for (Move move : detectiveMoves) {
					if (minEval.intValue() <= -740) {
						break;
					}
					myGameState childState = gameState.advance(move);
					int eval;
					if (childState.getRemainingPlayers().contains(childState.getMrX())) {
						eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList(), childState.getRemainingPlayers());
					} else {
						eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList(), childState.getRemainingPlayers());
					}
					minEval.set(min(minEval.intValue(), eval));
				}
			});

			return minEval.intValue();
		}
	}

	@Nonnull
	@Override
	public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		// Initializing Variables (Updated every MrX turn)
		ImmutableList<Move> moves = board.getAvailableMoves().asList();
		List<Player> players = boardPlayers(board, moves.get(0).source());
		Player mrX = players.get(0);
		List<Player> detectives = players.subList(1, players.size());
		this.moveEvalMap = new HashMap<>();

		// Create a gameState of the current board
		myGameState currentState = new myGameState(board.getSetup(), ImmutableSet.of(mrX.piece()), board.getMrXTravelLog(), mrX, detectives);

		// Calling minimax updates the moveEvalMap Map as a side effect
		miniMax(currentState, 1, true, moves, currentState.getDetectives());

		// Pick the move with the highest score
		Map.Entry<Move, Integer> bestMove = null;
		for (Map.Entry<Move, Integer> entry : this.moveEvalMap.entrySet()) {
			if ((bestMove == null) || (entry.getValue().compareTo(bestMove.getValue()) > 0)) {
				bestMove = entry;
			}
		}

		assert bestMove != null;
		return bestMove.getKey();
	}
}
