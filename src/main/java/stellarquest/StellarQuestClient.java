package stellarquest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.stellar.sdk.Network;
import org.stellar.sdk.Server;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.responses.TransactionResponse;

@Component
public final class StellarQuestClient {
    private final Server server;
    private final Network network;
    private final int baseFee;
    private final int timeoutSeconds;
    private final String friendbotUrl;
    private final HttpClient httpClient;

    public StellarQuestClient(QuestConfig config) {
        this.server = new Server(config.horizonUrl());
        this.network = config.network();
        this.baseFee = config.baseFee();
        this.timeoutSeconds = config.timeoutSeconds();
        this.friendbotUrl = config.friendbotUrl();
        this.httpClient = HttpClient.newHttpClient();
    }

    public TransactionBuilderAccount loadAccount(String accountId) throws IOException {
        try {
            return server.loadAccount(accountId);
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new IOException("Failed to load account: " + accountId, ex);
        }
    }

    public TransactionResponse submitTransaction(Transaction transaction) throws IOException {
        try {
            return server.submitTransaction(transaction);
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new IOException("Failed to submit transaction.", ex);
        }
    }

    public String fundWithFriendbot(String accountId) throws IOException {
        String encoded = URLEncoder.encode(accountId, StandardCharsets.UTF_8);
        String url = friendbotUrl + "?addr=" + encoded;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Friendbot request failed with status " + status + ": " + response.body());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Friendbot request interrupted.", ex);
        }
    }

    public Network network() {
        return network;
    }

    public int baseFee() {
        return baseFee;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
