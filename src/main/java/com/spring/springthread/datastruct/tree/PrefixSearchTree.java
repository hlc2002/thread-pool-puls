package com.spring.springthread.datastruct.tree;

/**
 * @author spring
 * @since 2024/10/28 13:31:45
 * @apiNote 前缀搜索树
 * @version 1.0
 */
public class PrefixSearchTree {
    Node root;

    PrefixSearchTree(){
        root = new Node();
    }

    public static class Node{
        Node[] arr;
        boolean isEnd;

        Node(){
            arr = new Node[26];
        }
    }

    /**
     * 新增字符串
     * @param str 新字符串
     */
    public void insetNewStr(String str){
        char[] chars = str.toCharArray();
        Node curr = root;
        for(char ch : chars){
            if(curr.arr[ch - 'a'] == null){
                curr.arr[ch - 'a'] = new Node();
            }
            curr = curr.arr[ch - 'a'];
        }
        curr.isEnd = true;
    }

    /**
     * 搜索字符串是否存在或者前缀是否存在
     * @param str 待搜索的字符串
     * @param searchPrefix 是否搜索前缀
     * @return 搜索结果
     */
    public boolean search(String str, boolean searchPrefix){
        char[] chars = str.toCharArray();
        Node curr = root;
        for (char ch : chars) {
            if (curr.arr[ch - 'a'] == null) {
                return false;
            }
            curr = curr.arr[ch - 'a'];
        }
        return searchPrefix || curr.isEnd;
    }

    public static void main(String[] args) {
        PrefixSearchTree tree = new PrefixSearchTree();
        tree.insetNewStr("abc");
        tree.insetNewStr("abd");
        tree.insetNewStr("abf");
        System.out.println(tree.search("ab",true));
    }
}
