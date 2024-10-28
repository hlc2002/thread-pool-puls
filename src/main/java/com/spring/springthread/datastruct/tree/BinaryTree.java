package com.spring.springthread.datastruct.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * @author spring
 * @since 2024/10/28 13:21:20
 * @apiNote 二叉树
 * @version 1.0
 */
@SuppressWarnings("all")
public class BinaryTree {
    Node root;
    BinaryTree(){}
    BinaryTree(Node root){
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
     * 根据前序与中序数组构建二叉树
     * @param preOrder 前序遍历
     * @param inOrder 中序遍历
     * @return 树
     */
    public BinaryTree bulidByPreOrderAndInOrderArr(int[] preOrder,int[] inOrder){
        Node node = bulidByPreOrderAndInOrder(preOrder, inOrder, 0, preOrder.length - 1, 0, inOrder.length - 1);
        return new BinaryTree(node);
    }

    public Node bulidByPreOrderAndInOrder(int[] preOrder, int[] inOrder,
                                          int preStart,int preEnd,
                                          int inStart,int inEnd) {
        if (preStart > preEnd || preEnd > preOrder.length - 1 || inStart > inEnd || inEnd > inOrder.length - 1){
            return null;
        }
        // 找到根节点
        Node root = new Node(preOrder[preStart]);
        int inOrderRootIndex = 0;
        // 计算根节点在中序遍历数组中的位置
        for (int i = inStart; i <= inEnd; i++){
            if(inOrder[i] == root.data){
                inOrderRootIndex = i;
            }
        }
        // 递归构建左右子树
        Node left = bulidByPreOrderAndInOrder(preOrder,inOrder, preStart+1,inOrderRootIndex-inStart+preStart, inStart,inOrderRootIndex);
        Node right = bulidByPreOrderAndInOrder(preOrder,inOrder, inOrderRootIndex-inStart+preStart+1, preEnd, inOrderRootIndex+1,inEnd);
        root.left = left;
        root.right = right;
        return root;
    }

    /**
     * 根据后序与中序构建二叉树
     * @param postOrder 后序遍历
     * @param inOrder 中序遍历
     * @return 树
     */
    public BinaryTree buildByPostOrderAndInOrderArr(int[] postOrder,int[] inOrder){
        Node node = bulidByPreOrderAndInOrder(postOrder, inOrder, 0, postOrder.length - 1, 0, inOrder.length - 1);
        return new BinaryTree(node);
    }
    private Node buildByPostOrderAndInOrder(int[] postOrder, int[] inOrder, int postStart, int postEnd, int inStart, int inEnd){
        if(postStart > postEnd || postEnd > postOrder.length - 1 || inStart > inEnd || inEnd > inOrder.length - 1){
            return null; // 边界条件，返回空节点
        }
        Node root = new Node(postOrder[postEnd]);
        int inOrderRootIndex = 0;
        for(int i = inStart; i <= inEnd; i++){
            if(inOrder[i] == root.data){
                inOrderRootIndex = i;
            }
        }
        // 递归构建左右子树
        Node left = buildByPostOrderAndInOrder(postOrder,inOrder,postStart,inOrderRootIndex-1,inStart,inOrderRootIndex-1);
        Node right = buildByPostOrderAndInOrder(postOrder,inOrder,inOrderRootIndex+1,postEnd-1,inOrderRootIndex+1,inEnd);
        root.left = left;
        root.right = right;
        return root;
    }

    public List<Integer> order(int type) {
        List<Integer> list = new ArrayList<>();
        order(root,list,type);
        return list;
    }
    private void order(Node node,List<Integer> list,int type){
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
        BinaryTree tree = new BinaryTree();
        tree.root = new Node(1);
        tree.root.left = new Node(2);
        tree.root.right = new Node(3);
        tree.root.left.left = new Node(4);
        tree.root.left.right = new Node(5);
        tree.root.right.left = new Node(6);
        tree.root.right.right = new Node(7);
        System.out.println(tree.order(tree.IN_ORDER));
        System.out.println(tree.order(tree.PRE_ORDER));
        System.out.println(tree.order(tree.POST_ORDER));
        BinaryTree binaryTree = tree.bulidByPreOrderAndInOrderArr(new int[]{1, 2, 4, 5, 3, 6, 7}, new int[]{4, 2, 5, 1, 6, 3, 7});
        System.out.println(binaryTree.order(1));
        tree.buildByPostOrderAndInOrderArr(new int[]{4,5,2,6,7,3,1},new int[]{4,2,5,6,7,3,1});
        System.out.println(tree.order(1));
    }
}
