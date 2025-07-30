package com.sky.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BaiduSDK {

    @Value("${sky.baidu.ak}")
    private String ak;
    @Value("${sky.baidu.geocidubg-url}")
    private String geocidubgUrl;
    @Value("${sky.baidu.planning-url}")
    private String planning_url;

    /**
     * 根据地址获取经纬度坐标
     * @param address 详细地址文字
     * @return "lat,lng" 格式的字符串，例如 "116.307852,40.057031"，获取失败返回 null
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public String getCoordinates(String address) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String jsonResult = restTemplate.getForObject(geocidubgUrl, String.class, address, ak);
            JsonNode rootNode = objectMapper.readTree(jsonResult);
            if (rootNode.path("status").asInt() == 0) {
                JsonNode locationNode = rootNode.path("result").path("location");
                double lng = locationNode.path("lng").asDouble();
                double lat = locationNode.path("lat").asDouble();
                // 2. 修正返回格式为 "纬度,经度"
                return lat + "," + lng;
            }
        } catch (JsonProcessingException | RestClientException e) { e.printStackTrace(); }
        return null;
    }

    /**
     * 计算两点之间的骑行距离
     * @param origin      起点坐标，格式 "lat,lng"
     * @param destination 终点坐标，格式 "lat,lng"
     * @return 距离（单位：米），获取失败返回 -1
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public long getRidingDistance(String origin, String destination) {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String jsonResult = restTemplate.getForObject(planning_url, String.class, origin, destination, ak);

            JsonNode rootNode = objectMapper.readTree(jsonResult);
            int status = rootNode.path("status").asInt();

            if (status == 0) { // 成功
                // 结果中可能有多条路线，我们取第一条
                JsonNode routeNode = rootNode.path("result").path("routes").get(0);
                if (routeNode != null) {
                    return routeNode.path("distance").asLong(); // distance 单位是米
                }
            } else {
                System.err.println("路线规划失败: " + rootNode.path("message").asText());
            }
        } catch (JsonProcessingException | RestClientException e) {
            e.printStackTrace();
        }
        return -1L; // 返回一个特殊值表示失败
    }

    public String getAK() {
        return ak;
    }

}
