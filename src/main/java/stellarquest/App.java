package stellarquest;

import stellarquest.quests.CreateAccountQuest;
import stellarquest.quests.PaymentQuest;
import stellarquest.quests.TrustlineQuest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.stellar.sdk.KeyPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner commandLineRunner(
            QuestConfig config,
            CreateAccountQuest createAccountQuest,
            PaymentQuest paymentQuest,
            TrustlineQuest trustlineQuest,
            StellarQuestClient client
    ) {
        return args -> {
            List<String> argList = new ArrayList<>(Arrays.asList(args));
            boolean verbose = argList.remove("--verbose") || argList.remove("-v");

            if (!argList.isEmpty() && ("-h".equals(argList.get(0)) || "--help".equals(argList.get(0)) || "help".equals(argList.get(0)))) {
                printUsage();
                return;
            }

            String command = argList.isEmpty() ? "create-account" : argList.get(0);

            if ("fund".equals(command)) {
                String accountId = argList.size() > 1 ? argList.get(1) : null;
                if (accountId == null || accountId.isBlank()) {
                    String secret = requireQuestSecret(config);
                    KeyPair questKeyPair = KeyPair.fromSecretSeed(secret);
                    accountId = questKeyPair.getAccountId();
                }

                String response = client.fundWithFriendbot(accountId);
                System.out.println("Friendbot response:");
                System.out.println(response);
                return;
            }

            if ("create-account".equals(command)) {
                requireQuestSecret(config);
                createAccountQuest.run(verbose);
                return;
            }

            if ("payment".equals(command)) {
                requireQuestSecret(config);
                paymentQuest.run(verbose);
                return;
            }

            if ("trustline".equals(command)) {
                requireQuestSecret(config);
                trustlineQuest.run(verbose);
                return;
            }

            System.err.println("Unknown command: " + command);
            printUsage();
        };
    }

    private static String requireQuestSecret(QuestConfig config) {
        String secret = config.questSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("QUEST_SECRET is required.");
        }
        return secret;
    }

    private static void printUsage() {
        System.out.println("Stellar Quest Java (Spring Boot)");
        System.out.println("\nCommands:");
        System.out.println("  create-account   Create and fund a new account from QUEST_SECRET");
        System.out.println("  payment          Send a native XLM payment from QUEST_SECRET");
        System.out.println("  trustline        Create a trustline from QUEST_SECRET to an issuer");
        System.out.println("  fund [ACCOUNT]   Fund ACCOUNT via friendbot (defaults to QUEST_SECRET)");
        System.out.println("\nOptions:");
        System.out.println("  -v, --verbose    Print account balances before and after");
        System.out.println("\nEnvironment:");
        System.out.println("  QUEST_SECRET          Quest account secret seed (required for create-account)");
        System.out.println("  HORIZON_URL           Default: https://horizon-testnet.stellar.org");
        System.out.println("  FRIENDBOT_URL         Default: https://friendbot.stellar.org");
        System.out.println("  NETWORK               testnet (default) | public");
        System.out.println("  STARTING_BALANCE      Default: 1000");
        System.out.println("  PAYMENT_AMOUNT        Default: 100");
        System.out.println("  ASSET_CODE            Default: SANTA");
        System.out.println("  TRUST_LIMIT           Default: 100");
        System.out.println("  BASE_FEE              Default: 100");
        System.out.println("  TIMEOUT_SECONDS       Default: 30");
        System.out.println("  DESTINATION_PUBLIC_KEY Optional override destination account");
        System.out.println("  PAYMENT_DESTINATION_PUBLIC_KEY Optional override payment destination");
        System.out.println("  ISSUER_PUBLIC_KEY     Optional override issuer account");
    }
}
