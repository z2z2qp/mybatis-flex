/*
 *  Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Date;

public class ConvertUtil {

    private ConvertUtil() {}

    public static <T> T convert(Object value, Class<T> targetClass) {
        return (T) convert(value, targetClass, false);
    }

    public static Object convert(Object value, Class targetClass, boolean ignoreConvertError) {
        if (value == null && targetClass.isPrimitive()) {
            return getPrimitiveDefaultValue(targetClass);
        }
        if (value == null || (targetClass != String.class && value.getClass() == String.class
                && StringUtil.isBlank((String) value))) {
            return null;
        }
        if (value.getClass().isAssignableFrom(targetClass)) {
            return value;
        }
        if (targetClass == String.class) {
            return value.toString();
        } else if (targetClass == Integer.class || targetClass == int.class) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(value.toString());
        } else if (targetClass == Long.class || targetClass == long.class) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(value.toString());
        } else if (targetClass == Double.class || targetClass == double.class) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            return Double.parseDouble(value.toString());
        } else if (targetClass == Float.class || targetClass == float.class) {
            if (value instanceof Number number) {
                return number.floatValue();
            }
            return Float.parseFloat(value.toString());
        } else if (targetClass == Boolean.class || targetClass == boolean.class) {
            var v = value.toString().toLowerCase();
            if ("1".equals(v) || "true".equalsIgnoreCase(v)) {
                return Boolean.TRUE;
            } else if ("0".equals(v) || "false".equalsIgnoreCase(v)) {
                return Boolean.FALSE;
            } else {
                throw new RuntimeException("Can not parse to boolean type of value: \"" + value + "\"");
            }
        } else if (targetClass == java.math.BigDecimal.class) {
            return new java.math.BigDecimal(value.toString());
        } else if (targetClass == java.math.BigInteger.class) {
            return new java.math.BigInteger(value.toString());
        } else if (targetClass == byte[].class) {
            return value.toString().getBytes();
        } else if (targetClass == Date.class) {
            return DateUtil.parseDate(value);
        } else if (targetClass == LocalDateTime.class) {
            return toLocalDateTime(value);
        } else if (targetClass == LocalDate.class) {
            return DateUtil.toLocalDate(DateUtil.parseDate(value));
        } else if (targetClass == LocalTime.class) {
            return DateUtil.toLocalTime(DateUtil.parseDate(value));
        } else if (targetClass == Short.class || targetClass == short.class) {
            if (value instanceof Number number) {
                return number.shortValue();
            }
            return Short.parseShort(value.toString());
        } else if (targetClass.isEnum()) {
            var enumWrapper = EnumWrapper.of(targetClass);
            if (enumWrapper.hasEnumValueAnnotation()) {
                return enumWrapper.getEnum(value);
            } else if (value instanceof String) {
                return Enum.valueOf(targetClass, value.toString());
            }
        }

        if (ignoreConvertError) {
            return null;
        } else {
            throw new IllegalArgumentException("Can not convert \"" + value + "\" to type\"" + targetClass.getName() + "\".");
        }
    }


    //Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE
    public static Object getPrimitiveDefaultValue(Class<?> paraClass) {
        if (paraClass == int.class || paraClass == long.class || paraClass == float.class || paraClass == double.class) {
            return 0;
        } else if (paraClass == boolean.class) {
            return Boolean.FALSE;
        } else if (paraClass == short.class) {
            return (short) 0;
        } else if (paraClass == byte.class) {
            return (byte) 0;
        } else if (paraClass == char.class) {
            return '\u0000';
        } else {
            throw new IllegalArgumentException("Can not get primitive default value for type: " + paraClass);
        }
    }


    public static Integer toInt(Object i) {
        if (i instanceof Integer integer) {
            return integer;
        } else if (i instanceof Number number) {
            return number.intValue();
        }
        return i != null ? Integer.parseInt(i.toString()) : null;
    }

    public static Long toLong(Object l) {
        if (l instanceof Long ll) {
            return ll;
        } else if (l instanceof Number number) {
            return number.longValue();
        }
        return l != null ? Long.parseLong(l.toString()) : null;
    }

    public static Double toDouble(Object d) {
        if (d instanceof Double dd) {
            return dd;
        } else if (d instanceof Number number) {
            return number.doubleValue();
        }

        return d != null ? Double.parseDouble(d.toString()) : null;
    }

    public static BigDecimal toBigDecimal(Object b) {
        if (b instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        } else if (b != null) {
            return new BigDecimal(b.toString());
        } else {
            return null;
        }
    }

    public static BigInteger toBigInteger(Object b) {
        if (b instanceof BigInteger bigInteger) {
            return bigInteger;
        }
        // 数据类型 id(19 number)在 Oracle Jdbc 下对应的是 BigDecimal,
        // 但是在 MySql 下对应的是 BigInteger，这会导致在 MySql 下生成的代码无法在 Oracle 数据库中使用
        if (b instanceof BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        } else if (b instanceof Number number) {
            return BigInteger.valueOf(number.longValue());
        } else if (b instanceof String str) {
            return new BigInteger(str);
        }

        return (BigInteger) b;
    }

    public static Float toFloat(Object f) {
        if (f instanceof Float ff) {
            return ff;
        } else if (f instanceof Number number) {
            return number.floatValue();
        }
        return f != null ? Float.parseFloat(f.toString()) : null;
    }


    public static Short toShort(Object s) {
        if (s instanceof Short ss) {
            return ss;
        } else if (s instanceof Number number) {
            return number.shortValue();
        }
        return s != null ? Short.parseShort(s.toString()) : null;
    }


    public static Byte toByte(Object b) {
        if (b instanceof Byte bb) {
            return bb;
        } else if (b instanceof Number number) {
            return number.byteValue();
        }
        return b != null ? Byte.parseByte(b.toString()) : null;
    }

    public static Boolean toBoolean(Object b) {
        if (b instanceof Boolean bb) {
            return bb;
        } else if (b == null) {
            return null;
        }

        // 支持 Number 之下的整数类型
        if (b instanceof Number number) {
            int n = number.intValue();
            if (n == 1) {
                return Boolean.TRUE;
            } else if (n == 0) {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Can not support convert: \"" + b + "\" to boolean.");
        }

        // 支持 String
        if (b instanceof String s) {
            if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
                return Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
                return Boolean.FALSE;
            }
        }

        return (Boolean) b;
    }

    public static Number toNumber(Object o) {
        if (o instanceof Number number) {
            return number;
        } else if (o == null) {
            return null;
        }
        String s = o.toString();
        return s.indexOf('.') != -1 ? Double.parseDouble(s) : Long.parseLong(s);
    }


    public static Date toDate(Object o) {
        if (o instanceof Date date) {
            return date;
        }

        if (o instanceof Temporal) {
            if (o instanceof LocalDateTime localDateTime) {
                return DateUtil.toDate(localDateTime);
            }
            if (o instanceof LocalDate localDate) {
                return DateUtil.toDate(localDate);
            }
            if (o instanceof LocalTime localTime) {
                return DateUtil.toDate(localTime);
            }
        }

        if (o instanceof String s) {
            return DateUtil.parseDate(s);
        }

        return (Date) o;
    }


    public static LocalDateTime toLocalDateTime(Object o) {
        if (o instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (o instanceof Date date) {
            return DateUtil.toLocalDateTime(date);
        }
        if (o instanceof LocalDate localDate) {
            return localDate.atStartOfDay();
        }
        if (o instanceof LocalTime localTime) {
            return LocalDateTime.of(LocalDate.now(), localTime);
        }

        if (o instanceof String s) {
            return DateUtil.parseLocalDateTime(s);
        }

        return (LocalDateTime) o;
    }
}
