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
import java.util.*;
import java.util.stream.*;

public class OllamaClient {
    private static String API_URI = "http://localhost:11434/api/generate";
    private static String MODEL = "llama3.2:3b";
    private static HttpClient CLIENT = HttpClient.newHttpClient();
    public static final OllamaClient INSTANCE = new OllamaClient();

    private OllamaClient() {}

    public static OllamaClient getInstance() {
        return INSTANCE;
    }

    public Stream<TokenData> ask(String prompt, List<String> history) throws Exception {
        // combine history if needed
        StringBuilder fullPrompt = new StringBuilder();
        for (String line : history) {
            fullPrompt.append(line).append("\\n");
        }
        fullPrompt.append("You: ").append(prompt).append("\\n");

        // use your stream-based Ollama client here (stream or full response)
        return OllamaClient.getInstance().generate(fullPrompt.toString());
    }

    private static class StreamChunk {
        String response;
    }

    public static Stream<TokenData> generate(String prompt) throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", OllamaClient.MODEL);
        jsonObject.addProperty("prompt", prompt);
        jsonObject.addProperty("stream", false);
        String json = new Gson().toJson(jsonObject);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(OllamaClient.API_URI))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = OllamaClient.CLIENT.send(
                request, HttpResponse.BodyHandlers.ofInputStream());

        var statusCode = response.statusCode();

        if(statusCode != 200) {
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8));
            StringBuilder sbErr = new StringBuilder();
            String errLine;
            while ((errLine = errReader.readLine()) != null) {
                sbErr.append(errLine).append("\n");
            }
            System.err.println("Ollama Error Body:\n" + sbErr);
            return Stream.empty();
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        Gson gson = new Gson();

        Iterator<TokenData> iterator = getTokenDataIterator(reader, gson);

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        ).onClose(() -> {
            try {
                reader.close();
            } catch (IOException ignored) {}
        });
    }

    /*
    *   Iterator definition for TokenData (needed to stream answers)
    */
    private static Iterator<TokenData> getTokenDataIterator(BufferedReader reader, Gson gson) {
        Iterator<TokenData> iterator = new Iterator<>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                try {
                    do {
                        nextLine = reader.readLine();
                    } while (nextLine != null && nextLine.trim().isEmpty());
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
                    return new TokenData("");
                }
            }
        };
        return iterator;
    }

    public static void main(String[] args) throws Exception {
        String prompt = "Hey, how are you?";
        try {
            generate(prompt)
                    .forEach(token -> System.out.print(token.getToken()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
