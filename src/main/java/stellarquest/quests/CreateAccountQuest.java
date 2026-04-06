package stellarquest.quests;

import java.io.IOException;
import java.math.BigDecimal;

import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.TransactionBuilderAccount;
import org.stellar.sdk.responses.TransactionResponse;
import org.stellar.sdk.operations.CreateAccountOperation;

import org.springframework.stereotype.Component;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;

@Component
public final class CreateAccountQuest {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public CreateAccountQuest(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public void run() throws IOException {
        KeyPair questKeyPair = KeyPair.fromSecretSeed(config.questSecret());
        KeyPair newKeyPair = KeyPair.random();

        String destination = config.destinationPublicKey();
        if (destination == null || destination.isBlank()) {
            destination = newKeyPair.getAccountId();
        }

        TransactionBuilderAccount questAccount = client.loadAccount(questKeyPair.getAccountId());

        Transaction transaction = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(
                        CreateAccountOperation.builder()
                                .destination(destination)
                                .startingBalance(new BigDecimal(config.startingBalance()))
                                .build()
                )
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKeyPair);

        TransactionResponse response = client.submitTransaction(transaction);

        System.out.println("Transaction successful.");
        System.out.println("Hash: " + response.getHash());
        System.out.println("Destination Public Key: " + destination);
        if (config.destinationPublicKey() == null || config.destinationPublicKey().isBlank()) {
            System.out.println("Destination Secret Key: " + newKeyPair.getSecretSeed());
        }
    }
}
