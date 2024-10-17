package org.cookieandkakao.babting.domain.calendar.service;


import java.net.URI;
import java.util.Map;
import org.cookieandkakao.babting.common.exception.customexception.ApiException;
import org.cookieandkakao.babting.common.properties.KakaoProviderProperties;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventDetailGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventListGetResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TalkCalendarClientService {

    private final RestClient restClient = RestClient.builder().build();
    private final KakaoProviderProperties kakaoProviderProperties;

    public TalkCalendarClientService(KakaoProviderProperties kakaoProviderProperties) {
        this.kakaoProviderProperties = kakaoProviderProperties;
    }

    public EventListGetResponse getEventList(String accessToken, String from, String to) {
        String url = kakaoProviderProperties.calendarEventListUri();
        URI uri = buildUri(url, from, to);
        try {
            ResponseEntity<EventListGetResponse> response = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toEntity(EventListGetResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new ApiException("일정 목록 조회 중 에러 발생 : " + e.getMessage());
        }
    }

    public EventDetailGetResponse getEvent(String accessToken, String eventId) {
        String url = kakaoProviderProperties.calendarEventDetailUri();
        URI uri = buildGetEventUri(url, eventId);

        try {
            ResponseEntity<EventDetailGetResponse> response = restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toEntity(EventDetailGetResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new ApiException("일정 상세 조회 중 오류 발생 : " + e.getMessage());
        }
    }

    public Map<String, Object> createEvent(String accessToken,
        MultiValueMap<String, String> formData) {
        String url = kakaoProviderProperties.calendarCreateEventUri();
        URI uri = URI.create(url);
        try {
            ResponseEntity<Map> response = restClient.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body(formData)
                .retrieve()
                .toEntity(Map.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new ApiException("일정 생성 중 오류 발생 : " + e.getMessage());
        }
    }

    private URI buildUri(String baseUrl, String from, String to) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("from",from)
            .queryParam("to", to)
            .queryParam("limit", 100)
            .queryParam("time_zone", "Asia/Seoul")
            .build().toUri();
    }

    private URI buildGetEventUri(String baseUrl, String eventId) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("event_id", eventId)
            .build().toUri();
    }

}
