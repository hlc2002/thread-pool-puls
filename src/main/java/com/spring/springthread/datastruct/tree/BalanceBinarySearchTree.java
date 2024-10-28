package com.spring.springthread.datastruct.tree;

/**
 * @author spring
 * @since 2024/10/28 13:23:32
 * @apiNote 平衡二叉搜索树
 * @version 1.0
 */
public class BalanceBinarySearchTree {
    /**
     * AVL平衡二叉搜索树
     */
    public static class AvlTree {
        private Node root;

        public static class Node {
            int data;
            Node left;
            Node right;
            int height;

            Node(int data) {
                this.data = data;
                height = 1;
            }
        }
    }

    /**
     * 红黑树
     */
    public static class RBTree {
        private final static boolean RED = true;
        private final static boolean BLACK = false;
        private Node root;

        public static class Node {
            int data;
            Node left;
            Node right;
            Node parent;
            boolean color;

            Node(int data, boolean color) {
                this.data = data;
                this.color = color;
            }
        }
    }
}
