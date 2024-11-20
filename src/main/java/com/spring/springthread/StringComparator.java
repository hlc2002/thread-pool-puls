package com.spring.springthread;

import java.util.List;

/**
 * @author: jd-jsj-spring
 * @since: 2024/11/20 16:54:31
 * @apiNote: 字符串比较工具类
 * @version: 1.0
 */
public class StringComparator {

    @FunctionalInterface
    public interface Mapper<T, R> {
        R getMappedValue(T t);
    }

    /**
     * 根据字符串映射字段进行排序，默认升序，字典序
     *
     * @param objectList   待排序对象列表
     * @param stringMapper 字符串映射函数
     * @return 排序后的对象列表
     */
    public static <T> List<T> sortByMapperStringField(List<T> objectList, Mapper<T, String> stringMapper) {
        return sortByMapperStringField(objectList, stringMapper, true);
    }

    /**
     * 根据字符串映射字段进行排序，默认升序，字典序
     *
     * @param objectList   待排序对象列表
     * @param stringMapper 字符串映射函数
     * @param ascending    是否升序
     * @return 排序后的对象列表
     */
    public static <T> List<T> sortByMapperStringField(List<T> objectList, Mapper<T, String> stringMapper, boolean ascending) {
        return sortByMapperStringField(objectList, stringMapper, ascending, true);
    }

    /**
     * 根据字符串映射字段进行排序
     *
     * @param objectList   待排序对象列表
     * @param stringMapper 字符串映射函数
     * @param ascending    是否升序
     * @param dictionary   是否按照字典顺序排序
     * @return 排序后的对象列表
     */
    public static <T> List<T> sortByMapperStringField(List<T> objectList, Mapper<T, String> stringMapper, boolean ascending, boolean dictionary) {
        if (null == objectList || objectList.isEmpty())
            return objectList;
        objectList.sort((o1, o2) -> ascending
                ? dictionary ? compareLeftBiggerByDict(stringMapper.getMappedValue(o1), stringMapper.getMappedValue(o2))
                        : compareLeftBiggerByNumberBits(stringMapper.getMappedValue(o1), stringMapper.getMappedValue(o2))
                : dictionary ? compareLeftBiggerByDict(stringMapper.getMappedValue(o2), stringMapper.getMappedValue(o1))
                        : compareLeftBiggerByNumberBits(stringMapper.getMappedValue(o2), stringMapper.getMappedValue(o1)));
        return objectList;
    }

    /**
     * 字符串之间的字典比较，按照字典顺序比较
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 比较结果，正数 表示 str1 大于 str2，负数 表示 str1 小于 str2，0 表示相等
     */
    public static int compareLeftBiggerByDict(String str1, String str2) {
        if (str1 == null || str2 == null)
            throw new IllegalArgumentException("arg list exist null str");
        return str1.compareTo(str2);
    }

    /**
     * 字符串之间的非字典比较，按照每一位的数字比较
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return 比较结果，正数 表示 str1 大于 str2，负数 表示 str1 小于 str2，0 表示相等
     */
    public static int compareLeftBiggerByNumberBits(String str1, String str2) {
        if (str1 == null || str2 == null)
            throw new IllegalArgumentException("arg list exist null str");
        int sum = 0;
        for (int i = 0; i < str1.length() && i < str2.length(); i++) {
            sum += str1.charAt(i) - str2.charAt(i);
        }
        if (sum != 0) {
            return sum > 0 ? 1 : -1;
        }
        return str1.length() > str2.length() ? 1 : str1.length() - str2.length();
    }

    public static void main(String[] args) {
        assert compareLeftBiggerByNumberBits("20241120-10032-1", "20241120-10032-2") == -1;
        assert compareLeftBiggerByNumberBits("20241120-10032-2", "20241120-10032-1") == 1;
        assert compareLeftBiggerByNumberBits("20241120-10032-2", "20241120-10032-2") == 0;
        assert compareLeftBiggerByNumberBits("20241120-10032", "20241120-10032-3") == -1;
        assert compareLeftBiggerByNumberBits("20241120-10032-3", "20241120-10032") == 1;
        assert compareLeftBiggerByNumberBits("20241120-10032-3", "20241120-10032-3") == 0;
        assert compareLeftBiggerByNumberBits("20241121", "20241120-11-1") == 1;
        assert compareLeftBiggerByNumberBits("20241120", "20241119") == 1;

        List<String> strList = List.of("20241120-10032-1", "20241120-10032-2", "20241120-10032-3", "20241120-10032-4", "20241120-10032-5", "20241120-10032-6", "20241120-10032-7", "20241120-10032-8", "20241120-10032-9", "20241120-10032-10");
        sortByMapperStringField(strList, String::toString, true, false);
        System.out.println(strList);
    }
}
