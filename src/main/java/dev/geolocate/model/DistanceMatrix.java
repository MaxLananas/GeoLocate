package dev.geolocate.model;

import dev.geolocate.mapping.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DistanceMatrix {

    private final List<String> labels;
    // label -> index in labels list — O(1) lookup instead of O(n) indexOf
    private final Map<String, Integer> labelIndex;
    private final double[][] matrix;

    public DistanceMatrix(Map<String, GeoPoint> players) {
        this.labels     = new ArrayList<>(players.keySet());
        this.labelIndex = new HashMap<>(labels.size() * 2);
        for (int i = 0; i < labels.size(); i++) {
            labelIndex.put(labels.get(i), i);
        }

        int n = labels.size();
        this.matrix = new double[n][n];
        List<GeoPoint> points = new ArrayList<>(players.values());

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = points.get(i).distanceTo(points.get(j));
                matrix[i][j] = d;
                matrix[j][i] = d; // symmetric
            }
        }
    }

    public double getDistance(String playerA, String playerB) {
        Integer i = labelIndex.get(playerA);
        Integer j = labelIndex.get(playerB);
        if (i == null || j == null) return -1;
        return matrix[i][j];
    }

    public String getClosestTo(String playerName) {
        Integer idx = labelIndex.get(playerName);
        if (idx == null) return null;
        double min   = Double.MAX_VALUE;
        String closest = null;
        double[] row = matrix[idx];
        for (int j = 0; j < labels.size(); j++) {
            if (j == idx) continue;
            if (row[j] < min) { min = row[j]; closest = labels.get(j); }
        }
        return closest;
    }

    public String getFarthestTo(String playerName) {
        Integer idx = labelIndex.get(playerName);
        if (idx == null) return null;
        double max    = -Double.MAX_VALUE;
        String farthest = null;
        double[] row  = matrix[idx];
        for (int j = 0; j < labels.size(); j++) {
            if (j == idx) continue;
            if (row[j] > max) { max = row[j]; farthest = labels.get(j); }
        }
        return farthest;
    }

    public Map<String, Double> getDistancesFrom(String playerName) {
        Integer idx = labelIndex.get(playerName);
        if (idx == null) return Collections.emptyMap();
        Map<String, Double> result = new LinkedHashMap<>(labels.size());
        double[] row = matrix[idx];
        for (int j = 0; j < labels.size(); j++) {
            if (j != idx) result.put(labels.get(j), row[j]);
        }
        return result;
    }

    public double getAverageDistance() {
        int n = labels.size();
        if (n < 2) return 0;
        double sum   = 0;
        int    count = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += matrix[i][j];
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    public List<String> getLabels()    { return Collections.unmodifiableList(labels); }
    public double[][]   getRawMatrix() { return matrix; }
}
