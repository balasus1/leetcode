package dsa;

import java.util.Arrays;

public class FindPeakElementII {

    /**
     * Finds a peak element in a 2D grid where perimeter values are -1.
     *
     * Approach: Binary Search on Columns + Finding Column Maximum
     *
     * Time Complexity: O(m * log(n)) where m is number of rows, n is number of columns
     * Space Complexity: O(1)
     *
     * @param mat 0-indexed m x n matrix
     * @return [row, col] index of any peak element
     */
    public int[] findPeakGrid(int[][] mat) {
        int m = mat.length;
        int n = mat[0].length;

        int leftCol = 0;
        int rightCol = n - 1;
        int step = 1;

        System.out.println("\n--------------------------------------------------");
        System.out.printf("Matrix Size: %d x %d\n", m, n);
        System.out.println("--------------------------------------------------");

        while (leftCol <= rightCol) {
            int midCol = leftCol + (rightCol - leftCol) / 2;

            // Step 1: Find the global maximum element in the midCol column
            int maxRow = 0;
            for (int r = 0; r < m; r++) {
                if (mat[r][midCol] > mat[maxRow][midCol]) {
                    maxRow = r;
                }
            }

            int currVal = mat[maxRow][midCol];
            int leftNeighbor = (midCol - 1 >= 0) ? mat[maxRow][midCol - 1] : -1;
            int rightNeighbor = (midCol + 1 < n) ? mat[maxRow][midCol + 1] : -1;

            System.out.printf("Step %d: Column Range [%d..%d], midCol = %d\n", step++, leftCol, rightCol, midCol);
            System.out.printf("   -> Max element in col %d is mat[%d][%d] = %d (top/bottom are smaller)\n",
                    midCol, maxRow, midCol, currVal);
            System.out.printf("   -> Left neighbor: %d, Right neighbor: %d\n", leftNeighbor, rightNeighbor);

            // Step 2: Check if current element is greater than its left and right neighbors
            if (currVal > leftNeighbor && currVal > rightNeighbor) {
                System.out.printf("   -> PEAK FOUND at [%d, %d] with value %d!\n", maxRow, midCol, currVal);
                return new int[] { maxRow, midCol };
            } else if (rightNeighbor > currVal) {
                System.out.printf("   -> Right neighbor (%d) > Curr (%d) [Uphill to the RIGHT]\n", rightNeighbor, currVal);
                System.out.printf("   -> Action: Move leftCol = %d\n\n", midCol + 1);
                leftCol = midCol + 1;
            } else {
                System.out.printf("   -> Left neighbor (%d) > Curr (%d) [Uphill to the LEFT]\n", leftNeighbor, currVal);
                System.out.printf("   -> Action: Move rightCol = %d\n\n", midCol - 1);
                rightCol = midCol - 1;
            }
        }

        return new int[] { -1, -1 };
    }

    public static void main(String[] args) {
        FindPeakElementII solver = new FindPeakElementII();

        // Test Case 1: Example from problem
        int[][] mat1 = {
            { 1, 4 },
            { 3, 2 }
        };

        // Test Case 2: 3x3 matrix
        int[][] mat2 = {
            { 10, 20, 15 },
            { 21, 30, 14 },
            {  7, 16, 32 }
        };

        // Test Case 3: 1x4 matrix (1D row)
        int[][] mat3 = {
            { 1, 3, 5, 2 }
        };

        // Test Case 4: 4x1 matrix (1D column)
        int[][] mat4 = {
            { 1 },
            { 5 },
            { 3 },
            { 2 }
        };

        int[][][] allTests = { mat1, mat2, mat3, mat4 };

        for (int i = 0; i < allTests.length; i++) {
            System.out.printf("\n=================== TEST CASE %d ===================", i + 1);
            int[] peak = solver.findPeakGrid(allTests[i]);
            System.out.printf("Result: [%d, %d] -> Value = %d\n", peak[0], peak[1], allTests[i][peak[0]][peak[1]]);
        }
    }
}
