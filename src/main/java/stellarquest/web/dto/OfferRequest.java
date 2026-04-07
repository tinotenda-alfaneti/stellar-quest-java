package stellarquest.web.dto;

public class OfferRequest {
    private String questSecret;
    private String offerType;
    private String offerAssetCode;
    private String offerAssetIssuer;
    private String offerPrice;
    private String offerAmount;
    private String offerBuyAmount;
    private Long offerId;
    private String offerTrustLimit;
    private Boolean includeBalances;

    public String getQuestSecret() {
        return questSecret;
    }

    public void setQuestSecret(String questSecret) {
        this.questSecret = questSecret;
    }

    public String getOfferType() {
        return offerType;
    }

    public void setOfferType(String offerType) {
        this.offerType = offerType;
    }

    public String getOfferAssetCode() {
        return offerAssetCode;
    }

    public void setOfferAssetCode(String offerAssetCode) {
        this.offerAssetCode = offerAssetCode;
    }

    public String getOfferAssetIssuer() {
        return offerAssetIssuer;
    }

    public void setOfferAssetIssuer(String offerAssetIssuer) {
        this.offerAssetIssuer = offerAssetIssuer;
    }

    public String getOfferPrice() {
        return offerPrice;
    }

    public void setOfferPrice(String offerPrice) {
        this.offerPrice = offerPrice;
    }

    public String getOfferAmount() {
        return offerAmount;
    }

    public void setOfferAmount(String offerAmount) {
        this.offerAmount = offerAmount;
    }

    public String getOfferBuyAmount() {
        return offerBuyAmount;
    }

    public void setOfferBuyAmount(String offerBuyAmount) {
        this.offerBuyAmount = offerBuyAmount;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getOfferTrustLimit() {
        return offerTrustLimit;
    }

    public void setOfferTrustLimit(String offerTrustLimit) {
        this.offerTrustLimit = offerTrustLimit;
    }

    public Boolean getIncludeBalances() {
        return includeBalances;
    }

    public void setIncludeBalances(Boolean includeBalances) {
        this.includeBalances = includeBalances;
    }
}
