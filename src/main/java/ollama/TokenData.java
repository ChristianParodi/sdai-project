package ollama;

public class TokenData {
    private final String token;
    private final long timestamp;

    public TokenData(String token) {
        this.token = token;
        this.timestamp = System.currentTimeMillis();
    }

    public String getToken() {
        return token;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + token;
    }
}