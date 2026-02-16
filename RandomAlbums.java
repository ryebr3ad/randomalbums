import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class RandomAlbums {

    public static final int DEFAULT_TOTAL = 5;

    public static void main(String args[]) {
        int totalAlbums = DEFAULT_TOTAL;
        if (args.length < 1) {
            System.err.println("Must provide path to music files");
            System.exit(-1);
        }
        if (args.length >= 2) {
            try {
                totalAlbums = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                System.err.println(args[1] + " is not a number");
                System.exit(-2);
            }
        }
        HttpClient client = HttpClient.newHttpClient();

        // https://hyperblast.org/beefweb/api/
        sendRequest(client, "http://localhost:8880/api/playlists/p3/clear", "");

        Path startPath = Paths.get(args[0]);
        try (Stream<Path> stream = Files.walk(startPath)) {
            List<Path> albums = stream.filter(p -> p.toString().split("\\\\").length == 6).toList();
            List<Path> tracks = new ArrayList<>();
            int size = albums.size();

            for (int i = 0; i < totalAlbums; i++) {
                Stream<Path> streamAgain = Files.walk(startPath);
                Path pickedAlbum = albums.get(ThreadLocalRandom.current().nextInt(size));
                List<Path> pickedTracks = streamAgain
                        .filter(p -> p.startsWith(pickedAlbum) && p.toString().split("\\\\").length > 6).toList();
                tracks.addAll(pickedTracks);
                streamAgain.close();
            }

            sendRequest(client, "http://localhost:8880/api/playlists/p3/items/add", buildBody(tracks));
            sendRequest(client, "http://localhost:8880/api/player/play/p3/0", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Body modeled for Beefweb API v. 0.11
    private static String buildBody(List<Path> tracks) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"items\": [");
        for (int i = 0; i < tracks.size(); i++) {
            var track = tracks.get(i);
            String trackStr = track.toString();
            String formattedTrackStr = trackStr.replace("\\", "/");
            sb.append("\"" + formattedTrackStr + "\"");
            if (i < tracks.size() - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        sb.append('}');
        return sb.toString();
    }

    private static void sendRequest(HttpClient client, String path, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}