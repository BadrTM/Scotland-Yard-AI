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

	public Integer BFS(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
					   List<Player> detectives,
					   int target,
					   int bfsDepth) {

		Queue<bfsNode> nodeTreeQueue = new LinkedList<>();
		List<Integer> distancesToMrX = new ArrayList<>();
		for (Player detective : detectives) {
			nodeTreeQueue.add(new bfsNode(detective, detective.location(), List.of(), 0));
		}

		while (!nodeTreeQueue.isEmpty()) {
			// Pick a bfsNode form the queue
			bfsNode currentNode = nodeTreeQueue.poll();

			// Mark bfsNode as explored and check if it contains mrX
			if (currentNode.exploreNode(target)) {
				distancesToMrX.add(currentNode.getDepth());
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

		if (distancesToMrX.isEmpty()) {
			return (bfsDepth + 1);
		} else {
			return Collections.min(distancesToMrX); // return closest distance to MrX
		}
	}

	public Integer evalState(myGameState gameState) {

		int mrXLocation = gameState.getMrX().location();
		List<Integer> detectivesLocations = gameState.getDetectives().stream().map(Player::location).toList();
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = gameState.getSetup().graph;
		List<Integer> adjacentNodes = new ArrayList<>(gameState.getSetup().graph.adjacentNodes(mrXLocation));
		adjacentNodes.removeAll(detectivesLocations);
		// Set<Integer> oneMoveNodes = this.Graph.adjacentNodes(mrXLocation);

		System.out.println("MRX: " + mrXLocation + " | DetsLocation: " + detectivesLocations);
		// System.out.println(oneMoveNodes);

		// if (detectivesLocations.contains(mrXLocation)){
		// 	return -10000;
		// }

		int depth = 2;
		int distanceToMrX = BFS(graph, gameState.getDetectives(), mrXLocation, depth);

		int score = (-1000 + (distanceToMrX * 250));
		score += adjacentNodes.size();

		return score;
		// Board score increased based on available routes
		//score += gameState.getSetup().graph.adjacentNodes(mrXLocation).size();

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
		//return score;
	}

	public Integer miniMax(myGameState gameState, Integer depth, boolean maximizeMrX, final List<Move> moves, List<Player> remainingDetectives) {
		if ((depth == 0) || !(gameState.getWinner().isEmpty())) {
			this.count += 1;
			return evalState(gameState);
		}

		// MrX moves
		if (maximizeMrX) {
			int maxEval = -1000000;

			// Remove Double Moves if no detective is at most {bfsDepth} moves away
			int bfsDepth = 2;
			Integer distanceToMrX = BFS(gameState.getSetup().graph, gameState.getDetectives(), gameState.getMrX().location(), bfsDepth);

			List<Move> movesList = moves;
			if (distanceToMrX >= bfsDepth + 1) {
				movesList = movesList.parallelStream().filter(x -> !((List<ScotlandYard.Ticket>) x.tickets()).contains(ScotlandYard.Ticket.DOUBLE)).toList();
			}
			System.out.println(movesList);

			for (Move move : movesList) {
				myGameState childState = gameState.advance(move);
				int eval = miniMax(childState, depth - 1, false, childState.getAvailableMoves().asList(), childState.getDetectives());
				maxEval = max(maxEval, eval);
				this.moveEvalMap.put(move, eval);
			}
			return maxEval;
		}
		// Detectives move
		else {
			AtomicInteger minEval = new AtomicInteger(1000000);
			//moves.stream().forEach();
			remainingDetectives.parallelStream().forEach(detective -> {
				List<Move> detectiveMoves = moves.stream().filter(move -> move.commencedBy() == detective.piece()).toList();
				for (Move move : detectiveMoves) {
					myGameState childState = gameState.advance(move);
					if (childState.getRemaining().contains(childState.getMrX().piece())) {
						int eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList(), childState.getDetectives());
						minEval.set(min(minEval.get(), eval));
					} else {
						int eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList(), childState.getDetectives());
						minEval.set(min(minEval.get(), eval));
					}
				}
			});
			//for (Player detective : remainingDetectives) {


			//}

			/*for (Move move : moves) {
				myGameState childState = gameState.advance(move);
				if (childState.getRemaining().contains(childState.getMrX().piece())) {
					int eval = miniMax(childState, depth - 1, true, childState.getAvailableMoves().asList());
					minEval = min(minEval, eval);
				} else {
					int eval = miniMax(childState, depth, false, childState.getAvailableMoves().asList());
					minEval = min(minEval, eval);
				}
			}*/

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
