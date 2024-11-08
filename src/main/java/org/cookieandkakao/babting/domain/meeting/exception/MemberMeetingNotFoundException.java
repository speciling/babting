package org.cookieandkakao.babting.domain.meeting.exception;

import java.util.NoSuchElementException;

public class MemberMeetingNotFoundException extends NoSuchElementException {
    public MemberMeetingNotFoundException(String message) {
        super(message);
    }
}
