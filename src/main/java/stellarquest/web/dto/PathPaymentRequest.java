package stellarquest.web.dto;

public class PathPaymentRequest {
    private String questSecret;
    private String pathAssetCode;
    private String pathSendAmount;
    private String pathDestMin;
    private Boolean autoFundAccounts;
    private Boolean includeBalances;

    public String getQuestSecret() {
        return questSecret;
    }

    public void setQuestSecret(String questSecret) {
        this.questSecret = questSecret;
    }

    public String getPathAssetCode() {
        return pathAssetCode;
    }

    public void setPathAssetCode(String pathAssetCode) {
        this.pathAssetCode = pathAssetCode;
    }

    public String getPathSendAmount() {
        return pathSendAmount;
    }

    public void setPathSendAmount(String pathSendAmount) {
        this.pathSendAmount = pathSendAmount;
    }

    public String getPathDestMin() {
        return pathDestMin;
    }

    public void setPathDestMin(String pathDestMin) {
        this.pathDestMin = pathDestMin;
    }

    public Boolean getAutoFundAccounts() {
        return autoFundAccounts;
    }

    public void setAutoFundAccounts(Boolean autoFundAccounts) {
        this.autoFundAccounts = autoFundAccounts;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
