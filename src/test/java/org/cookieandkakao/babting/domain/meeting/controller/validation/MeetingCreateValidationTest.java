package org.cookieandkakao.babting.domain.meeting.controller.validation;

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

class MeetingCreateValidationTest {

    private Validator validator;
    private LocationCreateRequest baseLocation;
    private LocalDate now;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationTime;
    private LocalTime startTime;
    private LocalTime endTime;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        baseLocation = new LocationCreateRequest("전대", "11", 1.1, 1.1);
        now = LocalDate.now();
        title = "밥팅";
        startDate = now.plusDays(1);
        endDate = now.plusDays(2);
        durationTime = 3;
        startTime = LocalTime.of(14, 0);
        endTime = LocalTime.of(17, 0);
    }

    @Test
    void 모임의_시작_시간은_현재_시간보다_빠를_수_없다() {
        //given
        startDate = now.minusDays(1);

        MeetingCreateRequest meetingCreateRequest = new MeetingCreateRequest(baseLocation, title,
            startDate, endDate, durationTime, startTime, endTime);
        //when
        Set<ConstraintViolation<MeetingCreateRequest>> validate = validator.validate(
            meetingCreateRequest);
        //then
        assertFalse(validate.isEmpty(), "유효성 검사 실패.");
        assertTrue(
            validate.stream().anyMatch(v -> v.getPropertyPath().toString().equals("startDate")));
    }

    @Test
    void 모임의_끝_시간은_현재_시간보다_빠를_수_없다() {
        //given
        endDate = now.minusDays(1);

        MeetingCreateRequest meetingCreateRequest = new MeetingCreateRequest(baseLocation, title,
            startDate, endDate, durationTime, startTime, endTime);
        //when
        Set<ConstraintViolation<MeetingCreateRequest>> validate = validator.validate(
            meetingCreateRequest);
        //then
        assertFalse(validate.isEmpty(), "유효성 검사 실패.");
        assertTrue(
            validate.stream().anyMatch(v -> v.getPropertyPath().toString().equals("endDate")));
    }

    @Test
    void 모임의_모든_정보가_입력되어야한다() {
        //given
        //when
        //then
    }
}