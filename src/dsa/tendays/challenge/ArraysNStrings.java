package dsa.tendays.challenge;

import java.util.Arrays;
import java.util.List;

public class ArraysNStrings {
    static void main(String[] args) {
        int[] subArrayTestCase = {1,2,3,4};
        System.out.println(List.of("subArraySum:" + subArraySum(subArrayTestCase, 9)));
        System.out.println(List.of("subArraySumWhileLoop:" + subArraySumWhileLoop(subArrayTestCase, 9)));
    }

    /*
        Approach: Sliding Window
        Explanation: Expand right, shrink left when sum exceeds target
        Time Complexity: O(n)
        Space Complexity: O(1)
        Test Cases:

        [1,2,3,4,5], 9 → [1,3]

        [1,4,20,3,10,5], 33 → [2,4]

        [1,2,3], 5 → [1,2]

        [1,2,3], 7 → [-1,-1]
        Estimated Time: 10-15 minutes
     */
    static List<Integer> subArraySum(int[] nums, int target) {
        int leftIndex = 0, sum = 0;
        for (int rightIndex = 0; rightIndex < nums.length; rightIndex++) {
            sum += nums[rightIndex];
            while (sum > target && leftIndex <= rightIndex) {
                sum -= nums[leftIndex++];
            }
            if (sum == target) {
                return Arrays.asList(leftIndex, rightIndex);
            }
        }
        return Arrays.asList(-1, -1);
    }

    static List<Integer> subArraySumWhileLoop(int[] nums, int target) {
        int leftIndex = 0, rightIndex = 0, sum = 0;
        while (rightIndex < nums.length) {
            sum += nums[rightIndex];
            while ((sum > target) && (leftIndex <= rightIndex)) {
                sum -= nums[leftIndex];
                leftIndex++;
            }
            if (sum == target) {
                return Arrays.asList(leftIndex, rightIndex);
            }
            rightIndex++;
        }
        return Arrays.asList(-1, -1);
    }

}
