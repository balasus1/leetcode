package dsa;

import java.util.*;
import java.util.Arrays;

public class TwoSum {
     static void main() {
        int[] twoSumProblem = getTwoSum(new int[]{2,7,11,8,1,15}, 9);
        System.out.println("TwoSum: " + Arrays.toString(twoSumProblem));
        System.out.println("TwoSum unique set: " + getTwoSumUniquePairs(new int[]{2,7,11,8,1,5,4,15}, 9));
        int[] sortedArrays = new int[]{2,3,5,7,8,9,10,14,24,38};
        int[] result = getTwoSumOnSortedArray(sortedArrays, 7);
        System.out.println("TwoSum 2: " + Arrays.toString(result));
        int[] zeroSumTestCaseArray = {1,2,2,2,2,-2,-2,-2,-2,-1,1};
        System.out.println("Zero Sum triplets: " + getZeroSumTriplets(zeroSumTestCaseArray));

    }

    // simple scenario
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

    //Two Sum (All Unique Pairs)
    static List<List<Integer>> getTwoSumUniquePairs(int[] nums, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Map<Integer, Integer> map = new HashMap<>();
        for(int eachNum : nums){
            int complement = target - eachNum;
            if(map.containsKey(complement) && map.get(complement) > 0){
                result.add(Arrays.asList(complement, eachNum));
                map.put(complement, map.get(complement) - 1);
            } else {
                map.put(eachNum, map.getOrDefault(eachNum, 0) + 1);
            }
        }
        return  result;
    }

    static List<List<Integer>> getZeroSumTriplets(int[] nums){
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);
        System.out.println(Arrays.toString(nums));

        for(int i=0; i<nums.length-1; i++){
            // skip duplicate elements in the nums array
            if((i > 0) && nums[i] == nums[i-1]) {
                continue;
            }
            int leftIndex=i+1, rightIndex = nums.length-1;
            while(leftIndex < rightIndex){
                int sum = nums[i] + nums[leftIndex] + nums[rightIndex];

                if(sum == 0){
                    result.add(Arrays.asList(nums[i], nums[leftIndex], nums[rightIndex]));
                    // skip already visited triplets
                    while (leftIndex < rightIndex && nums[leftIndex] == nums[leftIndex+1]) { leftIndex++; }
                    while (leftIndex < rightIndex && nums[rightIndex] == nums[rightIndex-1]) { rightIndex--; }
                    leftIndex++; rightIndex--;
                } else if(sum < 0) {
                    leftIndex++;
                } else {
                    rightIndex--;
                }
            }
        }
        return result;
    }
}
