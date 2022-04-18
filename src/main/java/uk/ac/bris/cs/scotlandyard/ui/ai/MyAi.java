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
	private List<Player> Players;
	private Player MrX;
	private List<Player> Detectives;
	private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> Graph;
	private Integer count;

	@Nonnull @Override public String name() { return "newAI!"; }

	public ImmutableMap<ScotlandYard.Ticket, Integer> getPieceTickets(Board board, Piece piece) {
		Board.TicketBoard ticketBoard = board.getPlayerTickets(piece).orElse(null);
		Map<ScotlandYard.Ticket, Integer> modifiableMap = new HashMap<>();
		List<ScotlandYard.Ticket> ticketTypes = List.of(ScotlandYard.Ticket.TAXI,
				ScotlandYard.Ticket.BUS,
				ScotlandYard.Ticket.UNDERGROUND,
				ScotlandYard.Ticket.SECRET,
				ScotlandYard.Ticket.DOUBLE);

		for (ScotlandYard.Ticket ticketType : ticketTypes) {
			modifiableMap.put(ticketType, ticketBoard.getCount(ticketType));
		}

		return ImmutableMap.copyOf(modifiableMap);
	}

	public List<Player> boardPlayers(Board board, Integer mrXLocation) {
		List<Piece> gamePieces = board.getPlayers().asList();
		List<Player> Players = new ArrayList<>();

		for (Piece piece : gamePieces) {
			int Location;
			if (piece.isDetective()) { Location = board.getDetectiveLocation((Piece.Detective) piece).orElse(0); }
			else { Location = mrXLocation; }

			Players.add(new Player(piece, getPieceTickets(board, piece), Location));
		}
		return Players;
	}

	/**
	 * @param graph A ValueGraph with the nodes and the edge values between them.
	 * @param detectives List of detectives
	 * @param target MrX's Location
	 * @param bfsDepth The number of moves to search for mrX from each detective
	 * @return The shortest distance from a detective to MrX
	 * */
	public Integer BFS(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
					   List<Player> detectives,
					   int target,
					   int bfsDepth) {

		Queue<bfsNode> nodeTreeQueue = new LinkedList<>();
		for (Player detective : detectives) {
			nodeTreeQueue.add(new bfsNode(detective, detective.location(), List.of(), 0));
		}

		while (!nodeTreeQueue.isEmpty()) {
			// Pick a bfsNode form the queue
			bfsNode currentNode = nodeTreeQueue.poll();

			// Mark bfsNode as explored and check if it contains mrX
			if (currentNode.exploreNode(target)) {
				/*
				 This is a Breadth-First-Search + We are utilizing queues.
				 This allows us to return the depth without checking the rest of the queue,
				 since its elements with greater depth are only processed after all the elements of the prior depth.
				 The returned value x <= the rest of the elements in the queue.
				*/
				return currentNode.getDepth();
			}

			// Check that depth of current node is less than desired depth
			else if (currentNode.getDepth() < bfsDepth) {
				List<Integer> adjNodes = new ArrayList<>(graph.adjacentNodes(currentNode.getNode()).stream().toList());
				adjNodes.removeAll(currentNode.getNodesTraversed());
				List<bfsNode> nextQueue = new ArrayList<>();

				for (Integer adjNode : adjNodes) {
					// Ticket(s) required to reach adjNode
					ImmutableSet<ScotlandYard.Transport> transportsSet = graph.edgeValue(currentNode.getNode(), adjNode).orElse(ImmutableSet.of());
					for (ScotlandYard.Transport t:  transportsSet) {
						// If detective has the required ticket to reach adjNode, add it to queue
						if (currentNode.getExplorer().has(t.requiredTicket())) {
							List<Integer> nodePath = new ArrayList<>(List.copyOf(currentNode.getNodesTraversed()));
							nodePath.add(currentNode.getNode());
							bfsNode node = new bfsNode(currentNode.updateExplorerTickets(t.requiredTicket()), adjNode, nodePath,currentNode.getDepth() + 1);
							nextQueue.add(node);
						}
					}
				}
				nodeTreeQueue.addAll(nextQueue);
			}
		}

		return (bfsDepth + 1);
/*		if (distancesToMrX.isEmpty()) {
			return (bfsDepth + 1);
		} else {
			System.out.println(distancesToMrX);
			return Collections.min(distancesToMrX); // return closest distance to MrX
		}*/
	}

	/**
	 * Evaluate the passed gameState using:<br>
	 * 1) the Breadth-First-Search (BFS) algorithm to check the distances from detectives to MrX.<br>
	 * 2) The empty nodes adjacent to MrX. <br>
	 * @param gameState The gameState to be evaluated.
	 * @return Returns an Integer representing the evaluation of the gameState.<br>
	 */
	public Integer evalState(myGameState gameState) {
		int score = 0;
		int mrXLocation = gameState.getMrX().location();
		int distanceToMrX = BFS(gameState.getSetup().graph, gameState.getDetectives(), mrXLocation, 2);
		List<Integer> detectivesLocations = gameState.getDetectives().stream().map(Player::location).toList();
		List<Integer> adjacentNodes = new ArrayList<>(gameState.getSetup().graph.adjacentNodes(mrXLocation));
		//System.out.println("MRX: " + mrXLocation);

		// Decrease score by 10 for every detective adjacent to MrX
		for (Integer detLocations : detectivesLocations) {
			if (adjacentNodes.contains(detLocations)) {
				score -= 10;
			}
		}
		adjacentNodes.removeAll(detectivesLocations); // Remove nodes occupied by detectives
		score += adjacentNodes.size(); 				  // Add 1 to the score per empty node surrounding MrX
		score += (-1000 + (distanceToMrX * 250)); 	  // Scoring the gameState based on the closest distance from MrX to a detective

		return score;
		/*
		// Score based on detectives close by
		for (Integer node : oneMoveNodes) {
			//for dets
			if (detectivesLocations.stream().anyMatch(location -> location.intValue() == mrXLocation)) {
				score -= 10000000;
			}

			// Check if Detectives is 1 node away
			if (detectivesLocations.stream().anyMatch(location -> location.intValue() == node)) {
				System.out.println("IN -1000");
				score -= 1000;
			}
			else {
				score += 1;
			}
		}*/
	}

	public Integer miniMax(myGameState gameState, Integer depth, boolean maximizeMrX, final List<Move> moves, List<Player> remainingDetectives) {
		if ((depth == 0) || !(gameState.getWinner().isEmpty())) {
			this.count += 1;
			return evalState(gameState);
		}

		// MrX moves
		if (maximizeMrX) {
			int maxEval = -1000000;
			List<Move> movesList = moves; // Create a copy of the list of moves so that the original list is unaltered

			// Remove Double Moves if no detective is at most {bfsDepth} moves away
			int bfsDepth = 2;
			Integer distanceToMrX = BFS(gameState.getSetup().graph, gameState.getDetectives(), gameState.getMrX().location(), bfsDepth);
			if (distanceToMrX >= bfsDepth + 1) {
				movesList = movesList.parallelStream().filter(x -> !((List<ScotlandYard.Ticket>) x.tickets()).contains(ScotlandYard.Ticket.DOUBLE)).toList();
			}
			System.out.println(movesList);

			/*
			movesList.parallelStream().forEach(move -> {
				myGameState childState = gameState.advance(move);
				int eval = miniMax(childState, depth - 1, false, childState.getAvailableMoves().asList(), childState.getDetectives());
				maxEval.set(max(maxEval.get(), eval));
				this.moveEvalMap.put(move, eval);
			});*/

			for (Move move : movesList) {
				myGameState childState = gameState.advance(move);
				int eval = miniMax(childState, depth - 1, false, childState.getAvailableMoves().asList(), childState.getDetectives());
				maxEval = max(maxEval, eval);
				if (maxEval == eval) {
					this.moveEvalMap.put(move, eval);
				}
			}
			return maxEval;
		}
		// Detectives move
		else {
			AtomicInteger minEval = new AtomicInteger(1000000);

			remainingDetectives.parallelStream().forEach(detective -> {
				List<Move> detectiveMoves = moves.stream()
						.filter(move -> move.commencedBy() == detective.piece())
						.toList();

				/*detectiveMoves.parallelStream().forEach(move -> {
					myGameState childState = gameState.advance(move);
					int eval;
					if (childState.getRemaining().contains(childState.getMrX().piece())) {
						eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList(), childState.getDetectives());
					} else {
						eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList(), childState.getDetectives());
					}
					minEval.set(min(minEval.get(), eval));
				});*/
				int currentEval = 0;
				for (Move move : detectiveMoves) {
					if (currentEval <= -745) {
						break;
					}
					myGameState childState = gameState.advance(move);
					int eval;
					if (childState.getRemaining().contains(childState.getMrX().piece())) {
						eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList(), childState.getDetectives());
					} else {
						eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList(), childState.getDetectives());
					}
					minEval.set(min(minEval.get(), eval));
					currentEval = minEval.intValue();
				}
			});

			/*
			for (Move move : moves) {
				myGameState childState = gameState.advance(move);
				int eval;
				if (childState.getRemaining().contains(childState.getMrX().piece())) {
					eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList(), childState.getDetectives(), alpha, beta);
				} else {
					eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList(), childState.getDetectives(), alpha, beta);
				}
				minEval.set(min(minEval.get(), eval));
				alpha = max(alpha, eval);
				if (beta <= alpha) {
					break;
				}
			}
			*/
			return minEval.get();
		}
	}

	@Nonnull
	@Override
	public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// Initializing Variables (Updated every MrX turn)
		ImmutableList<Move> moves = board.getAvailableMoves().asList();
		this.Players = boardPlayers(board, moves.get(0).source());
		this.MrX = this.Players.get(0);
		this.Detectives = this.Players.subList(1, this.Players.size());
		this.Graph = board.getSetup().graph;
		this.moveEvalMap = new HashMap<>();
		this.count = 0;

		// Create a duplicate gameState of the current board
		myGameState currentState = new myGameState(board.getSetup(), ImmutableSet.of(this.MrX.piece()), board.getMrXTravelLog(), this.MrX, this.Detectives);

		miniMax(currentState, 2, true, moves, currentState.getDetectives());

		// Pick the move with the highest score
		Map.Entry<Move, Integer> bestMove = null;
		for (Map.Entry<Move, Integer> entry : this.moveEvalMap.entrySet()) {
			if ((bestMove == null) || (entry.getValue().compareTo(bestMove.getValue()) > 0)) {
				bestMove = entry;
			}
		}

		System.out.println(this.moveEvalMap.entrySet());
		System.out.println(bestMove.getKey());
		System.out.println(this.count);

		return bestMove.getKey();
	}
}
