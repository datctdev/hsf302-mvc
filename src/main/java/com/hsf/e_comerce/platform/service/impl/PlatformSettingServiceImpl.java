package com.hsf.e_comerce.platform.service.impl;

import com.hsf.e_comerce.common.exception.CustomException;
import com.hsf.e_comerce.platform.entity.PlatformSetting;
import com.hsf.e_comerce.platform.repository.PlatformSettingRepository;
import com.hsf.e_comerce.platform.service.PlatformSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlatformSettingServiceImpl implements PlatformSettingService {

    private final PlatformSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCommissionRate() {
        return repository.findByKey(PlatformSetting.KEY_COMMISSION_RATE)
                .map(s -> {
                    try {
                        return new BigDecimal(s.getValue() != null ? s.getValue().trim() : "0");
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void setCommissionRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CustomException("Tỷ lệ hoa hồng phải từ 0 đến 100.");
        }
        setValue(PlatformSetting.KEY_COMMISSION_RATE, rate.stripTrailingZeros().toPlainString());
    }

    @Override
    @Transactional(readOnly = true)
    public String getValue(String key) {
        return repository.findByKey(key).map(PlatformSetting::getValue).orElse(null);
    }

    @Override
    @Transactional
    public void setValue(String key, String value) {
        PlatformSetting setting = repository.findByKey(key).orElse(new PlatformSetting());
        setting.setKey(key);
        setting.setValue(value != null ? value : "");
        repository.save(setting);
    }
}
