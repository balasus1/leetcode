package interview_prep_mastery._02_arrays;

import java.util.*;

/**
 * ============================================================================
 * MODULE 02: ARRAYS - PART 3
 * Topics: Cyclic Sort, Max Sum Subarray of Size K, Smallest Subarray > S,
 * Squaring Sorted Array, Subsets (Power Set), Subsets with Duplicates,
 * Order-agnostic Binary Search, Bitonic Array Maximum.
 * ============================================================================
 */
public class ArrayChallengesPart3 {

    // ------------------------------------------------------------------------
    // 1. Cyclic Sort (Elements in range 1 to n)
    // ------------------------------------------------------------------------
    /**
     * Invariant: Element x should be placed at index (x - 1).
     *
     * Complexity:
     * - Time: O(n) (Each element is swapped at most once into its correct position)
     * - Space: O(1) in-place
     */
    public static void cyclicSort(int[] nums) {
        if (nums == null || nums.length <= 1) return;
        int i = 0;
        while (i < nums.length) {
            int correctIndex = nums[i] - 1;
            if (nums[i] > 0 && nums[i] <= nums.length && nums[i] != nums[correctIndex]) {
                // Swap to correct position
                int temp = nums[i];
                nums[i] = nums[correctIndex];
                nums[correctIndex] = temp;
            } else {
                i++;
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. Maximum Sum Subarray of Size K (Fixed Sliding Window)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static int findMaxSumSubArrayOfSizeK(int k, int[] arr) {
        if (arr == null || arr.length < k || k <= 0) return 0;
        int windowSum = 0;
        int maxSum = Integer.MIN_VALUE;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < arr.length; windowEnd++) {
            windowSum += arr[windowEnd];
            if (windowEnd >= k - 1) {
                maxSum = Math.max(maxSum, windowSum);
                windowSum -= arr[windowStart++];
            }
        }
        return maxSum;
    }

    // ------------------------------------------------------------------------
    // 3. Smallest Subarray With a Greater/Equal Sum (Dynamic Sliding Window)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n) (Each element enters and exits window at most once)
     * - Space: O(1)
     */
    public static int findMinSubArrayLen(int S, int[] arr) {
        if (arr == null || arr.length == 0) return 0;
        int windowSum = 0;
        int minLength = Integer.MAX_VALUE;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < arr.length; windowEnd++) {
            windowSum += arr[windowEnd];
            while (windowSum >= S) {
                minLength = Math.min(minLength, windowEnd - windowStart + 1);
                windowSum -= arr[windowStart++];
            }
        }
        return minLength == Integer.MAX_VALUE ? 0 : minLength;
    }

    // ------------------------------------------------------------------------
    // 4. Squaring a Sorted Array (Two Pointers from ends)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(n) for resulting sorted squared array
     */
    public static int[] makeSquares(int[] arr) {
        if (arr == null || arr.length == 0) return new int[0];
        int n = arr.length;
        int[] squares = new int[n];
        int left = 0, right = n - 1;
        int highestSquareIdx = n - 1;

        while (left <= right) {
            int leftSquare = arr[left] * arr[left];
            int rightSquare = arr[right] * arr[right];
            if (leftSquare > rightSquare) {
                squares[highestSquareIdx--] = leftSquare;
                left++;
            } else {
                squares[highestSquareIdx--] = rightSquare;
                right--;
            }
        }
        return squares;
    }

    // ------------------------------------------------------------------------
    // 5. Subsets (Power Set - Cascading / Backtracking)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n * 2^n)
     * - Space: O(n * 2^n) to store all subsets
     */
    public static List<List<Integer>> findSubsets(int[] nums) {
        List<List<Integer>> subsets = new ArrayList<>();
        subsets.add(new ArrayList<>()); // Start with empty set
        if (nums == null) return subsets;

        for (int currentNumber : nums) {
            int n = subsets.size();
            for (int i = 0; i < n; i++) {
                List<Integer> set = new ArrayList<>(subsets.get(i));
                set.add(currentNumber);
                subsets.add(set);
            }
        }
        return subsets;
    }

    // ------------------------------------------------------------------------
    // 6. Subsets With Duplicates
    // ------------------------------------------------------------------------
    /**
     * Invariant: Sort array first. If current is duplicate of previous,
     * only add it to the subsets created in the previous step.
     *
     * Complexity:
     * - Time: O(n * 2^n)
     * - Space: O(n * 2^n)
     */
    public static List<List<Integer>> findSubsetsWithDuplicates(int[] nums) {
        List<List<Integer>> subsets = new ArrayList<>();
        if (nums == null || nums.length == 0) return subsets;
        Arrays.sort(nums);
        subsets.add(new ArrayList<>());

        int startIndex = 0, endIndex = 0;
        for (int i = 0; i < nums.length; i++) {
            startIndex = 0;
            // If current element is same as previous, only add to subsets added in last step
            if (i > 0 && nums[i] == nums[i - 1]) {
                startIndex = endIndex + 1;
            }
            endIndex = subsets.size() - 1;
            for (int j = startIndex; j <= endIndex; j++) {
                List<Integer> set = new ArrayList<>(subsets.get(j));
                set.add(nums[i]);
                subsets.add(set);
            }
        }
        return subsets;
    }

    // ------------------------------------------------------------------------
    // 7. Order-Agnostic Binary Search
    // ------------------------------------------------------------------------
    /**
     * Autodetects whether array is sorted in ascending or descending order.
     *
     * Complexity:
     * - Time: O(log n)
     * - Space: O(1)
     */
    public static int orderAgnosticBinarySearch(int[] arr, int target) {
        if (arr == null || arr.length == 0) return -1;
        int start = 0, end = arr.length - 1;
        boolean isAscending = arr[start] <= arr[end];

        while (start <= end) {
            int mid = start + (end - start) / 2;
            if (arr[mid] == target) return mid;

            if (isAscending) {
                if (target < arr[mid]) end = mid - 1;
                else start = mid + 1;
            } else {
                if (target > arr[mid]) end = mid - 1;
                else start = mid + 1;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // 8. Bitonic Array Maximum (Mountain Array Peak)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(log n)
     * - Space: O(1)
     */
    public static int findMaxInBitonicArray(int[] arr) {
        if (arr == null || arr.length == 0) return -1;
        int start = 0, end = arr.length - 1;
        while (start < end) {
            int mid = start + (end - start) / 2;
            if (arr[mid] > arr[mid + 1]) {
                // Descending part: peak is at mid or to the left
                end = mid;
            } else {
                // Ascending part: peak is to the right
                start = mid + 1;
            }
        }
        return arr[start];
    }

    // ------------------------------------------------------------------------
    // Test Suite for Part 3
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING ARRAY CHALLENGES PART 3 TEST SUITE ");
        System.out.println("=================================================");

        // 1. Cyclic Sort
        int[] cArr = {3, 1, 5, 4, 2};
        cyclicSort(cArr);
        assert Arrays.equals(cArr, new int[]{1, 2, 3, 4, 5});

        // 2. Max Sum Subarray of Size K
        assert findMaxSumSubArrayOfSizeK(3, new int[]{2, 1, 5, 1, 3, 2}) == 9;

        // 3. Smallest Subarray With Greater Sum
        assert findMinSubArrayLen(7, new int[]{2, 1, 5, 2, 3, 2}) == 2; // [5, 2]

        // 4. Squaring Sorted Array
        int[] sq = makeSquares(new int[]{-4, -1, 0, 3, 10});
        assert Arrays.equals(sq, new int[]{0, 1, 9, 16, 100});

        // 5. Subsets (Power Set)
        List<List<Integer>> subsets = findSubsets(new int[]{1, 3});
        assert subsets.size() == 4; // [], [1], [3], [1,3]

        // 6. Subsets With Duplicates
        List<List<Integer>> dupSubsets = findSubsetsWithDuplicates(new int[]{1, 3, 3});
        assert dupSubsets.size() == 6; // [], [1], [3], [1,3], [3,3], [1,3,3]

        // 7. Order-Agnostic Binary Search
        assert orderAgnosticBinarySearch(new int[]{4, 6, 10}, 10) == 2;
        assert orderAgnosticBinarySearch(new int[]{10, 6, 4}, 10) == 0;

        // 8. Bitonic Array Maximum
        assert findMaxInBitonicArray(new int[]{1, 3, 8, 12, 4, 2}) == 12;

        System.out.println(" ARRAY CHALLENGES PART 3 ALL PASSED!");
        System.out.println("=================================================");
    }
}
