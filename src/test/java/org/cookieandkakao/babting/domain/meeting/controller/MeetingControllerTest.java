package org.cookieandkakao.babting.domain.meeting.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.cookieandkakao.babting.domain.meeting.dto.request.LocationCreateRequest;
import org.cookieandkakao.babting.domain.meeting.dto.request.MeetingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetingControllerTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void 모임의_시작_시간은_현재_시간보다_빠를_수_없다() {
        //given
        LocalDate now = LocalDate.now();
        // 모임의 시작 시간보다 빠른 시간
        LocalDate startDate = now.minusDays(1);
        LocalDate endDate = now.plusDays(1);

        LocationCreateRequest locationCreateRequest = new LocationCreateRequest("전대", "11", 1.1,
            1.1);
        MeetingCreateRequest meetingCreateRequest = new MeetingCreateRequest(locationCreateRequest,
            "밥팅",
            startDate, endDate, 3, LocalTime.of(14, 0), LocalTime.of(17, 0));
        //when
        Set<ConstraintViolation<MeetingCreateRequest>> validate = validator.validate(
            meetingCreateRequest);
        //then
        assertFalse(validate.isEmpty(), "유효성 검사 실패.");
        assertTrue(validate.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startDate")));
    }

    @Test
    void 모임의_모든_정보가_입력되어야한다(){
        //given
        //when
        //then
    }
}