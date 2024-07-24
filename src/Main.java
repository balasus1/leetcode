import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("===1.Prefix Sum===");
        prefixSum();
    }

    /**
     * Generates the prefix sum array for the given array of numbers.
     * Finds the sum of a range of elements in the prefix sum array.
     * Finds the maximum subarray sum in the given array.
     * Finds the sum of a subarray in the given array.
     */

    public static void prefixSum() {
        int[] nums = {1, 2, 3, 4, 5, 6};
        int[] findMaxSubArray = new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0};
        int[] subArraySumArray = new int[]{0, -1, 5, -25, -21,-22};
        int[] prefixSumArray = PrefixSum.prefixSum(nums);
        System.out.println("Prefix Array: " + Arrays.toString(prefixSumArray));
        int rangeSum = PrefixSum.rangeSum(prefixSumArray, 1, 3);
        System.out.println("Find max subarray: " + rangeSum);
        int maxSubArray = PrefixSum.findMaxSubArray(findMaxSubArray);
        System.out.println("Max SubArray: " + maxSubArray);
        int subArraySum = PrefixSum.subarraySum(subArraySumArray, 4);
        System.out.println("SubArray Sum: " + subArraySum);
    }
}