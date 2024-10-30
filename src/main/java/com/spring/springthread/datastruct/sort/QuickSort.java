package com.spring.springthread.datastruct.sort;

/**
 * @author spring
 * @since 2024/10/30 15:55:20
 * @apiNote
 * @version 1.0
 */
public class QuickSort {
    public static void quickSort(int[] arr){
        quickSort(arr,0,arr.length-1);
    }
    public static void quickSort(int[] arr,int start,int end){
        if(start > end) {
            return;
        }
        int i = start,j = end, base = arr[start];
        while(i != j){
            while(arr[j] >= base && i < j) {
                j--;
            }
            while(arr[i] <= base && i < j) {
                i++;
            }
            if(i < j){
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        arr[start] = arr[i];
        arr[i] = base;
        quickSort(arr,start,i-1);
        quickSort(arr,i+1,end);
    }

    public static void main(String[] args) {
        int[] arr = {1,2,3,4,5,6,7,8,9,10};
        quickSort(arr);
        for (int j : arr) {
            System.out.print(j + " ");
        }
    }
}
