package stellarquest;

import stellarquest.quests.CreateAccountQuest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.stellar.sdk.KeyPair;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner commandLineRunner(
            QuestConfig config,
            CreateAccountQuest createAccountQuest,
            StellarQuestClient client
    ) {
        return args -> {
            if (args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]) || "help".equals(args[0]))) {
                printUsage();
                return;
            }

            String command = args.length == 0 ? "create-account" : args[0];

            if ("fund".equals(command)) {
                String accountId = args.length > 1 ? args[1] : null;
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
                createAccountQuest.run();
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
        System.out.println("  fund [ACCOUNT]   Fund ACCOUNT via friendbot (defaults to QUEST_SECRET)");
        System.out.println("\nEnvironment:");
        System.out.println("  QUEST_SECRET          Quest account secret seed (required for create-account)");
        System.out.println("  HORIZON_URL           Default: https://horizon-testnet.stellar.org");
        System.out.println("  FRIENDBOT_URL         Default: https://friendbot.stellar.org");
        System.out.println("  NETWORK               testnet (default) | public");
        System.out.println("  STARTING_BALANCE      Default: 1000");
        System.out.println("  BASE_FEE              Default: 100");
        System.out.println("  TIMEOUT_SECONDS       Default: 30");
        System.out.println("  DESTINATION_PUBLIC_KEY Optional override destination account");
    }
}
