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
import org.stellar.sdk.exception.AccountNotFoundException;
import org.stellar.sdk.exception.BadRequestException;
import org.stellar.sdk.exception.BadResponseException;
import org.stellar.sdk.exception.NetworkException;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.Problem;
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

    public AccountResponse loadAccount(String accountId) throws IOException {
        try {
            // AccountResponse is also a TransactionBuilderAccount and includes balances for verbose output.
            return server.accounts().account(accountId);
        } catch (AccountNotFoundException ex) {
            throw new IOException("Account not found on the current network: " + accountId, ex);
        } catch (NetworkException ex) {
            String status = ex.getCode() == null ? "unknown" : String.valueOf(ex.getCode());
            throw new IOException("Failed to load account: " + accountId + " (HTTP " + status + ").", ex);
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
        } catch (BadRequestException ex) {
            throw new IOException(problemMessage("Transaction rejected by Horizon", ex.getProblem(), ex), ex);
        } catch (BadResponseException ex) {
            throw new IOException(problemMessage("Unexpected Horizon response", ex.getProblem(), ex), ex);
        } catch (NetworkException ex) {
            String status = ex.getCode() == null ? "unknown" : String.valueOf(ex.getCode());
            throw new IOException("Failed to submit transaction (HTTP " + status + ").", ex);
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
                String body = response.body();
                // Friendbot returns 400 if the account is already funded; treat as non-fatal.
                if (status == 400 && body != null && body.contains("account already funded")) {
                    return body;
                }
                throw new IOException("Friendbot request failed with status " + status + ": " + body);
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

    private static String problemMessage(String prefix, Problem problem, NetworkException ex) {
        StringBuilder message = new StringBuilder(prefix);

        if (problem != null && problem.getDetail() != null && !problem.getDetail().isBlank()) {
            message.append(". ").append(problem.getDetail().trim());
        }

        if (problem != null && problem.getExtras() != null && problem.getExtras().getResultCodes() != null) {
            Problem.Extras.ResultCodes resultCodes = problem.getExtras().getResultCodes();
            if (resultCodes.getTransactionResultCode() != null && !resultCodes.getTransactionResultCode().isBlank()) {
                message.append(" tx=").append(resultCodes.getTransactionResultCode());
            }
            if (resultCodes.getOperationsResultCodes() != null && !resultCodes.getOperationsResultCodes().isEmpty()) {
                message.append(" ops=").append(resultCodes.getOperationsResultCodes());
            }
        }

        if (ex != null && ex.getCode() != null) {
            message.append(" (HTTP ").append(ex.getCode()).append(")");
        }

        return message.toString();
    }
}
