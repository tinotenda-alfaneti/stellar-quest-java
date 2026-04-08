package stellarquest.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.stellar.sdk.Asset;
import org.stellar.sdk.AssetTypeCreditAlphaNum12;
import org.stellar.sdk.AssetTypeCreditAlphaNum4;
import org.stellar.sdk.AssetTypeNative;
import org.stellar.sdk.ChangeTrustAsset;
import org.stellar.sdk.KeyPair;
import org.stellar.sdk.Network;
import org.stellar.sdk.Price;
import org.stellar.sdk.Transaction;
import org.stellar.sdk.TransactionBuilder;
import org.stellar.sdk.operations.AccountMergeOperation;
import org.stellar.sdk.operations.ChangeTrustOperation;
import org.stellar.sdk.operations.CreateAccountOperation;
import org.stellar.sdk.operations.CreatePassiveSellOfferOperation;
import org.stellar.sdk.operations.ManageBuyOfferOperation;
import org.stellar.sdk.operations.ManageDataOperation;
import org.stellar.sdk.operations.ManageSellOfferOperation;
import org.stellar.sdk.operations.PathPaymentStrictSendOperation;
import org.stellar.sdk.operations.PaymentOperation;
import org.stellar.sdk.responses.AccountResponse;
import org.stellar.sdk.responses.TransactionResponse;

import stellarquest.QuestConfig;
import stellarquest.StellarQuestClient;
import stellarquest.web.dto.AccountMergeRequest;
import stellarquest.web.dto.CreateAccountRequest;
import stellarquest.web.dto.FundRequest;
import stellarquest.web.dto.ManageDataRequest;
import stellarquest.web.dto.OfferRequest;
import stellarquest.web.dto.PathPaymentRequest;
import stellarquest.web.dto.PaymentRequest;
import stellarquest.web.dto.TrustlineRequest;

@Service
public final class TransactionService {
    private final StellarQuestClient client;
    private final QuestConfig config;

    public TransactionService(StellarQuestClient client, QuestConfig config) {
        this.client = client;
        this.config = config;
    }

    public Map<String, Object> appConfig() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("network", networkLabel());
        data.put("horizonUrl", config.horizonUrl());
        data.put("friendbotUrl", config.friendbotUrl());
        data.put("defaults", defaultValues());

        String questSecret = config.questSecret();
        if (!isBlank(questSecret)) {
            try {
                data.put("questAccountId", KeyPair.fromSecretSeed(questSecret).getAccountId());
            } catch (Exception ex) {
                data.put("questAccountId", null);
                data.put("questSecretError", "Configured QUEST_SECRET is not valid.");
            }
        } else {
            data.put("questAccountId", null);
        }

        return data;
    }

    public Map<String, Object> loadAccount(String accountId) throws IOException {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required.");
        }

        AccountResponse account = client.loadAccount(accountId.trim());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountId", account.getAccountId());
        data.put("balances", balancesOf(account));
        return data;
    }

    public Map<String, Object> fund(FundRequest request) throws IOException {
        FundRequest safe = request == null ? new FundRequest() : request;

        String accountId = safe.getAccountId();
        if (isBlank(accountId)) {
            String secret = resolveQuestSecret(safe.getQuestSecret());
            accountId = KeyPair.fromSecretSeed(secret).getAccountId();
        }

        String response = client.fundWithFriendbot(accountId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountId", accountId);
        data.put("friendbotResponse", response);
        return data;
    }

    public Map<String, Object> createAccount(CreateAccountRequest request) throws IOException {
        CreateAccountRequest safe = request == null ? new CreateAccountRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);
        KeyPair generatedDestination = KeyPair.random();

        String destination = firstNonBlank(safe.getDestinationPublicKey(), config.destinationPublicKey());
        boolean generated = false;
        if (isBlank(destination)) {
            destination = generatedDestination.getAccountId();
            generated = true;
        }

        String startingBalance = firstNonBlank(safe.getStartingBalance(), config.startingBalance());
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);

        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());

        List<Map<String, String>> sourceBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
        }

        Transaction transaction = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(CreateAccountOperation.builder()
                        .destination(destination)
                        .startingBalance(new BigDecimal(startingBalance))
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "create-account");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("destinationPublicKey", destination);
        data.put("startingBalance", startingBalance);
        data.put("generatedDestination", generated);
        if (generated) {
            data.put("destinationSecretKey", new String(generatedDestination.getSecretSeed()));
        }

        if (includeBalances) {
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("sourceBalancesAfter", balancesOf(client.loadAccount(sourceKey.getAccountId())));
            data.put("destinationBalancesAfter", balancesOf(client.loadAccount(destination)));
        }

        return data;
    }

    public Map<String, Object> payment(PaymentRequest request) throws IOException {
        PaymentRequest safe = request == null ? new PaymentRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);
        KeyPair generatedDestination = KeyPair.random();

        String destination = firstNonBlank(safe.getDestinationPublicKey(), config.paymentDestinationPublicKey());
        boolean generated = false;
        if (isBlank(destination)) {
            destination = generatedDestination.getAccountId();
            generated = true;
        }

        String paymentAmount = firstNonBlank(safe.getPaymentAmount(), config.paymentAmount());
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);
        boolean autoFundDestination = boolOrDefault(safe.getAutoFundDestination(), true);

        String friendbotResponse = null;
        if (generated && autoFundDestination && Network.TESTNET.equals(client.network())) {
            friendbotResponse = client.fundWithFriendbot(destination);
        }

        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());
        List<Map<String, String>> sourceBefore = null;
        List<Map<String, String>> destinationBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
            try {
                destinationBefore = balancesOf(client.loadAccount(destination));
            } catch (IOException ex) {
                // Some destination accounts may not exist before payment.
                destinationBefore = new ArrayList<>();
            }
        }

        Transaction transaction = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(PaymentOperation.builder()
                        .destination(destination)
                        .asset(new AssetTypeNative())
                        .amount(new BigDecimal(paymentAmount))
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "payment");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("destinationPublicKey", destination);
        data.put("paymentAmount", paymentAmount);
        data.put("generatedDestination", generated);
        if (generated) {
            data.put("destinationSecretKey", new String(generatedDestination.getSecretSeed()));
        }
        if (friendbotResponse != null) {
            data.put("friendbotResponse", friendbotResponse);
        }

        if (includeBalances) {
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("destinationBalancesBefore", destinationBefore);
            data.put("sourceBalancesAfter", balancesOf(client.loadAccount(sourceKey.getAccountId())));
            data.put("destinationBalancesAfter", balancesOf(client.loadAccount(destination)));
        }

        return data;
    }

    public Map<String, Object> manageData(ManageDataRequest request) throws IOException {
        ManageDataRequest safe = request == null ? new ManageDataRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);

        String dataName = firstNonBlank(safe.getDataName(), config.manageDataName());
        if (isBlank(dataName)) {
            throw new IllegalArgumentException("dataName is required.");
        }
        if (dataName.getBytes(StandardCharsets.UTF_8).length > 64) {
            throw new IllegalArgumentException("dataName must be 64 bytes or fewer.");
        }

        boolean deleteEntry = boolOrDefault(safe.getDeleteEntry(), false);
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);
        String valueEncoding = normalizeDataEncoding(firstNonBlank(safe.getValueEncoding(), config.manageDataEncoding()));

        byte[] dataValueBytes = null;
        String dataValueBase64 = null;
        String dataValueUtf8 = null;

        if (!deleteEntry) {
            String dataValue = firstNonBlank(safe.getDataValue(), config.manageDataValue());
            if (isBlank(dataValue)) {
                throw new IllegalArgumentException("dataValue is required unless deleteEntry is true.");
            }

            if ("base64".equals(valueEncoding)) {
                try {
                    dataValueBytes = Base64.getDecoder().decode(dataValue.trim());
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("dataValue must be valid base64 when valueEncoding=base64.");
                }
            } else {
                dataValueBytes = dataValue.getBytes(StandardCharsets.UTF_8);
            }

            if (dataValueBytes.length > 64) {
                throw new IllegalArgumentException("dataValue must be 64 bytes or fewer.");
            }

            dataValueBase64 = Base64.getEncoder().encodeToString(dataValueBytes);
            dataValueUtf8 = new String(dataValueBytes, StandardCharsets.UTF_8);
        }

        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());
        List<Map<String, String>> sourceBefore = null;
        List<Map<String, String>> dataEntriesBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
            dataEntriesBefore = dataEntriesOf(sourceAccount);
        }

        Transaction transaction = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(ManageDataOperation.builder()
                        .name(dataName)
                        .value(dataValueBytes)
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "manage-data");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("dataName", dataName);
        data.put("deleteEntry", deleteEntry);
        if (!deleteEntry) {
            data.put("valueEncoding", valueEncoding);
            data.put("dataValueBase64", dataValueBase64);
            data.put("dataValueUtf8", dataValueUtf8);
        }

        if (includeBalances) {
            AccountResponse sourceAfter = client.loadAccount(sourceKey.getAccountId());
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("sourceBalancesAfter", balancesOf(sourceAfter));
            data.put("dataEntriesBefore", dataEntriesBefore);
            data.put("dataEntriesAfter", dataEntriesOf(sourceAfter));
        }

        return data;
    }

    public Map<String, Object> accountMerge(AccountMergeRequest request) throws IOException {
        AccountMergeRequest safe = request == null ? new AccountMergeRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);
        KeyPair generatedDestination = KeyPair.random();

        String destination = firstNonBlank(
                safe.getDestinationPublicKey(),
                config.mergeDestinationPublicKey(),
                config.paymentDestinationPublicKey(),
                config.destinationPublicKey());

        boolean generated = false;
        if (isBlank(destination)) {
            destination = generatedDestination.getAccountId();
            generated = true;
        }

        if (sourceKey.getAccountId().equals(destination)) {
            throw new IllegalArgumentException("destinationPublicKey must be different from the source account.");
        }

        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);
        boolean autoFundDestination = boolOrDefault(safe.getAutoFundDestination(), true);

        String friendbotResponse = null;
        if (autoFundDestination && Network.TESTNET.equals(client.network()) && (generated || !accountExists(destination))) {
            friendbotResponse = client.fundWithFriendbot(destination);
        }

        if (!accountExists(sourceKey.getAccountId())) {
            throw new IllegalArgumentException(
                    "Source account was not found. It may already be merged/deleted; fund it again on testnet or use a different questSecret.");
        }
        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());
        if (sourceAccount.getSubentryCount() != null && sourceAccount.getSubentryCount() > 0) {
            throw new IllegalArgumentException(
                    "Source account has subentries (" + sourceAccount.getSubentryCount()
                            + "). Remove trustlines, offers, extra signers, and data entries before merging.");
        }
        if (!accountExists(destination)) {
            throw new IllegalArgumentException(
                    "destinationPublicKey account was not found. Provide an existing account or enable autoFundDestination on testnet.");
        }

        List<Map<String, String>> sourceBefore = null;
        List<Map<String, String>> destinationBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
            destinationBefore = loadBalancesOrEmpty(destination);
        }

        Transaction transaction = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(AccountMergeOperation.builder()
                        .destination(destination)
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "account-merge");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("destinationPublicKey", destination);
        data.put("generatedDestination", generated);
        if (generated) {
            data.put("destinationSecretKey", new String(generatedDestination.getSecretSeed()));
        }
        if (friendbotResponse != null) {
            data.put("friendbotResponse", friendbotResponse);
        }

        if (includeBalances) {
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("destinationBalancesBefore", destinationBefore);
            data.put("sourceAccountExistsAfter", accountExists(sourceKey.getAccountId()));
            data.put("destinationBalancesAfter", loadBalancesOrEmpty(destination));
        }

        return data;
    }

    public Map<String, Object> trustline(TrustlineRequest request) throws IOException {
        TrustlineRequest safe = request == null ? new TrustlineRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);
        KeyPair generatedIssuer = KeyPair.random();

        String issuerPublicKey = firstNonBlank(safe.getIssuerPublicKey(), config.issuerPublicKey());
        boolean generated = false;
        if (isBlank(issuerPublicKey)) {
            issuerPublicKey = generatedIssuer.getAccountId();
            generated = true;
        }

        boolean autoFundIssuer = boolOrDefault(safe.getAutoFundIssuer(), true);
        String friendbotResponse = null;
        if (generated && autoFundIssuer && Network.TESTNET.equals(client.network())) {
            friendbotResponse = client.fundWithFriendbot(issuerPublicKey);
        }

        String assetCode = firstNonBlank(safe.getAssetCode(), config.assetCode());
        String trustLimit = firstNonBlank(safe.getTrustLimit(), config.trustLimit());
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);

        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());
        List<Map<String, String>> sourceBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
        }

        ChangeTrustAsset trustAsset = new ChangeTrustAsset(buildAsset(assetCode, issuerPublicKey));

        Transaction transaction = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(ChangeTrustOperation.builder()
                        .asset(trustAsset)
                        .limit(new BigDecimal(trustLimit))
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "trustline");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("assetCode", assetCode);
        data.put("issuerPublicKey", issuerPublicKey);
        data.put("trustLimit", trustLimit);
        data.put("generatedIssuer", generated);
        if (generated) {
            data.put("issuerSecretKey", new String(generatedIssuer.getSecretSeed()));
        }
        if (friendbotResponse != null) {
            data.put("friendbotResponse", friendbotResponse);
        }

        if (includeBalances) {
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("sourceBalancesAfter", balancesOf(client.loadAccount(sourceKey.getAccountId())));
        }

        return data;
    }

    public Map<String, Object> offer(OfferRequest request) throws IOException {
        OfferRequest safe = request == null ? new OfferRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair sourceKey = KeyPair.fromSecretSeed(secret);
        AccountResponse sourceAccount = client.loadAccount(sourceKey.getAccountId());
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);

        String offerType = normalizeOfferType(firstNonBlank(safe.getOfferType(), config.offerType()));
        String offerAssetCode = firstNonBlank(safe.getOfferAssetCode(), config.offerAssetCode());
        String offerAssetIssuer = firstNonBlank(safe.getOfferAssetIssuer(), config.offerAssetIssuer());
        String offerPrice = firstNonBlank(safe.getOfferPrice(), config.offerPrice());
        String offerAmount = firstNonBlank(safe.getOfferAmount(), config.offerAmount());
        String offerBuyAmount = firstNonBlank(safe.getOfferBuyAmount(), config.offerBuyAmount());
        Long requestOfferId = safe.getOfferId();
        long offerId = requestOfferId == null ? config.offerId() : requestOfferId;
        String offerTrustLimit = firstNonBlank(safe.getOfferTrustLimit(), config.offerTrustLimit());
        if (isBlank(offerTrustLimit)) {
            offerTrustLimit = "922337203685.4775807";
        }

        Asset counterAsset = buildAsset(offerAssetCode, offerAssetIssuer);
        ChangeTrustAsset trustAsset = new ChangeTrustAsset(counterAsset);
        Price price = Price.fromString(offerPrice);
        BigDecimal sellAmount = new BigDecimal(offerAmount);
        BigDecimal buyAmount = new BigDecimal(offerBuyAmount);

        List<Map<String, String>> sourceBefore = null;
        if (includeBalances) {
            sourceBefore = balancesOf(sourceAccount);
        }

        TransactionBuilder builder = new TransactionBuilder(sourceAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(ChangeTrustOperation.builder()
                        .asset(trustAsset)
                        .limit(new BigDecimal(offerTrustLimit))
                        .build());

        if ("buy".equals(offerType)) {
            builder.addOperation(ManageBuyOfferOperation.builder()
                    .selling(new AssetTypeNative())
                    .buying(counterAsset)
                    .amount(buyAmount)
                    .price(price)
                    .offerId(offerId)
                    .build());
        } else if ("passive".equals(offerType)) {
            builder.addOperation(CreatePassiveSellOfferOperation.builder()
                    .selling(new AssetTypeNative())
                    .buying(counterAsset)
                    .amount(sellAmount)
                    .price(price)
                    .build());
        } else {
            builder.addOperation(ManageSellOfferOperation.builder()
                    .selling(new AssetTypeNative())
                    .buying(counterAsset)
                    .amount(sellAmount)
                    .price(price)
                    .offerId(offerId)
                    .build());
        }

        Transaction transaction = builder
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(sourceKey);
        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "offer");
        data.put("hash", tx.getHash());
        data.put("sourceAccount", sourceKey.getAccountId());
        data.put("offerType", offerType);
        data.put("offerAssetCode", offerAssetCode);
        data.put("offerAssetIssuer", offerAssetIssuer);
        data.put("offerPrice", offerPrice);
        data.put("offerAmount", "buy".equals(offerType) ? offerBuyAmount : offerAmount);
        data.put("offerId", offerId);

        if (includeBalances) {
            data.put("sourceBalancesBefore", sourceBefore);
            data.put("sourceBalancesAfter", balancesOf(client.loadAccount(sourceKey.getAccountId())));
        }

        return data;
    }

    public Map<String, Object> pathPayment(PathPaymentRequest request) throws IOException {
        PathPaymentRequest safe = request == null ? new PathPaymentRequest() : request;

        String secret = resolveQuestSecret(safe.getQuestSecret());
        KeyPair questKey = KeyPair.fromSecretSeed(secret);
        KeyPair issuerKey = KeyPair.random();
        KeyPair distributorKey = KeyPair.random();
        KeyPair destinationKey = KeyPair.random();

        String pathAssetCode = firstNonBlank(safe.getPathAssetCode(), config.pathAssetCode());
        String pathSendAmount = firstNonBlank(safe.getPathSendAmount(), config.pathSendAmount());
        String pathDestMin = firstNonBlank(safe.getPathDestMin(), config.pathDestMin());
        boolean autoFundAccounts = boolOrDefault(safe.getAutoFundAccounts(), true);
        boolean includeBalances = boolOrDefault(safe.getIncludeBalances(), true);

        Map<String, String> friendbotResponses = new LinkedHashMap<>();
        if (autoFundAccounts && Network.TESTNET.equals(client.network())) {
            friendbotResponses.put("questAccount", client.fundWithFriendbot(questKey.getAccountId()));
            friendbotResponses.put("issuerAccount", client.fundWithFriendbot(issuerKey.getAccountId()));
            friendbotResponses.put("distributorAccount", client.fundWithFriendbot(distributorKey.getAccountId()));
            friendbotResponses.put("destinationAccount", client.fundWithFriendbot(destinationKey.getAccountId()));
        }

        Asset pathAsset = buildAsset(pathAssetCode, issuerKey.getAccountId());
        ChangeTrustAsset trustAsset = new ChangeTrustAsset(pathAsset);
        AccountResponse questAccount = client.loadAccount(questKey.getAccountId());

        List<Map<String, String>> questBefore = null;
        List<Map<String, String>> distributorBefore = null;
        List<Map<String, String>> destinationBefore = null;
        if (includeBalances) {
            questBefore = balancesOf(questAccount);
            distributorBefore = loadBalancesOrEmpty(distributorKey.getAccountId());
            destinationBefore = loadBalancesOrEmpty(destinationKey.getAccountId());
        }

        Transaction transaction = new TransactionBuilder(questAccount, client.network())
                .setBaseFee(client.baseFee())
                .addOperation(ChangeTrustOperation.builder()
                        .asset(trustAsset)
                        .limit(new BigDecimal("922337203685.4775807"))
                        .sourceAccount(destinationKey.getAccountId())
                        .build())
                .addOperation(ChangeTrustOperation.builder()
                        .asset(trustAsset)
                        .limit(new BigDecimal("922337203685.4775807"))
                        .sourceAccount(distributorKey.getAccountId())
                        .build())
                .addOperation(PaymentOperation.builder()
                        .destination(distributorKey.getAccountId())
                        .asset(pathAsset)
                        .amount(new BigDecimal("1000000"))
                        .sourceAccount(issuerKey.getAccountId())
                        .build())
                .addOperation(CreatePassiveSellOfferOperation.builder()
                        .selling(pathAsset)
                        .buying(new AssetTypeNative())
                        .amount(new BigDecimal("2000"))
                        .price(Price.fromString("1"))
                        .sourceAccount(distributorKey.getAccountId())
                        .build())
                .addOperation(CreatePassiveSellOfferOperation.builder()
                        .selling(new AssetTypeNative())
                        .buying(pathAsset)
                        .amount(new BigDecimal("2000"))
                        .price(Price.fromString("1"))
                        .sourceAccount(distributorKey.getAccountId())
                        .build())
                .addOperation(PathPaymentStrictSendOperation.builder()
                        .sendAsset(new AssetTypeNative())
                        .sendAmount(new BigDecimal(pathSendAmount))
                        .destination(destinationKey.getAccountId())
                        .destAsset(pathAsset)
                        .destMin(new BigDecimal(pathDestMin))
                        .build())
                .setTimeout(client.timeoutSeconds())
                .build();

        transaction.sign(questKey);
        transaction.sign(issuerKey);
        transaction.sign(distributorKey);
        transaction.sign(destinationKey);

        TransactionResponse tx = client.submitTransaction(transaction);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", "path-payment");
        data.put("hash", tx.getHash());
        data.put("questAccount", questKey.getAccountId());
        data.put("issuerAccount", issuerKey.getAccountId());
        data.put("issuerSecretKey", new String(issuerKey.getSecretSeed()));
        data.put("distributorAccount", distributorKey.getAccountId());
        data.put("distributorSecretKey", new String(distributorKey.getSecretSeed()));
        data.put("destinationAccount", destinationKey.getAccountId());
        data.put("destinationSecretKey", new String(destinationKey.getSecretSeed()));
        data.put("pathAssetCode", pathAssetCode);
        data.put("pathAssetIssuer", issuerKey.getAccountId());
        data.put("pathSendAmount", pathSendAmount);
        data.put("pathDestMin", pathDestMin);
        if (!friendbotResponses.isEmpty()) {
            data.put("friendbotResponses", friendbotResponses);
        }

        if (includeBalances) {
            data.put("questBalancesBefore", questBefore);
            data.put("distributorBalancesBefore", distributorBefore);
            data.put("destinationBalancesBefore", destinationBefore);
            data.put("questBalancesAfter", loadBalancesOrEmpty(questKey.getAccountId()));
            data.put("distributorBalancesAfter", loadBalancesOrEmpty(distributorKey.getAccountId()));
            data.put("destinationBalancesAfter", loadBalancesOrEmpty(destinationKey.getAccountId()));
        }

        return data;
    }

    private Map<String, String> defaultValues() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("startingBalance", config.startingBalance());
        defaults.put("paymentAmount", config.paymentAmount());
        defaults.put("manageDataName", config.manageDataName());
        defaults.put("manageDataValue", config.manageDataValue());
        defaults.put("manageDataEncoding", config.manageDataEncoding());
        defaults.put("mergeDestinationPublicKey", config.mergeDestinationPublicKey());
        defaults.put("assetCode", config.assetCode());
        defaults.put("trustLimit", config.trustLimit());
        defaults.put("offerType", config.offerType());
        defaults.put("offerAssetCode", config.offerAssetCode());
        defaults.put("offerAssetIssuer", config.offerAssetIssuer());
        defaults.put("offerPrice", config.offerPrice());
        defaults.put("offerAmount", config.offerAmount());
        defaults.put("offerBuyAmount", config.offerBuyAmount());
        defaults.put("offerId", String.valueOf(config.offerId()));
        defaults.put("offerTrustLimit", config.offerTrustLimit());
        defaults.put("pathAssetCode", config.pathAssetCode());
        defaults.put("pathSendAmount", config.pathSendAmount());
        defaults.put("pathDestMin", config.pathDestMin());
        return defaults;
    }

    private String resolveQuestSecret(String requestSecret) {
        String secret = firstNonBlank(requestSecret, config.questSecret());
        if (isBlank(secret)) {
            throw new IllegalArgumentException("questSecret is required. Set QUEST_SECRET or pass questSecret in the request body.");
        }
        return secret;
    }

    private static List<Map<String, String>> balancesOf(AccountResponse account) {
        List<Map<String, String>> balances = new ArrayList<>();
        if (account.getBalances() == null) {
            return balances;
        }

        for (AccountResponse.Balance balance : account.getBalances()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("assetType", balance.getAssetType());
            item.put("assetCode", balance.getAssetCode());
            item.put("assetIssuer", balance.getAssetIssuer());
            item.put("balance", balance.getBalance());
            item.put("label", "native".equals(balance.getAssetType())
                    ? "XLM"
                    : balance.getAssetCode() + ":" + balance.getAssetIssuer());
            balances.add(item);
        }

        return balances;
    }

    private static List<Map<String, String>> dataEntriesOf(AccountResponse account) {
        List<Map<String, String>> entries = new ArrayList<>();
        if (account.getData() == null || account.getData().isEmpty()) {
            return entries;
        }

        List<String> keys = new ArrayList<>(account.getData().keySet());
        Collections.sort(keys);
        for (String key : keys) {
            String valueBase64 = account.getData().get(key);
            Map<String, String> item = new LinkedHashMap<>();
            item.put("name", key);
            item.put("valueBase64", valueBase64);
            try {
                byte[] valueDecoded = account.getData().getDecoded(key);
                if (valueDecoded != null) {
                    item.put("valueUtf8", new String(valueDecoded, StandardCharsets.UTF_8));
                }
            } catch (Exception ex) {
                // Keep base64 value if decoding fails.
            }
            entries.add(item);
        }

        return entries;
    }

    private static Asset buildAsset(String assetCode, String issuerPublicKey) {
        String code = assetCode == null ? "" : assetCode.trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("assetCode must not be empty.");
        }
        if (issuerPublicKey == null || issuerPublicKey.isBlank()) {
            throw new IllegalArgumentException("issuerPublicKey must not be empty.");
        }
        if (code.length() <= 4) {
            return new AssetTypeCreditAlphaNum4(code, issuerPublicKey);
        }
        if (code.length() <= 12) {
            return new AssetTypeCreditAlphaNum12(code, issuerPublicKey);
        }
        throw new IllegalArgumentException("assetCode must be 12 characters or fewer.");
    }

    private String networkLabel() {
        if (Network.PUBLIC.equals(client.network())) {
            return "public";
        }
        return "testnet";
    }

    private List<Map<String, String>> loadBalancesOrEmpty(String accountId) {
        try {
            return balancesOf(client.loadAccount(accountId));
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    private boolean accountExists(String accountId) {
        try {
            client.loadAccount(accountId);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static String normalizeOfferType(String value) {
        if (value == null) {
            return "sell";
        }
        String normalized = value.trim().toLowerCase();
        if ("buy".equals(normalized) || "sell".equals(normalized) || "passive".equals(normalized)) {
            return normalized;
        }
        return "sell";
    }

    private static String normalizeDataEncoding(String value) {
        if (value == null) {
            return "utf8";
        }
        String normalized = value.trim().toLowerCase();
        if ("base64".equals(normalized)) {
            return "base64";
        }
        return "utf8";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean boolOrDefault(Boolean value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
