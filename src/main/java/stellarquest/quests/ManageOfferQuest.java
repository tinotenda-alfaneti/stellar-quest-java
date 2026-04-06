package stellarquest.quests;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.ChangeTrustAsset;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Price;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.operations.ChangeTrustOperation;
import org.stellar.sdk.operations.CreatePassiveSellOfferOperation;
import org.stellar.sdk.operations.ManageBuyOfferOperation;
import org.stellar.sdk.operations.ManageSellOfferOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;

@Component
public final class ManageOfferQuest {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public ManageOfferQuest(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public void run(boolean verbose) throws IOException {
        // Flow: trust the counter-asset, then create a buy/sell/passive offer on the DEX.
        String secret = config.questSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("QUEST_SECRET is required.");
        }

        KeyPair questKeyPair = KeyPair.fromSecretSeed(secret);
        AccountResponse questAccount = client.loadAccount(questKeyPair.getAccountId());

        Asset counterAsset = buildAsset(config.offerAssetCode(), config.offerAssetIssuer());
        ChangeTrustAsset trustAsset = new ChangeTrustAsset(counterAsset);

        String trustLimit = config.offerTrustLimit();
        if (trustLimit == null || trustLimit.isBlank()) {
            trustLimit = "922337203685.4775807";
        }

        ChangeTrustOperation.ChangeTrustOperationBuilder<?, ?> trustBuilder = ChangeTrustOperation.builder()
                .asset(trustAsset)
                .limit(new BigDecimal(trustLimit));

        String offerType = normalize(config.offerType());
        Price price = Price.fromString(config.offerPrice());
        BigDecimal sellAmount = new BigDecimal(config.offerAmount());
        BigDecimal buyAmount = new BigDecimal(config.offerBuyAmount());

        TransactionBuilder builder = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(trustBuilder.build());

        if ("buy".equals(offerType)) {
            builder.addOperation(
                    ManageBuyOfferOperation.builder()
                            .selling(new AssetTypeNative())
                            .buying(counterAsset)
                            .amount(buyAmount)
                            .price(price)
                            .offerId(config.offerId())
                            .build()
            );
        } else if ("passive".equals(offerType)) {
            builder.addOperation(
                    CreatePassiveSellOfferOperation.builder()
                            .selling(new AssetTypeNative())
                            .buying(counterAsset)
                            .amount(sellAmount)
                            .price(price)
                            .build()
            );
        } else {
            builder.addOperation(
                    ManageSellOfferOperation.builder()
                            .selling(new AssetTypeNative())
                            .buying(counterAsset)
                            .amount(sellAmount)
                            .price(price)
                            .offerId(config.offerId())
                            .build()
            );
        }

        Transaction transaction = builder
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKeyPair);

        if (verbose) {
            System.out.println("Offer Type: " + offerType);
            System.out.println("Selling: XLM");
            System.out.println("Buying: " + config.offerAssetCode() + ":" + config.offerAssetIssuer());
            System.out.println("Price: " + config.offerPrice());
            System.out.println("Amount: " + ("buy".equals(offerType) ? config.offerBuyAmount() : config.offerAmount()));
        }

        TransactionResponse response = client.submitTransaction(transaction);

        System.out.println("Transaction successful.");
        System.out.println("Hash: " + response.getHash());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "sell";
        }
        String normalized = value.trim().toLowerCase();
        if ("buy".equals(normalized) || "sell".equals(normalized) || "passive".equals(normalized)) {
            return normalized;
        }
        return "sell";
    }

    private static Asset buildAsset(String assetCode, String issuerPublicKey) {
        String code = assetCode == null ? "" : assetCode.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("OFFER_ASSET_CODE must not be empty.");
        }
        if (issuerPublicKey == null || issuerPublicKey.isBlank()) {
            throw new IllegalArgumentException("OFFER_ASSET_ISSUER must not be empty.");
        }
        if (code.length() <= 4) {
            return new AssetTypeCreditAlphaNum4(code, issuerPublicKey);
        }
        if (code.length() <= 12) {
            return new AssetTypeCreditAlphaNum12(code, issuerPublicKey);
        }
        throw new IllegalArgumentException("OFFER_ASSET_CODE must be 12 characters or fewer.");
    }
}
