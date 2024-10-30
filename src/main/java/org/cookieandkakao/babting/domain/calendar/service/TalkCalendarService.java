package org.cookieandkakao.babting.domain.calendar.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.cookieandkakao.babting.common.cache.CacheKeyGenerator;
import org.cookieandkakao.babting.common.exception.customexception.EventCreationException;
import org.cookieandkakao.babting.common.exception.customexception.JsonConversionException;
import org.cookieandkakao.babting.domain.calendar.dto.request.EventCreateRequest;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventCreateResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventDetailGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventGetResponse;
import org.cookieandkakao.babting.domain.calendar.dto.response.EventListGetResponse;
import org.cookieandkakao.babting.domain.member.entity.KakaoToken;
import org.cookieandkakao.babting.domain.member.service.MemberService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class TalkCalendarService {

    private final TalkCalendarClientService talkCalendarClientService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventService eventService;
    private final MemberService memberService;
    private final RedisTemplate<String, String> redisTemplate;

    public TalkCalendarService(EventService eventService,
        TalkCalendarClientService talkCalendarClientService,
        MemberService memberService,
        RedisTemplate<String, String> redisTemplate) {
        this.eventService = eventService;
        this.talkCalendarClientService = talkCalendarClientService;
        this.memberService = memberService;
        this.redisTemplate = redisTemplate;
    }

    public List<EventGetResponse> getUpdatedEventList(String from, String to, Long memberId) {
        String cacheKey = CacheKeyGenerator.generateEventListKey(memberId, from, to);
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                // JSON 문자열을 List<EventGetResponse> 로 변환
                // 리스트이므로 TypeReference 사용
                return objectMapper.readValue(cachedJson, new TypeReference<List<EventGetResponse>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("캐시된 JSON 변환 오류", e);
            }
        }

        String kakaoAccessToken = getKakaoAccessToken(memberId);
        EventListGetResponse eventList = talkCalendarClientService.getEventList(kakaoAccessToken,
            from, to);
        List<EventGetResponse> updatedEvents = new ArrayList<>();

        for (EventGetResponse event : eventList.events()) {
            if (event.id() != null) {
                event = talkCalendarClientService.getEvent(kakaoAccessToken, event.id()).event();
                updatedEvents.add(event);
            } else {
                updatedEvents.add(event);
            }
        }
        cacheData(cacheKey, updatedEvents);

        return updatedEvents;
    }

    public EventDetailGetResponse getEvent(Long memberId, String eventId) {
        String cacheKey = CacheKeyGenerator.generateEventDetailKey(eventId);
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);

        if (cachedJson != null) {
            try {
                // JSON 문자열을 EventDetailGetResponse 로 변환
                // 단일 객체이므로 .class 사용
                return objectMapper.readValue(cachedJson, EventDetailGetResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("캐시된 JSON 변환 오류", e);
            }
        }

        String kakaoAccessToken = getKakaoAccessToken(memberId);
        EventDetailGetResponse eventDetailGetResponse = talkCalendarClientService.getEvent(kakaoAccessToken, eventId);

        cacheData(cacheKey, eventDetailGetResponse);
        return eventDetailGetResponse;
    }

    // JSON 변환 후 Redis에 저장
    private void cacheData(String cacheKey, Object data) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(data), Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 오류", e);
        }
    }

    public EventCreateResponse createEvent(
        EventCreateRequest eventCreateRequest, Long memberId) {
        String kakaoAccessToken = getKakaoAccessToken(memberId);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // event라는 key에 JSON 형태의 데이터를 추가해야 함
        // EventCreateRequestDto를 JSON으로 변환
        String eventJson = convertToJSONString(eventCreateRequest);
        // event라는 key로 JSON 데이터를 추가
        formData.add("event", eventJson);
        EventCreateResponse responseBody = Optional.ofNullable(
            talkCalendarClientService.createEvent(kakaoAccessToken, formData)).orElseThrow(
            () -> new EventCreationException("Event 생성 중 오류 발생: 응답에서 event_id가 없습니다."));
        evictMemberCache(memberId);
        return responseBody;
    }

    // EventCreateRequestDto를 JSON 문자열로 변환하는 메서드
    private String convertToJSONString(EventCreateRequest eventCreateRequest) {
        try {
            return objectMapper.writeValueAsString(eventCreateRequest);
        } catch (JsonProcessingException e) {
            throw new JsonConversionException("JSON 변환 중 오류가 발생했습니다.");
        }
    }

    private String getKakaoAccessToken(Long memberId) {
        KakaoToken kakaoToken = memberService.getKakaoToken(memberId);
        String kakaoAccessToken = kakaoToken.getAccessToken();
        return kakaoAccessToken;
    }

    // 키 값에서 memberId가 포함되어 있는 것 삭제
    private void evictMemberCache(Long memberId) {
        String pattern = CacheKeyGenerator.generateEventListPattern(memberId);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
