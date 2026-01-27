package com.hsf.e_comerce.platform.service;

import java.math.BigDecimal;

public interface PlatformSettingService {

    /**
     * Get commission rate in percent (e.g. 10 means 10%).
     * Default 0 if not set.
     */
    BigDecimal getCommissionRate();

    /**
     * Set commission rate in percent (0–100).
     */
    void setCommissionRate(BigDecimal rate);

    /**
     * Get value by key. Returns null if not found.
     */
    String getValue(String key);

    /**
     * Set value by key. Creates or updates.
     */
    void setValue(String key, String value);
}
