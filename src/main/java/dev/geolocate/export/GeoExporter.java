package dev.geolocate.export;

import dev.geolocate.mapping.GeoPoint;
import dev.geolocate.model.GeoPath;
import dev.geolocate.model.GeoRegion;
import dev.geolocate.model.GeoSnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public final class GeoExporter {

    private GeoExporter() {}

    public static String snapshotsToCSV(List<GeoSnapshot> snapshots) {
        StringBuilder sb = new StringBuilder();
        sb.append("uuid,name,lat,lon,alt,world,x,y,z,timestamp\n");
        for (GeoSnapshot s : snapshots) {
            sb.append(s.toCSVLine()).append("\n");
        }
        return sb.toString();
    }

    public static String snapshotsToGeoJSON(List<GeoSnapshot> snapshots) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"FeatureCollection\",\"features\":[");
        for (int i = 0; i < snapshots.size(); i++) {
            GeoSnapshot s = snapshots.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"type\":\"Feature\",");
            sb.append("\"geometry\":{\"type\":\"Point\",\"coordinates\":[");
            sb.append(s.getPoint().longitude()).append(",").append(s.getPoint().latitude()).append("]},");
            sb.append("\"properties\":{");
            sb.append("\"player\":\"").append(s.getPlayerName()).append("\",");
            sb.append("\"world\":\"").append(s.getWorldName()).append("\",");
            sb.append("\"altitude\":").append(s.getPoint().altitude()).append(",");
            sb.append("\"timestamp\":\"").append(s.getTimestamp()).append("\"");
            sb.append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String pathToGPX(GeoPath path) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<gpx version=\"1.1\" creator=\"GeoLocate\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n");
        sb.append("  <trk>\n");
        sb.append("    <name>").append(path.getName()).append("</name>\n");
        sb.append("    <trkseg>\n");
        for (GeoPoint p : path.getPoints()) {
            sb.append("      <trkpt lat=\"").append(p.latitude()).append("\" lon=\"").append(p.longitude()).append("\">\n");
            sb.append("        <ele>").append(p.altitude()).append("</ele>\n");
            sb.append("      </trkpt>\n");
        }
        sb.append("    </trkseg>\n");
        sb.append("  </trk>\n");
        sb.append("</gpx>");
        return sb.toString();
    }

    public static String pathToKML(GeoPath path) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("  <Document>\n");
        sb.append("    <name>").append(path.getName()).append("</name>\n");
        sb.append("    <Placemark>\n");
        sb.append("      <LineString>\n");
        sb.append("        <coordinates>\n");
        for (GeoPoint p : path.getPoints()) {
            sb.append("          ").append(p.longitude()).append(",").append(p.latitude()).append(",").append(p.altitude()).append("\n");
        }
        sb.append("        </coordinates>\n");
        sb.append("      </LineString>\n");
        sb.append("    </Placemark>\n");
        sb.append("  </Document>\n");
        sb.append("</kml>");
        return sb.toString();
    }

    public static String regionToGeoJSON(GeoRegion region) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Feature\",");
        sb.append("\"properties\":{\"id\":\"").append(region.getId()).append("\",\"name\":\"").append(region.getName()).append("\"},");
        sb.append("\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[");
        List<GeoPoint> vertices = region.getVertices();
        for (int i = 0; i <= vertices.size(); i++) {
            GeoPoint v = vertices.get(i % vertices.size());
            if (i > 0) sb.append(",");
            sb.append("[").append(v.longitude()).append(",").append(v.latitude()).append("]");
        }
        sb.append("]]}}");
        return sb.toString();
    }

    public static void saveToFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    public static File buildExportFile(File dataFolder, String playerName, String format) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        return new File(dataFolder, "exports/" + playerName + "_" + timestamp + "." + format);
    }
}
