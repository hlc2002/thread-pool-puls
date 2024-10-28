package com.spring.springthread.datastruct.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author spring
 * @since 2024/10/28 13:22:39
 * @apiNote 二叉搜索树
 * @version 1.0
 */
@SuppressWarnings("all")
public class BinarySearchTree {
    Node root;
    BinarySearchTree(){}
    BinarySearchTree(Node root){
        this.root = root;
    }

    private final int PRE_ORDER = 1;
    private final int IN_ORDER = 2;
    private final int POST_ORDER = 3;

    public static class Node{
        private int data;
        private Node left;
        private Node right;
        public Node(int data){
            this.data = data;
        }
    }

    /**
     * 通过中序遍历构建二叉搜索树
     * @param arr 随意的数组
     * @return 二叉搜索树
     */
    public BinarySearchTree buildByArr(int[] arr){
        // 首先对数组排序
        Arrays.sort(arr);
        // 利用二叉搜索树中序遍历是顺序数组，且左小右大的原则构建树
        Node node = buildByArr(arr, 0, arr.length - 1);
        return new BinarySearchTree(node);
    }

    private Node buildByArr(int[] arr,int start,int end){
        if(start > end || end > arr.length - 1){
            return null;
        }
        // 根节点坐标
        int mid = (start + end) / 2;
        // 创建根节点
        Node root = new Node(arr[mid]);
        // 递归创建左右子树
        root.left = buildByArr(arr, start, mid - 1);
        root.right = buildByArr(arr, mid + 1, end);
        return root;
    }

    /**
     * 二叉搜索树增加新节点
     * @param value 新节点值
     */
    public void add(int value){
        // 递归查找父节点
        Node perant = compare(root, value);
        // 存在相等的值，不允许插入
        if (perant == null){
            return;
        }
        Node node = new Node(value);
        if (value > perant.data){
            perant.right = node;
        }else if(value < perant.data){
            perant.left = node;
        }
    }

    private Node compare(Node node, int value){
        if(node.data == value){
            return  null;
        }
        if(node.left == null && node.data > value)
            return node;
        if(node.right == null && node.data < value)
            return node;
        return value > node.data ? compare(node.right,value) : compare(node.left,value);
    }

    /**
     * 二叉搜索树删除节点
     * @param value 删除的节点值
     */
    public void delete(int value){
        root = delete(root,value);
    }
    private Node delete(Node node,int value){
        if(node == null){
            return null;
        }
        if(value == node.data){
            if(node.left == null && node.right == null){
                return null;
            }
            if(node.left == null){
                return node.right;
            }else if(node.right == null){
                return node.left;
            }else{
                // 找到右子树最小值
                int newVal = findSmallest(node.right);
                // 将右子树最小值选举为根节点值
                node.data = newVal;
                // 删除右子树最小值的那个叶子节点
                node.right = delete(node.right,newVal);
                return node;
            }
        }
        if(value < node.data){
            node.left = delete(node.left, value);
        }else{
            node.right = delete(node.right, value);
        }
        return node;
    }

    private int findSmallest(Node node){
        return node.left == null ? node.data : findSmallest(node.left);
    }

    private Node find(Node node,int value) {
        if (node == null) {
            return null;
        }
        if (node.data == value) {
            return node;
        }
        return value > node.data ? find(node.right, value) : find(node.left, value);
    }

    /**
     * 查找节点为value的子树
     * @param value 节点值
     * @return 节点
     */
    public Node find(int value){
        return find(root,value);
    }

    public List<Integer> order(int type) {
        List<Integer> list = new ArrayList<>();
        order(root,list,type);
        return list;
    }
    private void order(Node node, List<Integer> list, int type){
        if(node == null) {
            return;
        }
        switch(type){
            case PRE_ORDER:
                list.add(node.data);
                order(node.left,list,type);
                order(node.right,list,type);
                break;
            case IN_ORDER:
                order(node.left,list,type);
                list.add(node.data);
                order(node.right,list,type);
                break;
            case POST_ORDER:
                order(node.left,list,type);
                order(node.right,list,type);
                list.add(node.data);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static void main(String[] args) {
        BinarySearchTree tree = new BinarySearchTree();
        BinarySearchTree binarySearchTree = tree.buildByArr(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        System.out.println(binarySearchTree.order(2));
        binarySearchTree.add(12);
        System.out.println(binarySearchTree.order(2));
        binarySearchTree.delete(5);
        System.out.println(binarySearchTree.order(2));
    }
}
