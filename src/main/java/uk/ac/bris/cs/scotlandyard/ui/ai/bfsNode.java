package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.List;

public class bfsNode {
    private final int node;
    private final int nodeDepth;
    private final Player explorer;
    private boolean isExplored;
    private final List<Integer> nodesTraversed;

    bfsNode(Player explorer, int node, List<Integer> nodesTraversed, int depthLevel) {
        this.explorer = explorer;
        this.node = node;
        this.nodesTraversed = List.copyOf(nodesTraversed);
        this.nodeDepth = depthLevel;
        this.isExplored = false;
    }

    public Player updateExplorerTickets(ScotlandYard.Ticket ticket) { return this.explorer.use(ticket); }

    // Getter Methods
    public Player getExplorer() { return this.explorer; }

    public int getNode() { return this.node; }

    public List<Integer> getNodesTraversed() { return this.nodesTraversed; }

    public int getDepth() { return this.nodeDepth; }

    // Setter Method(s)
    private void nodeExplored() { this.isExplored = true; }

    public boolean exploreNode(int target) throws IllegalAccessError {
        if (!isExplored) {
            nodeExplored();
            return this.node == target;
        } else {
            throw new IllegalAccessError("Node should not be explored again!");
        }
    }
}
