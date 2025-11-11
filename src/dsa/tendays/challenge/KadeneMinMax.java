package dsa.tendays.challenge;

import java.util.Arrays;

public class KadeneMinMax {

    public static class MinMaxResult {
        public int minStart;
        public int minEnd;
        public int minSum;
        public int maxStart;
        public int maxEnd;
        public int maxSum;

        public MinMaxResult(int minStart, int minEnd, int minSum, int maxStart, int maxEnd, int maxSum) {
            this.minStart = minStart;
            this.minEnd = minEnd;
            this.minSum = minSum;
            this.maxStart = maxStart;
            this.maxEnd = maxEnd;
            this.maxSum = maxSum;
        }

        @Override
        public String toString() {
            return "MinMaxResult{" +
                    ", minStart=" + minStart +
                    ", minEnd=" + minEnd +
                    ", minSum=" + minSum +
                    ", maxStart=" + maxStart +
                    ", maxEnd=" + maxEnd +
                    ", maxSum=" + maxSum +
                    '}';
        }
    }

    public static MinMaxResult findMinMaxSubArray(int[] nums) {
        if(nums == null || nums.length == 0) {
            return new MinMaxResult(0,0,-1,-1,-1,-1);
        }
        // For max. sub array
        int maxStart = 0, maxEnd = 0;
        int currentMaxSum = nums[0], maxSum = nums[0];
        int tempMaxStart = 0;

        //For min. sub array
        int minStart = 0, minEnd = 0;
        int minSum = nums[0], currentMinSum = nums[0];
        int tempMinStart = 0;

        // start the array look-up from second element
        for(int i=1; i < nums.length; i++) {
            // max. sub array sum calculation
            if(nums[i] < currentMaxSum + nums[i]) {
                currentMaxSum = currentMaxSum + nums[i];
            } else {
                currentMaxSum = nums[i];
                tempMaxStart = i;
            }

            if(currentMaxSum > maxSum) {
                maxSum = currentMaxSum;
                maxStart = tempMaxStart;
                maxEnd = i;
            }
            // min. sub array sum calculation
            if(nums[i] > currentMinSum + nums[i]) {
                currentMinSum = currentMinSum + nums[i];
            } else {
                currentMinSum = nums[i];
                tempMinStart = i;
            }

            if (currentMinSum <  minSum) {
                minSum = currentMinSum;
                minStart = tempMinStart;
                minEnd = i;
            }
        }
        return new MinMaxResult(minStart, minEnd, minSum, maxStart, maxEnd, maxSum);
    }

    static int maxSumSubArray(int[] nums) {
        int maxSum = nums[0], currentSum = nums[0];
        if(nums == null || nums.length == 0) {
            return 0;
        }
        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }
        return maxSum;
    }

    static void main() {
        int[][] testArrays1 = {
                {1,-3,2,1,-1},
                {-2,1,-3,4,-1,2,1,-5,4},
                {5,4,1,7,8},
                {-5,-2,-1,-8},
                {2,-10,5,-10,20,-10,3}
        };
        int[] testArray2 = {-2,1-3,4,-1,2,1,-5,4};
        for(int[] testArray : testArrays1) {
            MinMaxResult minMaxResult = findMinMaxSubArray(testArray);
            System.out.println(minMaxResult);
        }
        int maxSum = maxSumSubArray(testArray2);
        System.out.println("Max sum subArray: " + maxSum);
    }
}
