package com.spring.springthread.datastruct.tree;

/**
 * @author spring
 * @since 2024/10/28 13:23:32
 * @apiNote 平衡二叉搜索树
 * @version 1.0
 */
@SuppressWarnings("all")
public class BalanceBinarySearchTree {

    public <T> T search(int value) {
        return null;
    }
    public void add(int value){

    }
    public <T> T delete(){
        return null;
    }
    /**
     * AVL平衡二叉搜索树
     */
    public class AvlTree extends BalanceBinarySearchTree{
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

        @Override
        public Node search(int value) {
            return super.search(value);
        }

        @Override
        public void add(int value) {
            super.add(value);
        }

        @Override
        public Node delete() {
            return super.delete();
        }
    }

    /**
     * 红黑树
     */
    public class RBTree extends BalanceBinarySearchTree{
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

        @Override
        public Node search(int value) {
            return super.search(value);
        }

        @Override
        public void add(int value) {
            super.add(value);
        }

        @Override
        public Node delete() {
            return super.delete();
        }
    }
}
