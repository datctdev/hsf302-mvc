package com.hsf.e_comerce.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Payload webhook GHN – callback trạng thái đơn hàng.
 * GHN gửi POST JSON, Type: Create | Switch_status | Update_weight | Update_cod | Update_fee.
 * Trường theo doc: ClientOrderCode, OrderCode, Status, Type, …
 * Khách hàng luôn trả về 200.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GhnWebhookPayload {

    @JsonProperty("OrderCode")
    private String orderCode;

    @JsonProperty("ClientOrderCode")
    private String clientOrderCode;

    @JsonProperty("Status")
    private String deliveryStatus;

    @JsonProperty("Type")
    private String webhookType;

    @JsonProperty("StatusId")
    private Integer deliveryStatusId;

    @JsonProperty("partner_id")
    private String partnerId;

    @JsonProperty("label_id")
    private String labelId;
}
