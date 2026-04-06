package stellarquest.quests;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.ChangeTrustAsset;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.operations.ChangeTrustOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;

@Component
public final class TrustlineQuest {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public TrustlineQuest(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public void run(boolean verbose) throws IOException {
        // Flow: quest account creates a trustline to a custom asset issued by another account.
        String secret = config.questSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("QUEST_SECRET is required.");
        }

        KeyPair questKeyPair = KeyPair.fromSecretSeed(secret);
        KeyPair issuerKeyPair = KeyPair.random();

        String issuerPublicKey = config.issuerPublicKey();
        boolean generatedIssuer = false;
        if (issuerPublicKey == null || issuerPublicKey.isBlank()) {
            issuerPublicKey = issuerKeyPair.getAccountId();
            generatedIssuer = true;
        }

        if (client.network().equals(Network.TESTNET) && generatedIssuer) {
            client.fundWithFriendbot(issuerPublicKey);
        }

        AccountResponse questAccount = client.loadAccount(questKeyPair.getAccountId());

        String assetCode = config.assetCode();
        ChangeTrustAsset trustAsset = new ChangeTrustAsset(buildAsset(assetCode, issuerPublicKey));

        if (verbose) {
            System.out.println("Trusting Account: " + questAccount.getAccountId());
            System.out.println("Issuer Account: " + issuerPublicKey);
            System.out.println("Asset Code: " + assetCode);
            printBalances("Trusting Balances (before)", questAccount);
        }

        Transaction transaction = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(
                        ChangeTrustOperation.builder()
                                .asset(trustAsset)
                                .limit(new BigDecimal(config.trustLimit()))
                                .build()
                )
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKeyPair);

        TransactionResponse response = client.submitTransaction(transaction);

        System.out.println("Transaction successful.");
        System.out.println("Hash: " + response.getHash());
        System.out.println("Issuer Public Key: " + issuerPublicKey);
        if (generatedIssuer) {
            System.out.println("Issuer Secret Key: " + new String(issuerKeyPair.getSecretSeed()));
        }
        if (verbose) {
            AccountResponse updatedQuest = client.loadAccount(questKeyPair.getAccountId());
            printBalances("Trusting Balances (after)", updatedQuest);
        }
    }

    private static org.stellar.sdk.Asset buildAsset(String assetCode, String issuerPublicKey) {
        String code = assetCode == null ? "" : assetCode.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("ASSET_CODE must not be empty.");
        }
        if (code.length() <= 4) {
            return new AssetTypeCreditAlphaNum4(code, issuerPublicKey);
        }
        if (code.length() <= 12) {
            return new AssetTypeCreditAlphaNum12(code, issuerPublicKey);
        }
        throw new IllegalArgumentException("ASSET_CODE must be 12 characters or fewer.");
    }

    private static void printBalances(String label, AccountResponse account) {
        System.out.println(label + ":");
        if (account.getBalances() == null || account.getBalances().isEmpty()) {
            System.out.println("  (none)");
            return;
        }
        for (AccountResponse.Balance balance : account.getBalances()) {
            String asset = "native".equals(balance.getAssetType())
                    ? "XLM"
                    : balance.getAssetCode() + ":" + balance.getAssetIssuer();
            System.out.println("  " + asset + " = " + balance.getBalance());
        }
    }
}
