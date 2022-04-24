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

		/*for (Player detective : detectives) {
			nodeTreeQueue.add(new bfsNode(detective, detective.location(), List.of(), 0));
		}*/

		while (!nodeTreeQueue.isEmpty()) {
			// Pick a bfsNode form the queue
			bfsNode currentNode = nodeTreeQueue.poll();

			// If node it contains a detective
			if (detectivesLocations.contains(currentNode.getNode())) {
				/*
				 This is a Breadth-First-Search + We are utilizing queues.
				 This allows us to return the depth (distance) without checking the rest of the queue,
				 since its elements with greater depth are only processed after all the elements of the prior depth.
				 The returned value is less than or equal to the rest of the elements in the queue.
				*/

				List<Integer> pathToMrX = new ArrayList<>(currentNode.getNodesTraversed());
				//pathToMrX.remove(pathToMrX.get(pathToMrX.size() - 1));
				Collections.reverse(pathToMrX);
				Player closeDetective = detectives.stream().filter(detective -> detective.location() == currentNode.getNode()).toList().get(0);

				if (closeDetective.location() == mrX.location()) {
					return currentNode.getDepth();
				}
				ImmutableSet<ScotlandYard.Transport> transportsSet1 = graph.edgeValue(closeDetective.location(), pathToMrX.get(1)).orElse(ImmutableSet.of());

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

			// Check that depth of current node is less than desired depth
			else if (currentNode.getDepth() < bfsDepth) {
				List<Integer> adjNodes = new ArrayList<>(graph.adjacentNodes(currentNode.getNode()).stream().toList());
				adjNodes.removeAll(currentNode.getNodesTraversed());
				List<bfsNode> nextQueue = new ArrayList<>();

				for (Integer adjNode : adjNodes) {
					List<Integer> nodePath = new ArrayList<>(List.copyOf(currentNode.getNodesTraversed()));
					nodePath.add(adjNode);
					bfsNode node = new bfsNode(currentNode.getExplorer(), adjNode, nodePath,currentNode.getDepth() + 1);
					nextQueue.add(node);

					// Ticket(s) required to reach adjNode
					/*ImmutableSet<ScotlandYard.Transport> transportsSet = graph.edgeValue(currentNode.getNode(), adjNode).orElse(ImmutableSet.of());
					for (ScotlandYard.Transport t:  transportsSet) {
						// If detective has the required ticket to reach adjNode, add it to queue
						if (currentNode.getExplorer().has(t.requiredTicket())) {
							List<Integer> nodePath = new ArrayList<>(List.copyOf(currentNode.getNodesTraversed()));
							nodePath.add(currentNode.getNode());
							bfsNode node = new bfsNode(currentNode.updateExplorerTickets(t.requiredTicket()), adjNode, nodePath,currentNode.getDepth() + 1);
							nextQueue.add(node);
						}
					}*/
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
	 * 1) the Breadth-First-Search (BFS) algorithm to check the proximity of detectives to MrX.<br>
	 * 2) The empty nodes adjacent to MrX. <br>
	 * @param gameState The gameState to be evaluated.
	 * @return Returns an Integer representing the evaluation of the gameState.
	 */
	public Integer evalState(myGameState gameState) {
		int score = 0;
		int mrXLocation = gameState.getMrX().location();
		int distanceToMrX = BFS(gameState.getSetup().graph, gameState.getMrX(), gameState.getDetectives(), 1);
		List<Integer> detectivesLocations = gameState.getDetectives().stream().map(Player::location).toList();
		List<Integer> adjacentNodes = new ArrayList<>(gameState.getSetup().graph.adjacentNodes(mrXLocation));
		//System.out.println("MRX: " + mrXLocation);

		// Decrease score by 10 for every adjacent detective adjacent to MrX
		// for cases when all moves are 1 move away from detectives but one
		// move might only be close to 1 det instead of two
		for (Integer detLocation : detectivesLocations) {
			if (adjacentNodes.contains(detLocation)) {
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
			Integer distanceToMrX = BFS(gameState.getSetup().graph, gameState.getMrX(), gameState.getDetectives(), bfsDepth);
			System.out.println(distanceToMrX);
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
				// Subtracting one so that single ticket moves that result in the same destination are preferred
				if (((List<ScotlandYard.Ticket>) move.tickets()).contains(ScotlandYard.Ticket.DOUBLE)) {
					eval -= 1;
				}
				// We don't add moves with an evaluation that's less than any of the previous moves' evaluations.
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
