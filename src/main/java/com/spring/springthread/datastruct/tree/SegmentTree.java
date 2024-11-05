package com.spring.springthread.datastruct.tree;

/**
 * @author spring
 * @version 1.0
 * @apiNote
 * @since 2024/11/5 19:26:09
 */
@SuppressWarnings("all")
public class SegmentTree {
    int left; // 线段树的左区间索引
    int right; // 线段树的右区间索引
    SegmentTree leftChild; // 左节点
    SegmentTree rightChild; // 右节点
    int sum; // 叶子节点的和
    int max; // 叶子节点的最大值
    public SegmentTree(){}

    @Override
    public String toString() {
        return "SegmentTree{" +
                "left=" + left +
                ", right=" + right +
                ", leftChild=" + leftChild +
                ", rightChild=" + rightChild +
                ", sum=" + sum +
                ", max=" + max +
                '}';
    }

    public SegmentTree build(int[] arr, int left, int right){
        SegmentTree tree = new SegmentTree();
        tree.left = left;
        tree.right = right;
        if(left == right){ // 叶子节点时直接返回
            tree.sum = arr[left];
            tree.max = arr[left];
            return tree;
        }
        int mid = (left + right) >>> 1;
        tree.leftChild = build(arr,left, mid); // 递归构建左树
        tree.rightChild = build(arr,mid + 1,right); // 递归构建右树
        tree.max = Math.max(tree.leftChild.max, tree.rightChild.max);
        tree.sum = tree.leftChild.sum + tree.rightChild.sum; // 这里相当于线段树的 pushUp 向上传递值
        return tree;
    }

    public static void main(String[] args) {
        SegmentTree segmentTree = new SegmentTree();
        SegmentTree tree = segmentTree.build(new int[]{1, 3, 2, 8, 4, 5, 0}, 0, 6);
        System.out.println(tree.toString());
    }

}
