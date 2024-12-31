import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PrefixSum {
    public static int[] prefixSum(int[] arr) {
        int[] prefixSum = new int[arr.length];
        // Assuming previous element is 0, set the first element
        // which is current element as it is.
        prefixSum[0] = arr[0];
        for (int i = 1; i < arr.length; i++) {
            // Add previous element with current element
            prefixSum[i] = prefixSum[i - 1] + arr[i];
        }
        return prefixSum;
    }

    // UseCase-1: rangeSum
    public static int rangeSum(int[] arr, int fromIndex, int toIndex) {
        if (fromIndex == 0) {
            return arr[toIndex];
        }
        return arr[toIndex] - arr[fromIndex - 1];
    }

    // UseCase-Given a binary array nums, return the maximum length of a contiguous subarray with an equal number of 0
    // and 1.
    public static int findMaxSubArray(int[] nums) {
            int maxLength = 0;
            // hold key -> sum of 0 and 1
            // value -> index of that sum
            Map<Integer, Integer> map = new HashMap<>(nums.length);
            // Default map value is -1
            map.put(0, -1);
            int sum = 0;
            // Traverse the array
            // If found 0, decrement the sum by -1
            // If found 1, increment the sum by  1
            // If found 0 and 1 in the map, calculate the length of this subarray by substracting the previous index
                // from the current index and update the max length and return the max length.
            // If found 0 and 1 not in the map, add it to the map
            for (int i = 0; i < nums.length; i++) {
                sum += nums[i] == 0 ? -1 : 1;
                if (map.containsKey(sum)) {
                    int previousIndex = map.get(sum);
                    maxLength = Math.max(maxLength, i - previousIndex);
                } else {
                    map.put(sum, i);
                }
            }
            return maxLength;
    }

    /*
        Given an array of integers nums and an integer k, return the total number of subarrays whose sum equals to k.
        A subarray is a contiguous non-empty sequence of elements within an array.
    */
    public static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int sum = 0;
        int count = 0;

        for (int i : nums) {
            sum += i;
            if (map.containsKey(sum - k)) {
                count += map.get(sum - k);
            }
            map.put(sum, map.getOrDefault(sum, 0) + 1);
        }

        return count;
    }

    public static void main(String[] args) {
        int[] nums = { 1, 2, 3, 4, 5, 6, 7 };
        int[] findMaxSubArray = new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0};
        int[] subArraySumArray = new int[]{0, -1, 5, -25, -21,-22};
        int[] prefixSumArray = prefixSum(nums);
        System.out.println("Prefix Array: " + Arrays.toString(prefixSumArray));
        int rangeSum = rangeSum(prefixSumArray, 1, 3);
        System.out.println("Find max subarray: " + rangeSum);
        int maxSubArray = findMaxSubArray(findMaxSubArray);
        System.out.println("Max SubArray: " + maxSubArray);
        int subArraySum = subarraySum(subArraySumArray, 4);
        System.out.println("SubArray Sum: " + subArraySum);
    }

}



