package interview_prep_mastery._02_arrays;

import java.util.*;

/**
 * ============================================================================
 * MODULE 02: ARRAYS - PART 1
 * Topics: 2D Arrays, Challenges 1 to 11
 * ============================================================================
 */
public class ArrayChallengesPart1 {

    // ------------------------------------------------------------------------
    // Topic: Two Dimensional Arrays (Matrix Operations)
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Is the matrix guaranteed to be rectangular (m x n) or could it be jagged?
     * - Can matrix be empty or null?
     *
     * Complexity:
     * - Time: O(R * C) to traverse and compute row/col sums
     * - Space: O(1) auxiliary
     */
    public static int[] get2DMatrixRowSums(int[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) return new int[0];
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[] rowSums = new int[rows];
        for (int r = 0; r < rows; r++) {
            int sum = 0;
            for (int c = 0; c < cols; c++) {
                sum += matrix[r][c];
            }
            rowSums[r] = sum;
        }
        return rowSums;
    }

    // ------------------------------------------------------------------------
    // Challenge 1: Remove Even Integers from an Array
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Should the relative order of odd integers be preserved? (Yes)
     * - What if all numbers are even? (Returns empty array)
     *
     * Complexity:
     * - Time: O(n) single pass
     * - Space: O(n) for resulting array (or O(1) in-place if list)
     */
    public static int[] removeEven(int[] arr) {
        if (arr == null) return new int[0];
        int oddCount = 0;
        for (int x : arr) {
            if (x % 2 != 0) oddCount++;
        }
        int[] result = new int[oddCount];
        int idx = 0;
        for (int x : arr) {
            if (x % 2 != 0) {
                result[idx++] = x;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 2: Merge Two Sorted Arrays
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Are the inputs already sorted in ascending order? (Yes)
     * - Are duplicates allowed across or within arrays? (Yes)
     *
     * Complexity:
     * - Time: O(n + m) optimal two-pointer merge
     * - Space: O(n + m) for merged array
     */
    public static int[] mergeSortedArrays(int[] arr1, int[] arr2) {
        if (arr1 == null) return arr2 == null ? new int[0] : arr2;
        if (arr2 == null) return arr1;
        int n = arr1.length, m = arr2.length;
        int[] merged = new int[n + m];
        int i = 0, j = 0, k = 0;
        while (i < n && j < m) {
            if (arr1[i] <= arr2[j]) {
                merged[k++] = arr1[i++];
            } else {
                merged[k++] = arr2[j++];
            }
        }
        while (i < n) merged[k++] = arr1[i++];
        while (j < m) merged[k++] = arr2[j++];
        return merged;
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Find Two Numbers that Add up to "n" (Two Sum)
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Can an element be used twice? (No, distinct indices required)
     * - What if multiple pairs exist? (Return any valid pair)
     *
     * Complexity:
     * - Brute Force: O(n^2) time, O(1) space
     * - Optimal (HashSet): O(n) time, O(n) space
     */
    public static int[] findSum(int[] arr, int target) {
        if (arr == null || arr.length < 2) return new int[0];
        Set<Integer> seen = new HashSet<>();
        for (int num : arr) {
            int complement = target - num;
            if (seen.contains(complement)) {
                return new int[]{complement, num};
            }
            seen.add(num);
        }
        return new int[0];
    }

    // ------------------------------------------------------------------------
    // Challenge 4: Array of Products of All Elements Except Itself
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Can we use division? (Constraint: Must NOT use division operator!)
     * - How to handle 0s in the array? (Handled seamlessly via prefix/suffix products)
     *
     * Complexity:
     * - Brute Force: O(n^2) time
     * - Optimal: O(n) time, O(1) auxiliary space (excluding output array)
     */
    public static int[] findProduct(int[] arr) {
        if (arr == null || arr.length == 0) return new int[0];
        int n = arr.length;
        int[] result = new int[n];
        
        // Pass 1: Prefix products
        int prefix = 1;
        for (int i = 0; i < n; i++) {
            result[i] = prefix;
            prefix *= arr[i];
        }
        
        // Pass 2: Suffix products
        int suffix = 1;
        for (int i = n - 1; i >= 0; i--) {
            result[i] *= suffix;
            suffix *= arr[i];
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 5: Find Minimum Value in Array
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n) single pass
     * - Space: O(1) auxiliary
     */
    public static int findMinimum(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array must not be empty");
        }
        int min = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] < min) min = arr[i];
        }
        return min;
    }

    // ------------------------------------------------------------------------
    // Challenge 6: First Non-Repeating Integer in an Array
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n) two-pass frequency lookup
     * - Space: O(n) for frequency map
     */
    public static int findFirstUnique(int[] arr) {
        if (arr == null || arr.length == 0) return -1;
        Map<Integer, Integer> freq = new LinkedHashMap<>();
        for (int x : arr) {
            freq.put(x, freq.getOrDefault(x, 0) + 1);
        }
        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            if (entry.getValue() == 1) {
                return entry.getKey();
            }
        }
        return -1; // All repeating
    }

    // ------------------------------------------------------------------------
    // Challenge 7: Find Second Maximum Value in an Array
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - What if array has duplicates of the max value? (Find strictly second distinct max)
     *
     * Complexity:
     * - Time: O(n) single scan
     * - Space: O(1)
     */
    public static int findSecondMaximum(int[] arr) {
        if (arr == null || arr.length < 2) {
            throw new IllegalArgumentException("Array must have at least two elements");
        }
        int max = Integer.MIN_VALUE;
        int secondMax = Integer.MIN_VALUE;
        for (int x : arr) {
            if (x > max) {
                secondMax = max;
                max = x;
            } else if (x > secondMax && x != max) {
                secondMax = x;
            }
        }
        return secondMax;
    }

    // ------------------------------------------------------------------------
    // Challenge 8: Right Rotate the Array by One Index (or K Indices)
    // ------------------------------------------------------------------------
    /**
     * Reversal Algorithm:
     * 1. Reverse entire array.
     * 2. Reverse first k elements.
     * 3. Reverse remaining n-k elements.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) in-place!
     */
    public static void rotateArrayByK(int[] arr, int k) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        k = k % n;
        if (k < 0) k += n;
        reverse(arr, 0, n - 1);
        reverse(arr, 0, k - 1);
        reverse(arr, k, n - 1);
    }

    private static void reverse(int[] arr, int start, int end) {
        while (start < end) {
            int temp = arr[start];
            arr[start++] = arr[end];
            arr[end--] = temp;
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 9: Re-arrange Positive & Negative Values
    // ------------------------------------------------------------------------
    /**
     * In-place Partitioning (similar to QuickSort partition):
     * Negatives on left, Positives on right.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) in-place
     */
    public static void reArrangePosNeg(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int j = 0; // Pointer for next negative placement
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < 0) {
                if (i != j) {
                    int temp = arr[i];
                    arr[i] = arr[j];
                    arr[j] = temp;
                }
                j++;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 10: Rearrange Sorted Array in Max/Min Form
    // (arr[0]=max, arr[1]=min, arr[2]=2nd max, arr[3]=2nd min...)
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Can we achieve O(1) extra space on an integer array?
     * - YES! Using Modulo Arithmetic: arr[i] = arr[i] + (arr[target] % max_elem) * max_elem
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) auxiliary in-place encoding!
     */
    public static void maxMinRearrange(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int n = arr.length;
        int maxElem = arr[n - 1] + 1; // Used as modulo base
        int maxIdx = n - 1;
        int minIdx = 0;

        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                arr[i] += (arr[maxIdx] % maxElem) * maxElem;
                maxIdx--;
            } else {
                arr[i] += (arr[minIdx] % maxElem) * maxElem;
                minIdx++;
            }
        }

        // Decode original values
        for (int i = 0; i < n; i++) {
            arr[i] /= maxElem;
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 11: Find the Sum of Maximum Sum Subarray (Kadane's Algorithm)
    // ------------------------------------------------------------------------
    /**
     * Dynamic Programming state:
     * currentMax = max(arr[i], currentMax + arr[i])
     * globalMax = max(globalMax, currentMax)
     *
     * Complexity:
     * - Time: O(n) single pass
     * - Space: O(1) auxiliary
     */
    public static int findMaxSubArray(int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        int currentMax = arr[0];
        int globalMax = arr[0];
        for (int i = 1; i < arr.length; i++) {
            currentMax = Math.max(arr[i], currentMax + arr[i]);
            globalMax = Math.max(globalMax, currentMax);
        }
        return globalMax;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Part 1
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING ARRAY CHALLENGES PART 1 TEST SUITE ");
        System.out.println("=================================================");

        // 2D Array Matrix
        int[][] mat = {{1, 2, 3}, {4, 5, 6}};
        int[] rowSums = get2DMatrixRowSums(mat);
        assert Arrays.equals(rowSums, new int[]{6, 15});

        // Challenge 1: Remove Even
        int[] oddRes = removeEven(new int[]{1, 2, 4, 5, 10, 6, 3});
        assert Arrays.equals(oddRes, new int[]{1, 5, 3});

        // Challenge 2: Merge Sorted
        int[] merged = mergeSortedArrays(new int[]{1, 3, 5}, new int[]{2, 4, 6});
        assert Arrays.equals(merged, new int[]{1, 2, 3, 4, 5, 6});

        // Challenge 3: Find Sum
        int[] twoSum = findSum(new int[]{1, 21, 3, 14, 5, 60, 7, 6}, 27);
        assert (twoSum[0] + twoSum[1] == 27);

        // Challenge 4: Product Except Self
        int[] prod = findProduct(new int[]{1, 2, 3, 4});
        assert Arrays.equals(prod, new int[]{24, 12, 8, 6});

        // Challenge 5: Find Min
        assert findMinimum(new int[]{9, 2, 3, 6}) == 2;

        // Challenge 6: First Unique
        assert findFirstUnique(new int[]{9, 2, 3, 2, 6, 6}) == 9;

        // Challenge 7: Second Max
        assert findSecondMaximum(new int[]{9, 2, 3, 6}) == 6;

        // Challenge 8: Rotate by K
        int[] rot = {1, 2, 3, 4, 5};
        rotateArrayByK(rot, 2);
        assert Arrays.equals(rot, new int[]{4, 5, 1, 2, 3});

        // Challenge 9: Re-arrange pos/neg
        int[] posNeg = {10, -1, 20, 4, 5, -9, -6};
        reArrangePosNeg(posNeg);
        // Verify all negatives are placed first
        int lastNeg = -1;
        for (int i = 0; i < posNeg.length; i++) {
            if (posNeg[i] < 0) lastNeg = i;
        }
        for (int i = 0; i <= lastNeg; i++) {
            assert posNeg[i] < 0;
        }

        // Challenge 10: Max/Min Form
        int[] sortedArr = {1, 2, 3, 4, 5, 6, 7};
        maxMinRearrange(sortedArr);
        assert Arrays.equals(sortedArr, new int[]{7, 1, 6, 2, 5, 3, 4});

        // Challenge 11: Kadane's Max Subarray
        assert findMaxSubArray(new int[]{-4, 2, -5, 1, 2, 3, 6, -5, 1}) == 12;

        System.out.println(" ARRAY CHALLENGES PART 1 (CHALLENGES 1-11) ALL PASSED!");
        System.out.println("=================================================");
    }
}
