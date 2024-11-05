package com.spring.springthread;

import lombok.val;

import java.util.PriorityQueue;

/**
 * @author spring
 * @version 1.0
 * @apiNote
 * @since 2024/11/1 17:44:07
 */
public class Solution {
    public static void main(String[] args) {
        // 3 4 5 49 50 92 93 99 100
//        int[] test = new int[]{3,4,5,49,50,92,93,99,100};
//        for(int num : test){
//            System.out.print(compute(num)+"  ");
//        }
//        String num = "24123";
//        System.out.println(isBalanced(num));
    }

    public static long compute(int n){
        // 斐波那契数列 1 1 2 3 5
        //  我们可以发现 f[n] = f[n-1] + f[n-2]
        long[] func = new long[n+1];
        func[1] = 1;
        func[2] = 1;
        for(int i = 3; i <= n; i++) {
            func[i] = func[i - 1] + func[i - 2];
        }
        return func[n];
    }

}
