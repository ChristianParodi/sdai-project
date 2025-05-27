package ollama;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OllamaClient {
    private static String uri = "http://localhost:11434/api/generate";
    private static String model = "llama3.2:3b";
    private static HttpClient client = HttpClient.newHttpClient();

    private static class StreamChunk {
        String response;
    }

    public static Stream<TokenData> generate(String prompt) throws Exception {
        String json = "{\"model\":\"" + OllamaClient.model + "\",\"prompt\":\"" + prompt + "\",\"stream\":true}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(OllamaClient.uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = OllamaClient.client.send(
                request, HttpResponse.BodyHandlers.ofInputStream());

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        Gson gson = new Gson();

        Iterator<TokenData> iterator = new Iterator<>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                try {
                    nextLine = reader.readLine();
                    while (nextLine != null && nextLine.trim().isEmpty()) {
                        nextLine = reader.readLine();
                    }
                    return nextLine != null;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public TokenData next() {
                try {
                    StreamChunk chunk = gson.fromJson(nextLine, StreamChunk.class);
                    return new TokenData(chunk.response);
                } catch (Exception e) {
                    return new TokenData(""); // or throw
                }
            }
        };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).onClose(() -> {
            try {
                reader.close();
            } catch (IOException ignored) {}
        });
    }

    public static void main(String[] args) throws Exception {
        String prompt = "Hey, how are you?";
        generate(prompt)
                .forEach(token -> System.out.print(token.getToken()));
    }
}
