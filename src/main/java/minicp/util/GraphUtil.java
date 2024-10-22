/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Algorithms and Graph interface
 */
public class GraphUtil {

    /**
     * Directed graph API
     */
    public interface Graph {
        /**
         * Returns the number of nodes in this graph.
         * @return the number of nodes in this graph.
         *         Nodes are identified from 0 to {@link #n()}-1.
         */
        int n();

        /**
         * Returns the incoming node indexes in the specified node
         * @param id the identifier of the specified node
         * @return the identifiers of the nodes pointing to the specified node
         */
        Iterable<Integer> in(int id);

        /**
         * Returns the outgoing node indexes from the specified node
         * @param id the identifier of the specified node
         * @return the identifiers of the nodes originating from the specified node
         */
        Iterable<Integer> out(int id);
    }

    public static class DirectedGraph implements Graph {

        private ArrayList<Set<Integer>> in;
        private ArrayList<Set<Integer>> out;
        private int n;

        public DirectedGraph(int n) {
            this.n = n;
            in = new ArrayList<>();
            out = new ArrayList<>();
            for (int i = 0; i < n; ++i) {
                in.add(new HashSet<>());
                out.add(new HashSet<>());
            }
        }

        public void clear() {
            for (int i = 0; i < n; ++i) {
                in.get(i).clear();
                out.get(i).clear();
            }
        }

        // Add an edge from i to j
        public void link(int i, int j) {
            out.get(i).add(j);
            in.get(j).add(i);
        }

        // Remove an edge from i to j
        public void unlink(int i, int j) {
            out.get(i).remove(j);
            in.get(j).remove(i);
        }

        /**
         * @return the number of nodes in this graph. They are indexed from 0 to n-1.
         */
        @Override
        public int n() {
            return n;
        }

        /**
         * @param idx the node to consider
         * @return the nodes ids that have an edge going from then to node idx
         */
        @Override
        public Iterable<Integer> in(int idx) {
            return in.get(idx);
        }

        /**
         * @param idx the node to consider
         * @return the nodes ids that have an edge going from node idx to them.
         */
        @Override
        public Iterable<Integer> out(int idx) {
            return out.get(idx);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < n; ++i) {
                builder.append("x[").append(i).append("];").append("\n");

                builder.append("in: ");

                for (int in: in(i)) {
                    builder.append(in).append(" ");
                }
                builder.append("\n");

                builder.append("out: ");
                for (int out: out(i)) {
                    builder.append(out).append(" ");
                }
                builder.append("\n");
            }

            return builder.toString();
        }
    }

    /**
     * Transpose the graph i.e. every edge is reversed.
     *
     * @param graph a Graph
     * @return a new graph such that every edge is reversed
     */
    public static Graph transpose(Graph graph) {
        return new Graph() {
            @Override
            public int n() {
                return graph.n();
            }

            @Override
            public Iterable<Integer> in(int idx) {
                return graph.out(idx);
            }

            @Override
            public Iterable<Integer> out(int idx) {
                return graph.in(idx);
            }
        };
    }


    /**
     * Computes the strongly connected components of the graph
     * @param graph the input graph on which to compute the strongly
     *              connected components
     * @return for each node id, an id of the strongly connected
     *          components it belongs to
     */
    public static int[] stronglyConnectedComponents(Graph graph) {
        //Compute the suffix order
        Stack<Integer> firstOrder = new Stack<>();
        int[] visited = new int[graph.n()];
        Arrays.fill(visited, 0);
        for (int i = 0; i < graph.n(); i++) {
            if (visited[i] == 0) {
                dfsNode(graph, (suffix, b) -> {
                    if (suffix) firstOrder.push(b);
                }, visited, i);
            }
        }

        //Reverse the order, and do the dfs of the transposed graph
        Arrays.fill(visited, 0);
        int[] scc = new int[graph.n()];
        AtomicInteger cpt = new AtomicInteger(0);
        Graph tranposed = GraphUtil.transpose(graph);

        while (!firstOrder.empty()) {
            int next = firstOrder.pop();
            if (visited[next] == 0) {
                cpt.incrementAndGet();
                dfsNode(tranposed, (suffix, x) -> {
                    if (!suffix) scc[x] = cpt.get();
                }, visited, next);
            }
        }
        return scc;
    }

    private static void dfsNode(Graph graph, BiConsumer<Boolean, Integer> action, int[] visited, int start) {
        Stack<Integer> todo = new Stack<>();
        todo.add(start);

        // seen = 1
        // visited = 2
        // closed = 3
        while (!todo.isEmpty()) {
            int cur = todo.peek();
            if (visited[cur] == 0) {
                visited[cur] = 1; //seen
                action.accept(false, cur);
                for (int next : graph.out(cur)) {
                    if (visited[next] == 0) {
                        todo.add(next);
                    }
                }
                visited[cur] = 2; //visited
            } else if (visited[cur] == 2) {
                action.accept(true, cur);
                visited[cur] = 3; //closed
                todo.pop();
            }
            else
                todo.pop();
        }
    }

    /**
     * Checks if a path exists between start and end
     * @param graph
     * @param start a node id from the graph
     * @param end a node id from the graph
     * @return true if a directed path from start to end exists, false otherwise
     */
    public static boolean pathExists(Graph graph, int start, int end) {
        int[] visited = new int[graph.n()];
        Arrays.fill(visited, 0);
        dfsNode(graph,(v,n) -> {},visited,start);
        return visited[end] != 0;
    }
}
