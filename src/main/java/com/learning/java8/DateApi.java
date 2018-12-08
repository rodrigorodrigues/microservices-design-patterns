package com.learning.java8;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

@Slf4j
public class DateApi {
    public static void main(String[] args) {
        //Old way
        Calendar calendar = new GregorianCalendar(2018, 0, 1, 10, 11, 50); //Ugly month starts with 0 = January

        String displayMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String time = new SimpleDateFormat("HH:mm:ss").format(calendar.getTime());
        log.debug("old way - Year: {},\tMonth: {},\tDay: {},\tTime: {}", calendar.get(Calendar.YEAR), displayMonthName, calendar.get(Calendar.DAY_OF_MONTH), time);
        //Old way

        //New way
        LocalDateTime dateTime = LocalDateTime.of(2018, Month.JANUARY, 1, 10, 11, 50);
        time = DateTimeFormatter.ISO_TIME.format(dateTime);
        log.debug("new way - Year: {},\tMonth: {},\tDay: {},\tTime: {}", dateTime.getYear(), dateTime.getMonth(), dateTime.getDayOfMonth(), time);
        //New way
    }
}
