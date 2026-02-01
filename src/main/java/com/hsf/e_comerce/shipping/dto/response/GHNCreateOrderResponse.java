package com.hsf.e_comerce.shipping.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GHNCreateOrderResponse {

    /** Mã vận đơn GHN – dùng để lưu vào order.ghnOrderCode */
    @JsonProperty("OrderCode")
    private String order_code;

    @JsonProperty("TotalFee")
    private Integer total_fee;

    @JsonProperty("Time")
    private String time;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Fee")
    private GHNFeeDetail fee;

    @JsonProperty("SortCode")
    private String sort_code;

    @JsonProperty("TransType")
    private String trans_type;

    @JsonProperty("WardEncode")
    private String ward_encode;

    @JsonProperty("DistrictEncode")
    private String district_encode;

    /** Chi tiết phí (GHN trả về object, không phải số). */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GHNFeeDetail {
        @JsonProperty("Total")
        private Integer total;
        @JsonProperty("MainService")
        private Integer mainService;
        @JsonProperty("Insurance")
        private Integer insurance;
        @JsonProperty("StationDO")
        private Integer stationDO;
        @JsonProperty("StationPU")
        private Integer stationPU;
        @JsonProperty("CODFee")
        private Integer codFee;
        @JsonProperty("Coupon")
        private Integer coupon;
    }
}
