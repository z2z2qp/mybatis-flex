/*
 *  Copyright (c) 2022-2025, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mybatisflex.core.util;


import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DateUtil {

    private DateUtil() {
    }

    public static String datePatternWithoutDividing = "yyyyMMdd";
    public static String datePattern = "yyyy-MM-dd";
    public static final String dateMinutePattern = "yyyy-MM-dd HH:mm";
    public static final String dateMinutePattern2 = "yyyy-MM-dd'T'HH:mm";
    public static String datetimePattern = "yyyy-MM-dd HH:mm:ss";
    public static final String dateMillisecondPattern = "yyyy-MM-dd HH:mm:ss SSS";
    public static final String dateCSTPattern = "EEE MMM dd HH:mm:ss zzz yyyy";

    private static final ThreadLocal<HashMap<String, SimpleDateFormat>> TL = ThreadLocal.withInitial(HashMap::new);

    private static final Map<String, DateTimeFormatter> dateTimeFormatters = new ConcurrentHashMap<>();

    public static DateTimeFormatter getDateTimeFormatter(String pattern) {
        var ret = dateTimeFormatters.get(pattern);
        if (ret == null) {
            ret = DateTimeFormatter.ofPattern(pattern);
            dateTimeFormatters.put(pattern, ret);
        }
        return ret;
    }

    public static SimpleDateFormat getSimpleDateFormat(String pattern) {
        var ret = TL.get().get(pattern);
        if (ret == null) {
            if (dateCSTPattern.equals(pattern)) {
                ret = new SimpleDateFormat(dateCSTPattern, Locale.US);
            } else {
                ret = new SimpleDateFormat(pattern);
            }
            TL.get().put(pattern, ret);
        }
        return ret;
    }


    public static Date parseDate(Object value) {
        return switch (value) {
            case Number num -> new Date(num.longValue());
            case Timestamp timestamp -> new Date(timestamp.getTime());
            case LocalDate localDate -> DateUtil.toDate(localDate);
            case LocalDateTime localDateTime -> DateUtil.toDate(localDateTime);
            case LocalTime localTime -> DateUtil.toDate(localTime);
            default -> {
                String s = value.toString();
                if (StringUtil.isNumeric(s)) {
                    yield new Date(Long.parseLong(s));
                }
                yield DateUtil.parseDate(s);
            }
        };
    }


    public static Date parseDate(String dateString) {
        if (StringUtil.noText(dateString)) {
            return null;
        }
        dateString = dateString.trim();
        try {
            var sdf = getSimpleDateFormat(getPattern(dateString));
            try {
                return sdf.parse(dateString);
            } catch (ParseException ex) {
                //2022-10-23 00:00:00.0
                var lastIndexOf = dateString.lastIndexOf(".");
                if (lastIndexOf == 19) {
                    return parseDate(dateString.substring(0, lastIndexOf));
                }

                //2022-10-23 00:00:00,0
                lastIndexOf = dateString.lastIndexOf(",");
                if (lastIndexOf == 19) {
                    return parseDate(dateString.substring(0, lastIndexOf));
                }

                //2022-10-23 00:00:00 000123
                lastIndexOf = dateString.lastIndexOf(" ");
                if (lastIndexOf == 19) {
                    return parseDate(dateString.substring(0, lastIndexOf));
                }

                if (dateString.contains(".") || dateString.contains("/")) {
                    dateString = dateString.replace(".", "-").replace("/", "-");
                    return sdf.parse(dateString);
                } else {
                    throw ex;
                }
            }
        } catch (ParseException ex) {
            throw new IllegalArgumentException("The date format is not supported for the date string: " + dateString);
        }
    }


    public static LocalDateTime parseLocalDateTime(String dateString) {
        if (StringUtil.noText(dateString)) {
            return null;
        }
        dateString = dateString.trim();
        var dateTimeFormatter = getDateTimeFormatter(getPattern(dateString));
        try {
            return LocalDateTime.parse(dateString, dateTimeFormatter);
        } catch (Exception ex) {
            //2022-10-23 00:00:00.0
            var lastIndexOf = dateString.lastIndexOf(".");
            if (lastIndexOf == 19) {
                return parseLocalDateTime(dateString.substring(0, lastIndexOf));
            }

            //2022-10-23 00:00:00,0
            lastIndexOf = dateString.lastIndexOf(",");
            if (lastIndexOf == 19) {
                return parseLocalDateTime(dateString.substring(0, lastIndexOf));
            }

            //2022-10-23 00:00:00 000123
            lastIndexOf = dateString.lastIndexOf(" ");
            if (lastIndexOf == 19) {
                return parseLocalDateTime(dateString.substring(0, lastIndexOf));
            }

            if (dateString.contains(".") || dateString.contains("/")) {
                dateString = dateString.replace(".", "-").replace("/", "-");
                dateTimeFormatter = getDateTimeFormatter(getPattern(dateString));
                return LocalDateTime.parse(dateString, dateTimeFormatter);
            } else {
                throw ex;
            }
        }
    }


    private static String getPattern(String dateString) {
        var length = dateString.length();
        if (length == datetimePattern.length()) {
            return datetimePattern;
        } else if (length == datePattern.length()) {
            return datePattern;
        } else if (length == dateMinutePattern.length()) {
            if (dateString.contains("T")) {
                return dateMinutePattern2;
            }
            return dateMinutePattern;
        } else if (length == dateMillisecondPattern.length()) {
            return dateMillisecondPattern;
        } else if (length == datePatternWithoutDividing.length()) {
            return datePatternWithoutDividing;
        } else if (length == dateCSTPattern.length()) {
            return dateCSTPattern;
        } else {
            throw new IllegalArgumentException("The date format is not supported for the date string: " + dateString);
        }
    }


    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        // java.sql.Date 不支持 toInstant()，需要先转换成 java.util.Date
        if (date instanceof java.sql.Date) {
            date = new Date(date.getTime());
        }

        var instant = date.toInstant();
        var zone = ZoneId.systemDefault();
        return LocalDateTime.ofInstant(instant, zone);
    }


    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        // java.sql.Date 不支持 toInstant()，需要先转换成 java.util.Date
        if (date instanceof java.sql.Date) {
            date = new Date(date.getTime());
        }

        var instant = date.toInstant();
        var zone = ZoneId.systemDefault();
        var localDateTime = LocalDateTime.ofInstant(instant, zone);
        return localDateTime.toLocalDate();
    }


    public static LocalTime toLocalTime(Date date) {
        if (date == null) {
            return null;
        }
        // java.sql.Date 不支持 toInstant()，需要先转换成 java.util.Date
        if (date instanceof java.sql.Date) {
            date = new Date(date.getTime());
        }

        var instant = date.toInstant();
        var zone = ZoneId.systemDefault();
        var localDateTime = LocalDateTime.ofInstant(instant, zone);
        return localDateTime.toLocalTime();
    }


    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        var zone = ZoneId.systemDefault();
        var instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }


    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        var zone = ZoneId.systemDefault();
        var instant = localDate.atStartOfDay().atZone(zone).toInstant();
        return Date.from(instant);
    }


    public static Date toDate(LocalTime localTime) {
        if (localTime == null) {
            return null;
        }
        var localDate = LocalDate.now();
        var localDateTime = LocalDateTime.of(localDate, localTime);
        var zone = ZoneId.systemDefault();
        var instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }


    public static String toDateTimeString(Date date) {
        return toString(date, datetimePattern);
    }


    public static String toString(Date date, String pattern) {
        return date == null ? null : getSimpleDateFormat(pattern).format(date);
    }


}
