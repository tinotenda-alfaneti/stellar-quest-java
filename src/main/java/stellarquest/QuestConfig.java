package stellarquest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Network;

@Component
public final class QuestConfig {
    @Value("${QUEST_SECRET:}")
    private String questSecret;

    @Value("${HORIZON_URL:https://horizon-testnet.stellar.org}")
    private String horizonUrl;

    @Value("${FRIENDBOT_URL:https://friendbot.stellar.org}")
    private String friendbotUrl;

    @Value("${NETWORK:testnet}")
    private String networkName;

    @Value("${STARTING_BALANCE:1000}")
    private String startingBalance;

    @Value("${BASE_FEE:100}")
    private int baseFee;

    @Value("${TIMEOUT_SECONDS:30}")
    private int timeoutSeconds;

    @Value("${DESTINATION_PUBLIC_KEY:}")
    private String destinationPublicKey;

    private static Network networkFromName(String name) {
        if (name == null) {
            return Network.TESTNET;
        }
        String normalized = name.trim().toLowerCase();
        if ("public".equals(normalized) || "pubnet".equals(normalized) || "mainnet".equals(normalized)) {
            return Network.PUBLIC;
        }
        return Network.TESTNET;
    }

    public String questSecret() {
        return questSecret;
    }

    public String horizonUrl() {
        return horizonUrl;
    }

    public String friendbotUrl() {
        return friendbotUrl;
    }

    public Network network() {
        return networkFromName(networkName);
    }

    public String startingBalance() {
        return startingBalance;
    }

    public int baseFee() {
        return baseFee;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public String destinationPublicKey() {
        return destinationPublicKey;
    }
}
