package org.cookieandkakao.babting.domain.meeting.exception;

import java.util.NoSuchElementException;

public class MeetingNotFoundException extends NoSuchElementException {
    public MeetingNotFoundException(String message) {
        super(message);
    }
}
