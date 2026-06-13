package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DistanceMatrix {

    private final List<String> labels;
    private final double[][] matrix;

    public DistanceMatrix(Map<String, GeoPoint> players) {
        this.labels = new ArrayList<>(players.keySet());
        int n = labels.size();
        this.matrix = new double[n][n];

        List<GeoPoint> points = new ArrayList<>(players.values());

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = points.get(i).distanceTo(points.get(j));
                }
            }
        }
    }

    public double getDistance(String playerA, String playerB) {
        int i = labels.indexOf(playerA);
        int j = labels.indexOf(playerB);
        if (i < 0 || j < 0) return -1;
        return matrix[i][j];
    }

    public String getClosestTo(String playerName) {
        int idx = labels.indexOf(playerName);
        if (idx < 0) return null;
        double min = Double.MAX_VALUE;
        String closest = null;
        for (int j = 0; j < labels.size(); j++) {
            if (j == idx) continue;
            if (matrix[idx][j] < min) {
                min = matrix[idx][j];
                closest = labels.get(j);
            }
        }
        return closest;
    }

    public String getFarthestTo(String playerName) {
        int idx = labels.indexOf(playerName);
        if (idx < 0) return null;
        double max = Double.MIN_VALUE;
        String farthest = null;
        for (int j = 0; j < labels.size(); j++) {
            if (j == idx) continue;
            if (matrix[idx][j] > max) {
                max = matrix[idx][j];
                farthest = labels.get(j);
            }
        }
        return farthest;
    }

    public Map<String, Double> getDistancesFrom(String playerName) {
        int idx = labels.indexOf(playerName);
        if (idx < 0) return Collections.emptyMap();
        Map<String, Double> result = new LinkedHashMap<>();
        for (int j = 0; j < labels.size(); j++) {
            if (j != idx) result.put(labels.get(j), matrix[idx][j]);
        }
        return result;
    }

    public double getAverageDistance() {
        int n = labels.size();
        if (n < 2) return 0;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += matrix[i][j];
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    public List<String> getLabels() { return Collections.unmodifiableList(labels); }
    public double[][] getRawMatrix() { return matrix; }
}
