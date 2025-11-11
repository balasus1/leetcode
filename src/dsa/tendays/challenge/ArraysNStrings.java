package dsa.tendays.challenge;

import java.util.Arrays;
import java.util.List;

public class ArraysNStrings {
    static void main(String[] args) {
        int[] subArrayTestCase = {1,2,3,4};
        int[][] subMinMaxArrayTestCases = {
                {1,-3,2,1,-1},
                {-2,1,-3,4,-1,2,1,-5,4},
                {5,4,1,7,8},
                {-5,-2,-1,-8}
        };
        int minSubArray = findArrrayIndexOfMinSubArray(subMinMaxArrayTestCases);
        System.out.println("Minimum Sub Array Index: " + minSubArray);
        System.out.println(List.of("subArraySumForLoop:" + subArraySum(subArrayTestCase, 9)));
        System.out.println(List.of("subArraySumWhileLoop:" + subArraySumWhileLoop(subArrayTestCase, 9)));
        System.out.println("Sub Array sum - Kadane's Algorithm:" + findMaxContingencySubArray(subMinMaxArrayTestCases));
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

    // using while loop
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

    // flip the comparison to greater for the max. sum of subarray non-contiguous
    static int findArrrayIndexOfMinSubArray(int[][] subArrays){
        int minIndex = 0;
        int minArray = Arrays.stream(subArrays[minIndex]).sum();
        for(int i = 0; i < subArrays.length; i++){
            int eachArray = Arrays.stream(subArrays[i]).sum();
            System.out.println("Sum of all elements of array at index[" + i + "]:" + eachArray);
            if(eachArray < minArray) {
                minIndex = i;
            }
        }
        return minIndex;
    }

    // Kadane's Algorithm: Fids the max sum of any contiguous subarray
    static int findMaxContingencySubArray(int[][] subArrays){
        int maxIndex = 0;
        int maxSum = Integer.MIN_VALUE;
        for(int i = 0; i < subArrays.length; i++){
            int currentArray = subArrays[i][0];
            int maxArray = subArrays[i][0];
            for(int j = 1; j < subArrays[i].length; j++){
                currentArray = Math.max(subArrays[i][j], currentArray + subArrays[i][j]);
                maxArray = Math.max(maxArray, currentArray);
            }
            if(maxArray > maxSum){
                maxSum = maxArray;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

}
