package com.spring.springthread.datastruct.heap;

/**
 * @author spring
 * @version 1.0
 * @apiNote 二叉堆
 * @since 2024/12/4 17:53:48
 */
@SuppressWarnings("all")
public class BinaryHeap {
    Integer[] nums;
    int size = 0;

    public void insert(Integer num){}
    public Integer delMax(){return null;}

    public void pushUp(int index){}
    public void pushDown(int index){}
    public boolean less(int i, int j){
        return nums[i].compareTo(nums[j]) < 0;
    }
    public void swap(int i, int j){
        Integer temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }
}
