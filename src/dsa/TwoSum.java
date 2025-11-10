package dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TwoSum {
    public static void main(String[] args) {
        int[] twoSumProblem = getTwoSum(new int[]{2,7,11,15}, 9);
        System.out.println("TwoSum: " + Arrays.toString(twoSumProblem));
        int[] sortedArrays = new int[]{2,3,5,7,8,9,10,14,24,38};
        int[] result = getTwoSumOnSortedArray(sortedArrays, 7);
        System.out.println("TwoSum 2: " + Arrays.toString(result));
    }

    static int[] getTwoSum(int[] nums, int target){
        Map<Integer, Integer> map = new HashMap<>();
        for(int i=0; i<nums.length; i++) {
            int complement = target - nums[i];
            System.out.println("target" + target + ", nums[" + i + "], complement:" + complement);
            if (map.containsKey(complement)) {
                map.forEach((k,v)-> System.out.println("get()" + k + "-" + v));
                return new int[] { map.get(complement), i };
            } else {
                map.put(nums[i], i);
                map.forEach((k,v)-> System.out.println("put():" + k + "-" + v));
            }
        }
        map.forEach((k,v)-> System.out.println(k + "-" + v));
        throw new IllegalArgumentException("No match found");
    }

    static int[] getTwoSumOnSortedArray(int[] nums, int target){
        // two pointer approach i to move right and j to move left
        int leftIndex = 0, rightIndex = nums.length-1;
        while( leftIndex < rightIndex ){
            int sum = nums[leftIndex] + nums[rightIndex];
            if(sum < target) {
                leftIndex++;
            } else if (sum > target){
                rightIndex--;
            } else {
                return new int[] { leftIndex, rightIndex };
            }
        }
        throw new IllegalArgumentException("No two sum solution found");
    }
}
