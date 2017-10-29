package com.codebutler.farebot.xml;

import org.simpleframework.xml.transform.Transform;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Transforms a java.util.Calendar into seconds since UNIX epoch (like EpochDateTransform)
 */

public class EpochCalendarTransform implements Transform<Calendar> {

    @Override
    public Calendar read(String value) throws Exception {
        long s = Long.valueOf(value);
        Calendar c = GregorianCalendar.getInstance();
        c.setTimeInMillis(s);
        return c;
    }

    @Override
    public String write(Calendar value) throws Exception {
        return String.valueOf(value.getTimeInMillis());
    }
}
