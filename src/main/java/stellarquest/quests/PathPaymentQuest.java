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
import org.stellar.sdk.Network;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.operations.ChangeTrustOperation;
import org.stellar.sdk.operations.CreatePassiveSellOfferOperation;
import org.stellar.sdk.operations.PathPaymentStrictSendOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;

@Component
public final class PathPaymentQuest {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public PathPaymentQuest(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public void run(boolean verbose) throws IOException {
        // Flow: set up issuer/distributor/destination, create offers, then execute strict-send path payment.
        String secret = config.questSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("QUEST_SECRET is required.");
        }

        KeyPair questKeyPair = KeyPair.fromSecretSeed(secret);
        KeyPair issuerKeyPair = KeyPair.random();
        KeyPair distributorKeyPair = KeyPair.random();
        KeyPair destinationKeyPair = KeyPair.random();

        if (client.network().equals(Network.TESTNET)) {
            client.fundWithFriendbot(questKeyPair.getAccountId());
            client.fundWithFriendbot(issuerKeyPair.getAccountId());
            client.fundWithFriendbot(distributorKeyPair.getAccountId());
            client.fundWithFriendbot(destinationKeyPair.getAccountId());
        }

        Asset pathAsset = buildAsset(config.pathAssetCode(), issuerKeyPair.getAccountId());
        ChangeTrustAsset trustAsset = new ChangeTrustAsset(pathAsset);

        AccountResponse questAccount = client.loadAccount(questKeyPair.getAccountId());

        Transaction transaction = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(
                        ChangeTrustOperation.builder()
                                .asset(trustAsset)
                                .limit(new BigDecimal("922337203685.4775807"))
                                .sourceAccount(destinationKeyPair.getAccountId())
                                .build()
                )
                .addOperation(
                        ChangeTrustOperation.builder()
                                .asset(trustAsset)
                                .limit(new BigDecimal("922337203685.4775807"))
                                .sourceAccount(distributorKeyPair.getAccountId())
                                .build()
                )
                .addOperation(
                        org.stellar.sdk.operations.PaymentOperation.builder()
                                .destination(distributorKeyPair.getAccountId())
                                .asset(pathAsset)
                                .amount(new BigDecimal("1000000"))
                                .sourceAccount(issuerKeyPair.getAccountId())
                                .build()
                )
                .addOperation(
                        CreatePassiveSellOfferOperation.builder()
                                .selling(pathAsset)
                                .buying(new AssetTypeNative())
                                .amount(new BigDecimal("2000"))
                                .price(org.stellar.sdk.Price.fromString("1"))
                                .sourceAccount(distributorKeyPair.getAccountId())
                                .build()
                )
                .addOperation(
                        CreatePassiveSellOfferOperation.builder()
                                .selling(new AssetTypeNative())
                                .buying(pathAsset)
                                .amount(new BigDecimal("2000"))
                                .price(org.stellar.sdk.Price.fromString("1"))
                                .sourceAccount(distributorKeyPair.getAccountId())
                                .build()
                )
                .addOperation(
                        PathPaymentStrictSendOperation.builder()
                                .sendAsset(new AssetTypeNative())
                                .sendAmount(new BigDecimal(config.pathSendAmount()))
                                .destination(destinationKeyPair.getAccountId())
                                .destAsset(pathAsset)
                                .destMin(new BigDecimal(config.pathDestMin()))
                                .build()
                )
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKeyPair);
        transaction.sign(issuerKeyPair);
        transaction.sign(distributorKeyPair);
        transaction.sign(destinationKeyPair);

        if (verbose) {
            System.out.println("Quest Account: " + questKeyPair.getAccountId());
            System.out.println("Issuer Account: " + issuerKeyPair.getAccountId());
            System.out.println("Distributor Account: " + distributorKeyPair.getAccountId());
            System.out.println("Destination Account: " + destinationKeyPair.getAccountId());
            System.out.println("Path Asset: " + config.pathAssetCode() + ":" + issuerKeyPair.getAccountId());
            System.out.println("Path Payment (strict send): " + config.pathSendAmount() + " XLM -> min " + config.pathDestMin() + " " + config.pathAssetCode());
        }

        TransactionResponse response = client.submitTransaction(transaction);

        System.out.println("Transaction successful.");
        System.out.println("Hash: " + response.getHash());
        System.out.println("Issuer Secret Key: " + new String(issuerKeyPair.getSecretSeed()));
        System.out.println("Distributor Secret Key: " + new String(distributorKeyPair.getSecretSeed()));
        System.out.println("Destination Secret Key: " + new String(destinationKeyPair.getSecretSeed()));
    }

    private static Asset buildAsset(String assetCode, String issuerPublicKey) {
        String code = assetCode == null ? "" : assetCode.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("PATH_ASSET_CODE must not be empty.");
        }
        if (issuerPublicKey == null || issuerPublicKey.isBlank()) {
            throw new IllegalArgumentException("Issuer public key is required.");
        }
        if (code.length() <= 4) {
            return new AssetTypeCreditAlphaNum4(code, issuerPublicKey);
        }
        if (code.length() <= 12) {
            return new AssetTypeCreditAlphaNum12(code, issuerPublicKey);
        }
        throw new IllegalArgumentException("PATH_ASSET_CODE must be 12 characters or fewer.");
    }
}
