package dsa;

import java.util.Arrays;

/**
 * https://leetcode.com/problems/minimum-path-sum/
 * 64. Minimum Path Sum
 * Level: Medium
 * Given a m x n grid filled with non-negative numbers, find a path from top left to bottom right, which minimizes the sum of all numbers along its path.
 *
 * Note: You can only move either down or right at any point in time.
 */
public class MinPathSum {
    public static void main(String[] args) {
        int[][] grid = {{1,3,1},{1,5,1},{4,2,1}};
        minPathSum(grid);
    }

    static void minPathSum(int[][] grid) {
        int m = grid.length;
        int[] row = grid[0];
        int[] dp = new int[row.length];
        dp[0] = grid[0][0]; // first row
        // fill first row

    }

}
