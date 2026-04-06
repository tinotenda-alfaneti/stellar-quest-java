package stellarquest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stellar.sdk.Network;

@Component
public final class QuestConfig {
    // Quest account (payer) secret used across quests.
    @Value("${QUEST_SECRET:}")
    private String questSecret;

    // Network endpoints and selection.
    @Value("${HORIZON_URL:https://horizon-testnet.stellar.org}")
    private String horizonUrl;

    @Value("${FRIENDBOT_URL:https://friendbot.stellar.org}")
    private String friendbotUrl;

    @Value("${NETWORK:testnet}")
    private String networkName;

    // Create-account quest options.
    @Value("${STARTING_BALANCE:1000}")
    private String startingBalance;

    // Payment quest options.
    @Value("${PAYMENT_AMOUNT:100}")
    private String paymentAmount;

    // Trustline quest options.
    @Value("${ASSET_CODE:SANTA}")
    private String assetCode;

    @Value("${TRUST_LIMIT:100}")
    private String trustLimit;

    // Fees and timeouts.
    @Value("${BASE_FEE:100}")
    private int baseFee;

    @Value("${TIMEOUT_SECONDS:30}")
    private int timeoutSeconds;

    // Optional destination overrides.
    @Value("${DESTINATION_PUBLIC_KEY:}")
    private String destinationPublicKey;

    @Value("${PAYMENT_DESTINATION_PUBLIC_KEY:}")
    private String paymentDestinationPublicKey;

    @Value("${ISSUER_PUBLIC_KEY:}")
    private String issuerPublicKey;

    // DEX offer quest options.
    @Value("${OFFER_TYPE:buy}")
    private String offerType;

    @Value("${OFFER_ASSET_CODE:USDC}")
    private String offerAssetCode;

    @Value("${OFFER_ASSET_ISSUER:GBBD47IF6LWK7P7MDEVSCWR7DPUWV3NY3DTQEVFL4NAT4AQH3ZLLFLA5}")
    private String offerAssetIssuer;

    @Value("${OFFER_PRICE:0.1}")
    private String offerPrice;

    @Value("${OFFER_AMOUNT:1000}")
    private String offerAmount;

    @Value("${OFFER_BUY_AMOUNT:100}")
    private String offerBuyAmount;

    @Value("${OFFER_ID:0}")
    private long offerId;

    @Value("${OFFER_TRUST_LIMIT:}")
    private String offerTrustLimit;

    // Path payment quest options.
    @Value("${PATH_ASSET_CODE:PATH}")
    private String pathAssetCode;

    @Value("${PATH_SEND_AMOUNT:1000}")
    private String pathSendAmount;

    @Value("${PATH_DEST_MIN:1000}")
    private String pathDestMin;

    @Value("${PATH_DEST_AMOUNT:450}")
    private String pathDestAmount;

    @Value("${PATH_SEND_MAX:450}")
    private String pathSendMax;

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

    public String paymentAmount() {
        return paymentAmount;
    }

    public String assetCode() {
        return assetCode;
    }

    public String trustLimit() {
        return trustLimit;
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

    public String paymentDestinationPublicKey() {
        return paymentDestinationPublicKey;
    }

    public String issuerPublicKey() {
        return issuerPublicKey;
    }

    public String offerType() {
        return offerType;
    }

    public String offerAssetCode() {
        return offerAssetCode;
    }

    public String offerAssetIssuer() {
        return offerAssetIssuer;
    }

    public String offerPrice() {
        return offerPrice;
    }

    public String offerAmount() {
        return offerAmount;
    }

    public String offerBuyAmount() {
        return offerBuyAmount;
    }

    public long offerId() {
        return offerId;
    }

    public String offerTrustLimit() {
        return offerTrustLimit;
    }

    public String pathAssetCode() {
        return pathAssetCode;
    }

    public String pathSendAmount() {
        return pathSendAmount;
    }

    public String pathDestMin() {
        return pathDestMin;
    }

    public String pathDestAmount() {
        return pathDestAmount;
    }

    public String pathSendMax() {
        return pathSendMax;
    }
}
