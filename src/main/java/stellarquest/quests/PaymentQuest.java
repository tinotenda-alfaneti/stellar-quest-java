package stellarquest.quests;

import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.responses.TransactionResponse;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;

@Component
public final class PaymentQuest {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public PaymentQuest(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public void run() throws IOException {
        String secret = config.questSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("QUEST_SECRET is required.");
        }

        KeyPair questKeyPair = KeyPair.fromSecretSeed(secret);
        KeyPair destinationKeyPair = KeyPair.random();

        String destination = config.paymentDestinationPublicKey();
        boolean generatedDestination = false;
        if (destination == null || destination.isBlank()) {
            destination = destinationKeyPair.getAccountId();
            generatedDestination = true;
        }

        if (client.network().equals(Network.TESTNET) && generatedDestination) {
            client.fundWithFriendbot(destination);
        }

        TransactionBuilderAccount questAccount = client.loadAccount(questKeyPair.getAccountId());

        Transaction transaction = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(
                        PaymentOperation.builder()
                                .destination(destination)
                                .asset(new AssetTypeNative())
                                .amount(new BigDecimal(config.paymentAmount()))
                                .build()
                )
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKeyPair);

        TransactionResponse response = client.submitTransaction(transaction);

        System.out.println("Transaction successful.");
        System.out.println("Hash: " + response.getHash());
        System.out.println("Destination Public Key: " + destination);
        if (generatedDestination) {
            System.out.println("Destination Secret Key: " + destinationKeyPair.getSecretSeed());
        }
    }
}
