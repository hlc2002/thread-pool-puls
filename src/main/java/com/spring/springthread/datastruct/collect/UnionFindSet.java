package com.spring.springthread.datastruct.collect;

/**
 * @author spring
 * @since 2024/10/28 13:31:16
 * @apiNote 并查集
 * @version 1.0
 */
@SuppressWarnings("all")
public class UnionFindSet {
    // 链接关系集合 i 的父亲是 connAr[i]
    int[] connAr;

    public UnionFindSet(int n) {
        connAr = new int[n];
        for (int i = 0; i < n; i++) {
            connAr[i] = i;
        }
    }

    /**
     * 查找集合根节点
     * @param val 当前值
     * @return 根节点值
     */
    public int findRoot(int val){
        while (connAr[val] != val){
            val = connAr[val];
        }
        return val;
    }

    /**
     * 合并集合
     * @param root1 集合1的根
     * @param root2 集合2的根节点
     */
    public void union(int root1, int root2){
        connAr[root2] = root1;
    }

    public int queryCollectCnt(){
        int cnt = 0;
        for (int i = 0 ; i < connAr.length; i++){
            if(i == connAr[i]){
                cnt ++;
            }
        }
        return cnt;
    }
    public static void main(String[] args) {
        UnionFindSet unionFindSet = new UnionFindSet(10);
        unionFindSet.union(1,2);
        unionFindSet.union(3,4);
        unionFindSet.union(4,5);
        unionFindSet.union(5,7);
        System.out.println(unionFindSet.findRoot(3));
        System.out.println(unionFindSet.findRoot(7));
        System.out.println(unionFindSet.findRoot(5));
        System.out.println(unionFindSet.queryCollectCnt());
    }
}
