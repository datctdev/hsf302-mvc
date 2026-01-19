package com.hsf.e_comerce.shipping.service;

import com.hsf.e_comerce.shipping.config.GHNConfig;
import com.hsf.e_comerce.shipping.dto.request.GHNCalculateFeeRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNCreateOrderRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNGetOrderDetailRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNCancelOrderRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNGetDistrictsRequest;
import com.hsf.e_comerce.shipping.dto.request.GHNGetWardsRequest;
import com.hsf.e_comerce.shipping.dto.response.GHNCalculateFeeResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNCommonResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNCreateOrderResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNOrderDetailResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNCancelOrderResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNProvinceResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNDistrictResponse;
import com.hsf.e_comerce.shipping.dto.response.GHNWardResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GHNService {

    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;

    /**
     * Tính phí vận chuyển
     */
    public GHNCalculateFeeResponse calculateFee(GHNCalculateFeeRequest request) {
        // Validate config
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            log.error("GHN Token is not configured. Please set GHN_TOKEN environment variable.");
            throw new RuntimeException("GHN Token chưa được cấu hình. Vui lòng set biến môi trường GHN_TOKEN.");
        }
        
        if (ghnConfig.getShopId() == null) {
            log.error("GHN Shop ID is not configured. Please set GHN_SHOP_ID environment variable.");
            throw new RuntimeException("GHN Shop ID chưa được cấu hình. Vui lòng set biến môi trường GHN_SHOP_ID.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/v2/shipping-order/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        log.debug("Calling GHN API: {}", url);
        log.debug("Headers: Token={}, ShopId={}", 
                ghnConfig.getToken() != null ? ghnConfig.getToken().substring(0, Math.min(10, ghnConfig.getToken().length())) + "..." : "null",
                ghnConfig.getShopId());
        log.debug("Request body: {}", request);

        HttpEntity<GHNCalculateFeeRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<GHNCalculateFeeResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<GHNCalculateFeeResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN calculate fee failed: {}", response.getBody());
                throw new RuntimeException("Không thể tính phí vận chuyển: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.error("GHN API 401 Unauthorized. Check your Token and Shop ID.");
            log.error("Token: {}", ghnConfig.getToken() != null ? "Set (length: " + ghnConfig.getToken().length() + ")" : "NULL");
            log.error("Shop ID: {}", ghnConfig.getShopId());
            throw new RuntimeException("Lỗi xác thực GHN API (401). Vui lòng kiểm tra lại Token và Shop ID trong cấu hình.");
        } catch (Exception e) {
            log.error("Error calling GHN calculate fee API", e);
            throw new RuntimeException("Lỗi khi gọi API GHN: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo đơn hàng vận chuyển
     */
    public GHNCreateOrderResponse createOrder(GHNCreateOrderRequest request) {
        // Validate config
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            log.error("GHN Token is not configured.");
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }
        
        if (ghnConfig.getShopId() == null) {
            log.error("GHN Shop ID is not configured.");
            throw new RuntimeException("GHN Shop ID chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/v2/shipping-order/create";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        log.debug("Calling GHN create order API: {}", url);
        log.debug("Request body: {}", request);

        HttpEntity<GHNCreateOrderRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<GHNCreateOrderResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<GHNCreateOrderResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN create order failed: {}", response.getBody());
                throw new RuntimeException("Không thể tạo đơn vận chuyển: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN create order API", e);
            throw new RuntimeException("Lỗi khi tạo đơn vận chuyển: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy chi tiết đơn hàng (by client_order_code)
     */
    public GHNOrderDetailResponse getOrderDetailByClientCode(String clientOrderCode) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }
        
        if (ghnConfig.getShopId() == null) {
            throw new RuntimeException("GHN Shop ID chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/v2/shipping-order/detail-by-client-code";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        GHNGetOrderDetailRequest request = GHNGetOrderDetailRequest.builder()
                .client_order_code(clientOrderCode)
                .build();

        HttpEntity<GHNGetOrderDetailRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<GHNOrderDetailResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<GHNOrderDetailResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN get order detail failed: {}", response.getBody());
                throw new RuntimeException("Không thể lấy chi tiết đơn hàng: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN get order detail API", e);
            throw new RuntimeException("Lỗi khi lấy chi tiết đơn hàng: " + e.getMessage(), e);
        }
    }

    /**
     * Hủy đơn hàng trên GHN
     */
    public GHNCancelOrderResponse cancelOrder(String orderCode) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }
        
        if (ghnConfig.getShopId() == null) {
            throw new RuntimeException("GHN Shop ID chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/v2/shipping-order/cancel";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        GHNCancelOrderRequest request = GHNCancelOrderRequest.builder()
                .order_codes(List.of(orderCode))
                .build();

        HttpEntity<GHNCancelOrderRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<GHNCancelOrderResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<GHNCancelOrderResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN cancel order failed: {}", response.getBody());
                throw new RuntimeException("Không thể hủy đơn vận chuyển: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN cancel order API", e);
            throw new RuntimeException("Lỗi khi hủy đơn vận chuyển: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách Tỉnh/Thành phố
     */
    public List<GHNProvinceResponse> getProvinces() {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/master-data/province";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GHNCommonResponse<List<GHNProvinceResponse>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<List<GHNProvinceResponse>>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN get provinces failed: {}", response.getBody());
                throw new RuntimeException("Không thể lấy danh sách tỉnh/thành: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN get provinces API", e);
            throw new RuntimeException("Lỗi khi lấy danh sách tỉnh/thành: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách Quận/Huyện theo Tỉnh/Thành
     */
    public List<GHNDistrictResponse> getDistricts(Integer provinceId) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/master-data/district";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        GHNGetDistrictsRequest request = GHNGetDistrictsRequest.builder()
                .province_id(provinceId)
                .build();

        HttpEntity<GHNGetDistrictsRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<List<GHNDistrictResponse>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<List<GHNDistrictResponse>>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN get districts failed: {}", response.getBody());
                throw new RuntimeException("Không thể lấy danh sách quận/huyện: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN get districts API", e);
            throw new RuntimeException("Lỗi khi lấy danh sách quận/huyện: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách Phường/Xã theo Quận/Huyện
     */
    public List<GHNWardResponse> getWards(Integer districtId) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
            throw new RuntimeException("GHN Token chưa được cấu hình.");
        }

        String url = ghnConfig.getUrl() + "/shiip/public-api/master-data/ward";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        GHNGetWardsRequest request = GHNGetWardsRequest.builder()
                .district_id(districtId)
                .build();

        HttpEntity<GHNGetWardsRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GHNCommonResponse<List<GHNWardResponse>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<GHNCommonResponse<List<GHNWardResponse>>>() {}
            );

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            } else {
                log.error("GHN get wards failed: {}", response.getBody());
                throw new RuntimeException("Không thể lấy danh sách phường/xã: " + 
                        (response.getBody() != null ? response.getBody().getMessage() : "Unknown error"));
            }
        } catch (Exception e) {
            log.error("Error calling GHN get wards API", e);
            throw new RuntimeException("Lỗi khi lấy danh sách phường/xã: " + e.getMessage(), e);
        }
    }
}
