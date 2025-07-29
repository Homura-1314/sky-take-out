package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.baidu")
@Data
public class Baidu {
    private String shop;
    private String ak;
    private String geocidubg_url;
    private long Max_delivery;
    private String SHOP_COORDINATES;
    private String planning_url;
}
