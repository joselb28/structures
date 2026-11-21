package com.transit.map;

import com.transit.model.BusLine;
import com.transit.model.CityMap;
import com.transit.model.Edge;
import com.transit.model.Node;
import com.transit.settings.Settings;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistance de la carte dans SQLite. Utilisée seulement au démarrage (et à l'arrêt) :
 * la boucle de simulation ne touche jamais à la base.
 */
public final class MapStore {

    /** À augmenter quand le schéma ou l'algorithme du générateur change, pour forcer la régénération. */
    static final int SCHEMA_VERSION = 3;

    private MapStore() {
    }

    /**
     * Charge la carte depuis {@code dbFile}, en la générant et en l'enregistrant d'abord si le fichier
     * n'existe pas ou a été produit par une autre version du schéma, une autre graine ou d'autres réglages de ville.
     */
    public static CityMap loadOrGenerate(Path dbFile, Settings.City city, long seed) {
        String fingerprint = "v" + SCHEMA_VERSION + " seed=" + seed + " " + city;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile)) {
            if (!fingerprint.equals(readMeta(conn, "fingerprint"))) {
                save(conn, MapGenerator.generate(city, seed), fingerprint);
            }
            return load(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load city map from " + dbFile, e);
        }
    }

    /** @return la valeur de méta, ou null si la clé ou toute la table meta est absente */
    private static String readMeta(Connection conn, String key) {
        try (PreparedStatement st = conn.prepareStatement("SELECT value FROM meta WHERE key = ?")) {
            st.setString(1, key);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException noMetaTable) {
            return null;
        }
    }

    /** Remplace tout le contenu de la base par {@code map}, en une seule transaction. */
    static void save(Connection conn, CityMap map, String fingerprint) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String table : List.of("meta", "nodes", "edges", "bus_lines", "bus_line_nodes", "roads")) {
                st.execute("DROP TABLE IF EXISTS " + table); // 'roads' : ancienne table du prototype
            }
            st.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            st.execute("CREATE TABLE nodes (id INTEGER PRIMARY KEY, x REAL NOT NULL, y REAL NOT NULL, type TEXT NOT NULL)");
            st.execute("CREATE TABLE edges (a INTEGER NOT NULL, b INTEGER NOT NULL, PRIMARY KEY (a, b))");
            st.execute("CREATE TABLE bus_lines (number INTEGER PRIMARY KEY)");
            st.execute("CREATE TABLE bus_line_nodes (line INTEGER NOT NULL, seq INTEGER NOT NULL, node INTEGER NOT NULL,"
                    + " PRIMARY KEY (line, seq))");
        }

        try (PreparedStatement meta = conn.prepareStatement("INSERT INTO meta VALUES (?, ?)");
             PreparedStatement node = conn.prepareStatement("INSERT INTO nodes VALUES (?, ?, ?, ?)");
             PreparedStatement edge = conn.prepareStatement("INSERT INTO edges VALUES (?, ?)");
             PreparedStatement line = conn.prepareStatement("INSERT INTO bus_lines VALUES (?)");
             PreparedStatement stop = conn.prepareStatement("INSERT INTO bus_line_nodes VALUES (?, ?, ?)")) {

            for (String[] entry : List.of(new String[]{"fingerprint", fingerprint},
                    new String[]{"width", String.valueOf(map.width())},
                    new String[]{"height", String.valueOf(map.height())})) {
                meta.setString(1, entry[0]);
                meta.setString(2, entry[1]);
                meta.addBatch();
            }
            meta.executeBatch();

            for (Node n : map.nodes()) {
                node.setInt(1, n.id());
                node.setDouble(2, n.x());
                node.setDouble(3, n.y());
                node.setString(4, n.type().name());
                node.addBatch();
            }
            node.executeBatch();

            for (Edge e : map.edges()) {
                edge.setInt(1, e.a());
                edge.setInt(2, e.b());
                edge.addBatch();
            }
            edge.executeBatch();

            for (BusLine l : map.busLines()) {
                line.setInt(1, l.number());
                line.addBatch();
                List<Integer> ids = l.nodeIds();
                for (int seq = 0; seq < ids.size(); seq++) {
                    stop.setInt(1, l.number());
                    stop.setInt(2, seq);
                    stop.setInt(3, ids.get(seq));
                    stop.addBatch();
                }
            }
            line.executeBatch();
            stop.executeBatch();
        }
        conn.commit();
        conn.setAutoCommit(true);
    }

    /** Relit toute la carte, une requête par table. */
    static CityMap load(Connection conn) throws SQLException {
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        Map<Integer, List<Integer>> lineNodes = new LinkedHashMap<>();

        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT id, x, y, type FROM nodes ORDER BY id")) {
                while (rs.next()) {
                    nodes.add(new Node(rs.getInt(1), rs.getDouble(2), rs.getDouble(3), Node.Type.valueOf(rs.getString(4))));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT a, b FROM edges")) {
                while (rs.next()) {
                    edges.add(new Edge(rs.getInt(1), rs.getInt(2)));
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT line, node FROM bus_line_nodes ORDER BY line, seq")) {
                while (rs.next()) {
                    lineNodes.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>()).add(rs.getInt(2));
                }
            }
        }

        List<BusLine> busLines = new ArrayList<>();
        lineNodes.forEach((number, ids) -> busLines.add(new BusLine(number, ids)));
        int width = Integer.parseInt(readMeta(conn, "width"));
        int height = Integer.parseInt(readMeta(conn, "height"));
        return new CityMap(width, height, nodes, edges, busLines);
    }
}
