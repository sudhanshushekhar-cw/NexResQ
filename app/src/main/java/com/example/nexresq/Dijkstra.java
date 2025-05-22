package com.example.nexresq;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    private Map<String, Map<String, Double>> graph;
    private Map<String, Double> distances;
    private Map<String, String> previous;
    private Set<String> visited;

    public Dijkstra(Map<String, Map<String, Double>> graph) {
        this.graph = graph;
        this.distances = new HashMap<>();
        this.previous = new HashMap<>();
        this.visited = new HashSet<>();
    }

    public List<String> findShortestPath(String start, String end) {
        // Initialize distances
        for (String node : graph.keySet()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(start, 0.0);

        PriorityQueue<String> queue = new PriorityQueue<>(
                Comparator.comparingDouble(node -> distances.get(node))
        );
        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(end)) {
                break;
            }

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            Map<String, Double> neighbors = graph.get(current);
            if (neighbors == null) continue;

            for (Map.Entry<String, Double> neighbor : neighbors.entrySet()) {
                String nextNode = neighbor.getKey();
                double distance = neighbor.getValue();
                double newDistance = distances.get(current) + distance;

                if (newDistance < distances.get(nextNode)) {
                    distances.put(nextNode, newDistance);
                    previous.put(nextNode, current);
                    queue.add(nextNode);
                }
            }
        }

        return reconstructPath(end);
    }

    private List<String> reconstructPath(String end) {
        List<String> path = new ArrayList<>();
        String current = end;

        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }

        return path;
    }

    public double getDistance(String node) {
        return distances.getOrDefault(node, Double.MAX_VALUE);
    }
}
