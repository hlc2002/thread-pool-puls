package com.spring.springthread;

import lombok.Data;

import java.util.*;

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
     * 根据字符串字段进行排序 不分组 字典序
     *
     * @param objectList   待排序的列表
     * @param stringMapper 字符串映射函数
     * @param ascending    是否升序
     * @param <T>          泛型
     */
    public static <T> void sortByMapperStringFieldNoGroupOrderByDict(List<T> objectList,
                                                                     Mapper<T, String> stringMapper,
                                                                     boolean ascending) {
        sortByMapperStringField(objectList, stringMapper, ascending, false, false, "", null);
    }

    /**
     * 根据字符串字段进行排序 不分组
     *
     * @param objectList   待排序的列表
     * @param stringMapper 字符串映射函数
     * @param ascending    是否升序
     * @param dictionary   是否字典排序
     * @param <T>          泛型
     */
    public static <T> void sortByMapperStringFieldNoGrouping(List<T> objectList,
                                                             Mapper<T, String> stringMapper,
                                                             boolean ascending,
                                                             boolean dictionary) {
        sortByMapperStringField(objectList, stringMapper, ascending, dictionary, false, "", null);
    }

    /**
     * 根据字符串字段进行排序
     *
     * @param objectList            待排序的列表
     * @param stringMapper          字符串映射函数
     * @param ascending             是否升序
     * @param dictionary            是否字典排序
     * @param fieldExistGroupFlag   字段是否存在分组
     * @param groupFlag             分组标识
     * @param filterGroupIndexArray 过滤的分组索引
     * @param <T>                   泛型
     */
    public static <T> void sortByMapperStringField(List<T> objectList,
                                                   Mapper<T, String> stringMapper,
                                                   boolean ascending,
                                                   boolean dictionary,
                                                   boolean fieldExistGroupFlag,
                                                   String groupFlag,
                                                   List<Integer> filterGroupIndexArray) {
        if (null == objectList || objectList.isEmpty())
            throw new IllegalArgumentException("arg list is empty");
        objectList.sort((group1, group2) -> {
            String mappedValue1 = stringMapper.getMappedValue(group1), mappedValue2 = stringMapper.getMappedValue(group2);
            if (mappedValue1 == null || mappedValue1.isEmpty()) {
                return mappedValue2 == null || mappedValue2.isEmpty() ? 0 : -1;
            }
            if (mappedValue2 == null || mappedValue2.isEmpty()) {
                return 1;
            }
            if (!fieldExistGroupFlag) {
                return dictionary
                        ? ascending
                        ? compareLeftBiggerByDict(mappedValue1, mappedValue2)
                        : compareLeftBiggerByDict(mappedValue2, mappedValue1)
                        : ascending
                        ? compareLeftBiggerByNumValue(mappedValue1, mappedValue2)
                        : compareLeftBiggerByNumValue(mappedValue2, mappedValue1);
            }

            String[] mappedArr1 = mappedValue1.split(groupFlag), mappedArr2 = mappedValue2.split(groupFlag);

            List<String> mappedArr1List = new LinkedList<>(), mappedArr2List = new LinkedList<>();
            for (int i = 0, j = 0; i < mappedArr1.length || j < mappedArr2.length; i++, j++) {
                if (i < mappedArr1.length && !filterGroupIndexArray.contains(i)) {
                    mappedArr1List.add(mappedArr1[i]);
                }
                if (j < mappedArr2.length && !filterGroupIndexArray.contains(j)) {
                    mappedArr2List.add(mappedArr2[j]);
                }
            }
            String mappedValue1Str = String.join("", mappedArr1List), mappedValue2Str = String.join("", mappedArr2List);
            return dictionary
                    ? ascending
                    ? compareLeftBiggerByDict(mappedValue1Str, mappedValue2Str)
                    : compareLeftBiggerByDict(mappedValue2Str, mappedValue1Str)
                    : ascending
                    ? compareLeftBiggerByNumValue(mappedValue1Str, mappedValue2Str)
                    : compareLeftBiggerByNumValue(mappedValue2Str, mappedValue1Str);
        });
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
    public static int compareLeftBiggerByNumValue(String str1, String str2) {
        if (str1 == null || str2 == null)
            throw new IllegalArgumentException("arg list exist null str");
        return Integer.valueOf(str1).compareTo(Integer.valueOf(str2));
    }

    public static void main(String[] args) {
        // 存在分组情况
        List<ObjStr> list = new ArrayList<>();
        list.add(new ObjStr("20241120-10032-1", 1));
        list.add(new ObjStr("20241120", 2));
        list.add(new ObjStr("20241120-10031-3", 3));
        list.add(new ObjStr("20241120-10031-20", 4));
        sortByMapperStringField(list, ObjStr::getStr, true, false, true, "-", Collections.singletonList(1));
        System.out.println(list);
        // 不存在分组情况
        List<ObjStr> list2 = new ArrayList<>();
        list2.add(new ObjStr("202411212", 1));
        list2.add(new ObjStr("20241120", 2));
        list2.add(new ObjStr("20241123", 3));
        sortByMapperStringFieldNoGrouping(list2, ObjStr::getStr, true, false);
        System.out.println(list2);
    }

    @Data
    public static class ObjStr {
        private String str;
        private int num;

        public ObjStr(String str, int num) {
            this.str = str;
            this.num = num;
        }
    }
}
