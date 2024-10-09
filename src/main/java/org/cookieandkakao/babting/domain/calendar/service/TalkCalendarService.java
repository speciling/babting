package org.cookieandkakao.babting.domain.calendar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.cookieandkakao.babting.domain.calendar.dto.request.EventCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventDetailGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventListGetResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class TalkCalendarService {

    private final TalkCalendarClientService talkCalendarClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;

    public TalkCalendarService(EventService eventService, TalkCalendarClientService talkCalendarClientService) {
        this.eventService = eventService;
        this.talkCalendarClientService = talkCalendarClientService;
    }

    // 일정 목록을 조회할 때 캐시 적용
    @Cacheable(value = "eventListCache", key = "#memberId")
    public List<EventGetResponse> getUpdatedEventList(String accessToken, String from, String to, Long memberId) {
        EventListGetResponse eventList = talkCalendarClientService.getEventList(accessToken, from, to);
        List<EventGetResponse> updatedEvents = new ArrayList<>();

        for (EventGetResponse event : eventList.events()) {
            if (event.id() != null) {
                event = talkCalendarClientService.getEvent(accessToken, event.id()).event();
                updatedEvents.add(event);
            } else {
                updatedEvents.add(event);
            }
        }

        return updatedEvents;
    }

    @Cacheable(value = "eventDetailCache", key = "#eventId")
    public EventDetailGetResponse getEvent(String accessToken, String eventId) {
        return talkCalendarClientService.getEvent(accessToken, eventId);
    }

    @CacheEvict(value = "eventListCache", key = "#memberId")
    public EventCreateResponse createEvent(String accessToken,
        EventCreateRequest eventCreateRequest, Long memberId) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // event라는 key에 JSON 형태의 데이터를 추가해야 함
        // EventCreateRequestDto를 JSON으로 변환
        String eventJson = convertToJSONString(eventCreateRequest);
        // event라는 key로 JSON 데이터를 추가
        formData.add("event", eventJson);
        // 응답에서 event_id 추출
        Map<String, Object> responseBody =talkCalendarClientService.createEvent(accessToken, formData);
        if (responseBody != null && responseBody.containsKey("event_id")) {
            String eventId = responseBody.get("event_id").toString();
            // EventCreateResponseDto로 응답 반환
            return new EventCreateResponse(eventId);
        }
        throw new RuntimeException("Event 생성 중 오류 발생: 응답에서 event_id가 없습니다.");
    }

    // EventCreateRequestDto를 JSON 문자열로 변환하는 메서드
    private String convertToJSONString(EventCreateRequest eventCreateRequest) {
        try {
            return objectMapper.writeValueAsString(eventCreateRequest);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
