package stellarquest.web.dto;

public class CreateAccountRequest {
    private String questSecret;
    private String destinationPublicKey;
    private String startingBalance;
    private Boolean includeBalances;

    public String getQuestSecret() {
        return questSecret;
    }

    public void setQuestSecret(String questSecret) {
        this.questSecret = questSecret;
    }

    public String getDestinationPublicKey() {
        return destinationPublicKey;
    }

    public void setDestinationPublicKey(String destinationPublicKey) {
        this.destinationPublicKey = destinationPublicKey;
    }

    public String getStartingBalance() {
        return startingBalance;
    }

    public void setStartingBalance(String startingBalance) {
        this.startingBalance = startingBalance;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
