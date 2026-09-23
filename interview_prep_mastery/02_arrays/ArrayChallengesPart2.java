package interview_prep_mastery._02_arrays;

import java.util.*;

/**
 * ============================================================================
 * MODULE 02: ARRAYS - PART 2
 * Topics: Binary Search, Sliding Window Max, Rotated Array Search,
 * Smallest Common Number, Low/High Index, Move Zeros, Stock Buy/Sell,
 * Merge Intervals, QuickSort.
 * ============================================================================
 */
public class ArrayChallengesPart2 {

    // ------------------------------------------------------------------------
    // 1. Binary Search on a Sorted Array (Avoids integer overflow)
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Is the array sorted in ascending order? (Yes)
     * - Does mid calculation prevent overflow? (Use mid = low + (high - low) / 2)
     *
     * Complexity:
     * - Time: O(log n)
     * - Space: O(1) iterative
     */
    public static int binarySearch(int[] arr, int target) {
        if (arr == null || arr.length == 0) return -1;
        int low = 0, high = arr.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) return mid;
            else if (arr[mid] < target) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // 2. Find Maximum in Sliding Window
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - What if window size k > array length? (k is capped at array length)
     * - Approach: Monotonic decreasing Deque storing indices.
     *
     * Complexity:
     * - Time: O(n) (Each element is pushed and popped at most once)
     * - Space: O(k) for Deque
     */
    public static int[] maxSlidingWindow(int[] nums, int k) {
        if (nums == null || nums.length == 0 || k <= 0) return new int[0];
        int n = nums.length;
        if (k > n) k = n;
        int[] result = new int[n - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            // Remove elements outside current window [i - k + 1, i]
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                deque.pollFirst();
            }
            // Remove elements smaller than current element nums[i] from tail
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }
            deque.offerLast(i);

            // Window valid once i >= k - 1
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // 3. Search in a Rotated Sorted Array
    // ------------------------------------------------------------------------
    /**
     * Key Invariant: At least one half of the rotated array is always strictly sorted.
     *
     * Complexity:
     * - Time: O(log n)
     * - Space: O(1)
     */
    public static int searchRotatedArray(int[] arr, int target) {
        if (arr == null || arr.length == 0) return -1;
        int low = 0, high = arr.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] == target) return mid;

            // Check if left half is sorted
            if (arr[low] <= arr[mid]) {
                if (target >= arr[low] && target < arr[mid]) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            } else { // Right half is sorted
                if (target > arr[mid] && target <= arr[high]) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // 4. Find the Smallest Common Number in 3 Sorted Arrays
    // ------------------------------------------------------------------------
    /**
     * 3-Pointer technique: Increment the pointer with the smallest current value.
     *
     * Complexity:
     * - Time: O(n1 + n2 + n3)
     * - Space: O(1)
     */
    public static int findSmallestCommonNumber(int[] a, int[] b, int[] c) {
        if (a == null || b == null || c == null) return -1;
        int i = 0, j = 0, k = 0;
        while (i < a.length && j < b.length && k < c.length) {
            if (a[i] == b[j] && b[j] == c[k]) {
                return a[i];
            }
            int minVal = Math.min(a[i], Math.min(b[j], c[k]));
            if (a[i] == minVal) i++;
            if (b[j] == minVal) j++;
            if (c[k] == minVal) k++;
        }
        return -1; // No common number found
    }

    // ------------------------------------------------------------------------
    // 5. Find Low/High Index of an Element in a Sorted Array
    // ------------------------------------------------------------------------
    /**
     * Binary Search finding first occurrence (low index) and last occurrence (high index).
     *
     * Complexity:
     * - Time: O(log n)
     * - Space: O(1)
     */
    public static int findLowIndex(int[] arr, int target) {
        if (arr == null || arr.length == 0) return -1;
        int low = 0, high = arr.length - 1;
        int result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] >= target) {
                if (arr[mid] == target) result = mid;
                high = mid - 1; // Keep searching left
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    public static int findHighIndex(int[] arr, int target) {
        if (arr == null || arr.length == 0) return -1;
        int low = 0, high = arr.length - 1;
        int result = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (arr[mid] <= target) {
                if (arr[mid] == target) result = mid;
                low = mid + 1; // Keep searching right
            } else {
                high = mid - 1;
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // 6. Move All Zeros to the Beginning of the Array
    // ------------------------------------------------------------------------
    /**
     * Backward scan two-pointer: Read from right to left.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) in-place
     */
    public static void moveZerosToBeginning(int[] arr) {
        if (arr == null || arr.length <= 1) return;
        int writeIdx = arr.length - 1;
        for (int readIdx = arr.length - 1; readIdx >= 0; readIdx--) {
            if (arr[readIdx] != 0) {
                arr[writeIdx--] = arr[readIdx];
            }
        }
        while (writeIdx >= 0) {
            arr[writeIdx--] = 0;
        }
    }

    // ------------------------------------------------------------------------
    // 7. Stock Buy Sell to Maximize Profit (Single Transaction)
    // ------------------------------------------------------------------------
    /**
     * Valley-Peak approach: Track minPrice and maxProfit in single scan.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static int maxStockProfit(int[] prices) {
        if (prices == null || prices.length < 2) return 0;
        int minPrice = prices[0];
        int maxProfit = 0;
        for (int i = 1; i < prices.length; i++) {
            maxProfit = Math.max(maxProfit, prices[i] - minPrice);
            minPrice = Math.min(minPrice, prices[i]);
        }
        return maxProfit;
    }

    // ------------------------------------------------------------------------
    // 8. Merge an Array With Overlapping Intervals
    // ------------------------------------------------------------------------
    public static class Interval {
        public int start, end;
        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Interval other = (Interval) o;
            return this.start == other.start && this.end == other.end;
        }
        @Override
        public String toString() {
            return "[" + start + ", " + end + "]";
        }
    }

    /**
     * Complexity:
     * - Time: O(n log n) sorting
     * - Space: O(n) for merged list
     */
    public static List<Interval> mergeIntervals(List<Interval> intervals) {
        if (intervals == null || intervals.size() <= 1) return intervals;
        intervals.sort(Comparator.comparingInt(a -> a.start));
        List<Interval> merged = new ArrayList<>();
        Interval current = intervals.get(0);

        for (int i = 1; i < intervals.size(); i++) {
            Interval next = intervals.get(i);
            if (next.start <= current.end) {
                // Overlapping: expand end
                current.end = Math.max(current.end, next.end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    // ------------------------------------------------------------------------
    // 9. QuickSort Algorithm (In-place Lomuto Partitioning)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n log n) average, O(n^2) worst case
     * - Space: O(log n) call stack
     */
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pIndex = partition(arr, low, high);
            quickSort(arr, low, pIndex - 1);
            quickSort(arr, pIndex + 1, high);
        }
    }

    private static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }
        swap(arr, i + 1, high);
        return i + 1;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Part 2
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING ARRAY CHALLENGES PART 2 TEST SUITE ");
        System.out.println("=================================================");

        // 1. Binary Search
        int[] sorted = {1, 3, 5, 7, 9, 11};
        assert binarySearch(sorted, 7) == 3;
        assert binarySearch(sorted, 8) == -1;

        // 2. Sliding Window Max
        int[] wArr = {1, 3, -1, -3, 5, 3, 6, 7};
        int[] wMax = maxSlidingWindow(wArr, 3);
        assert Arrays.equals(wMax, new int[]{3, 3, 5, 5, 6, 7});

        // 3. Search Rotated Array
        int[] rot = {4, 5, 6, 7, 0, 1, 2};
        assert searchRotatedArray(rot, 0) == 4;
        assert searchRotatedArray(rot, 3) == -1;

        // 4. Smallest Common Number
        int[] a = {6, 7, 10, 25, 30, 63, 64};
        int[] b = {0, 4, 5, 6, 7, 8, 50};
        int[] c = {1, 6, 10, 14};
        assert findSmallestCommonNumber(a, b, c) == 6;

        // 5. Low/High Index
        int[] dupArr = {1, 1, 1, 2, 2, 2, 2, 3, 3, 5};
        assert findLowIndex(dupArr, 2) == 3;
        assert findHighIndex(dupArr, 2) == 6;

        // 6. Move Zeros to Beginning
        int[] zArr = {1, 10, 20, 0, 59, 63, 0, 88, 0};
        moveZerosToBeginning(zArr);
        assert zArr[0] == 0 && zArr[1] == 0 && zArr[2] == 0 && zArr[3] != 0;

        // 7. Stock Buy Sell
        assert maxStockProfit(new int[]{7, 1, 5, 3, 6, 4}) == 5;

        // 8. Merge Intervals
        List<Interval> intervals = new ArrayList<>(Arrays.asList(
                new Interval(1, 5), new Interval(3, 7), new Interval(4, 6), new Interval(8, 10)
        ));
        List<Interval> merged = mergeIntervals(intervals);
        assert merged.size() == 2;
        assert merged.get(0).equals(new Interval(1, 7));
        assert merged.get(1).equals(new Interval(8, 10));

        // 9. QuickSort
        int[] qArr = {9, 3, 7, 5, 6, 4, 8, 2};
        quickSort(qArr, 0, qArr.length - 1);
        assert Arrays.equals(qArr, new int[]{2, 3, 4, 5, 6, 7, 8, 9});

        System.out.println(" ARRAY CHALLENGES PART 2 ALL PASSED!");
        System.out.println("=================================================");
    }
}
