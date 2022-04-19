package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Player;
import java.util.List;

public class bfsNode {
    private final int node;
    private final int nodeDepth;
    private final Player explorer;
    private final List<Integer> nodesTraversed;

    bfsNode(Player explorer, int node, List<Integer> nodesTraversed, int depthLevel) {
        this.explorer = explorer;
        this.node = node;
        this.nodesTraversed = List.copyOf(nodesTraversed);
        this.nodeDepth = depthLevel;
    }

    // Getter Methods
    public Player getExplorer() { return this.explorer; }

    public int getNode() { return this.node; }

    public List<Integer> getNodesTraversed() { return this.nodesTraversed; }

    public int getDepth() { return this.nodeDepth; }
}
