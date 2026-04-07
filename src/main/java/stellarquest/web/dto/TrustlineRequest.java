package stellarquest.web.dto;

public class TrustlineRequest {
    private String questSecret;
    private String issuerPublicKey;
    private String assetCode;
    private String trustLimit;
    private Boolean autoFundIssuer;
    private Boolean includeBalances;

    public String getQuestSecret() {
        return questSecret;
    }

    public void setQuestSecret(String questSecret) {
        this.questSecret = questSecret;
    }

    public String getIssuerPublicKey() {
        return issuerPublicKey;
    }

    public void setIssuerPublicKey(String issuerPublicKey) {
        this.issuerPublicKey = issuerPublicKey;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getTrustLimit() {
        return trustLimit;
    }

    public void setTrustLimit(String trustLimit) {
        this.trustLimit = trustLimit;
    }

    public Boolean getAutoFundIssuer() {
        return autoFundIssuer;
    }

    public void setAutoFundIssuer(Boolean autoFundIssuer) {
        this.autoFundIssuer = autoFundIssuer;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
