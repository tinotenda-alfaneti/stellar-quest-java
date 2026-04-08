package stellarquest.web.dto;

public class ManageDataRequest {
    private String questSecret;
    private String dataName;
    private String dataValue;
    private String valueEncoding;
    private Boolean deleteEntry;
    private Boolean includeBalances;

    public String getQuestSecret() {
        return questSecret;
    }

    public void setQuestSecret(String questSecret) {
        this.questSecret = questSecret;
    }

    public String getDataName() {
        return dataName;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    public String getValueEncoding() {
        return valueEncoding;
    }

    public void setValueEncoding(String valueEncoding) {
        this.valueEncoding = valueEncoding;
    }

    public Boolean getDeleteEntry() {
        return deleteEntry;
    }

    public void setDeleteEntry(Boolean deleteEntry) {
        this.deleteEntry = deleteEntry;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
