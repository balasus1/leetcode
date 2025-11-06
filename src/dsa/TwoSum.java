package dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    public static void main(String[] args) {
        int[] twoSumProblem = getTwoSum(new int[]{2,7,11,15}, 9);
        System.out.println("TwoSum: " + Arrays.toString(twoSumProblem));
    }

    static int[] getTwoSum(int[] nums, int target){
        Map<Integer, Integer> map = new HashMap<>();
        for(int i=0; i<nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            } else {
                map.put(nums[i], i);
            }
        }
        throw new IllegalArgumentException("No match found");
    }
}
