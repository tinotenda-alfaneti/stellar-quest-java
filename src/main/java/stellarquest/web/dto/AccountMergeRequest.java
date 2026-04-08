package stellarquest.web.dto;

public class AccountMergeRequest {
    private String questSecret;
    private String destinationPublicKey;
    private Boolean autoFundDestination;
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

    public Boolean getAutoFundDestination() {
        return autoFundDestination;
    }

    public void setAutoFundDestination(Boolean autoFundDestination) {
        this.autoFundDestination = autoFundDestination;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
