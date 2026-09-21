package dsa;

import java.util.Arrays;

public class FindPeekElement {

    /**
     * Finds a peak element in an array and returns its index.
     * Includes step-by-step debug logging to trace how search space reduces.
     *
     * Time Complexity: O(log n)
     * Space Complexity: O(1)
     */
    public int findPeakElement(int[] nums) {
        int left = 0;
        int right = nums.length - 1;
        int step = 1;

        System.out.println("\n--------------------------------------------------");
        System.out.println("Input Array: " + Arrays.toString(nums));
        System.out.println("Length: " + nums.length);
        System.out.println("--------------------------------------------------");

        while (left < right) {
            int mid = left + (right - left) / 2;

            System.out.printf("Step %d: Range [%d..%d] (values: nums[%d]=%d, nums[%d]=%d)\n",
                    step++, left, right, left, nums[left], right, nums[right]);
            System.out.printf("   -> mid = %d (nums[mid] = %d), next = %d (nums[mid+1] = %d)\n",
                    mid, nums[mid], mid + 1, nums[mid + 1]);

            if (nums[mid] > nums[mid + 1]) {
                System.out.printf("   -> nums[%d] (%d) > nums[%d] (%d) [DESCENDING / DOWNHILL]\n",
                        mid, nums[mid], mid + 1, nums[mid + 1]);
                System.out.printf("   -> Action: Peak must be at mid or left. Move right = mid (%d)\n\n", mid);
                right = mid;
            } else {
                System.out.printf("   -> nums[%d] (%d) < nums[%d] (%d) [ASCENDING / UPHILL]\n",
                        mid, nums[mid], mid + 1, nums[mid + 1]);
                System.out.printf("   -> Action: Peak must be to the right. Move left = mid + 1 (%d)\n\n", mid + 1);
                left = mid + 1;
            }
        }

        System.out.printf("CONVERGED: left == right == %d (Peak Value: nums[%d] = %d)\n", left, left, nums[left]);
        return left;
    }

    public static void main(String[] args) {
        FindPeekElement solver = new FindPeekElement();

        int[][] testCases = {
                { 1, 2, 3, 1 }, // Single peak in middle
                { 1, 2, 1, 3, 5, 6, 4 }, // Multiple peaks (index 1 or index 5)
                { 1, 2, 3, 4, 5 }, // Strictly increasing (peak at end)
                { 5, 4, 3, 2, 1 }, // Strictly decreasing (peak at start)
                { 1 }, // Single element
                { 1, 2 }, // 2 elements ascending
                { 2, 1 } // 2 elements descending
        };

        for (int i = 0; i < testCases.length; i++) {
            System.out.printf("\n=================== TEST CASE %d ===================", i + 1);
            int peakIndex = solver.findPeakElement(testCases[i]);
            System.out.printf("Result: Peak found at index %d with value %d\n", peakIndex, testCases[i][peakIndex]);
        }
    }
}
