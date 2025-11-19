package dsa.tendays.challenge;

import java.util.Arrays;

public class TwoPointers {
    public static void main(String[] args) {
        int[][] twoSumTestData = new int[][]{{1,2,3,4,6}, {1,3,4,7,10}};
        int[] twoSumSortedTestCase = twoSumSorted(twoSumTestData[0], 6);
        int[] removeDuplicatesData = new int[] {0,0,1,1,2};
        int[] negativePartitionData = new int[] {1,-2,3,-4};
        int[] reverseSubArrayInPlaceData = new int[] {1,2,3,4,6};
        int[] trapRainWaterData = new int[] {4,5,0,3,2,5};
        System.out.println("Two sum sorted: " + Arrays.toString(twoSumSortedTestCase));
        System.out.println("Closest target: " + Arrays.toString(closetSumTarget(twoSumTestData[0], 8)));
        System.out.println("Remove duplicates on sorted array: " +
                Arrays.toString(removeDuplicatesSortedArray(removeDuplicatesData)));
        System.out.println("Negative partitions: " +
                Arrays.toString(partitionNegatives(negativePartitionData)));
        System.out.println("Reverse subArray: " + Arrays.toString(reverseSubArrayInPlace(reverseSubArrayInPlaceData, 1,3)));
        System.out.println("Trap rain water content: " + trapRainWater(trapRainWaterData));
    }

    static int[] twoSumSorted(int[] nums, int target) {
        int leftIndex = 0, rightIndex = nums.length - 1;

        while (leftIndex < rightIndex) {
            int sum = nums[leftIndex] + nums[rightIndex];
            if (sum == target) {
                return new int[] {leftIndex, rightIndex};
            } else if (sum < target) {
                leftIndex++;
            } else {
                rightIndex--;
            }
        }
        return new int[] {-1, -1};
    }

    static int[] closetSumTarget(int[] nums, int target) {
        int leftIndex = 0, rightIndex = nums.length-1;
        int[] result = new int[2];
        int bestDiff = Integer.MAX_VALUE;
        int difference = 0, sum = 0;
        while (leftIndex < rightIndex) {
            sum = nums[leftIndex] + nums[rightIndex];
            difference = Math.abs(sum - target);
            if (difference < bestDiff) {
                result[0] = nums[leftIndex];
                result[1] = nums[rightIndex];
            }
            if (sum < target) {
                leftIndex++;
            } else {
                rightIndex--;
            }
        }
        return result;
    }

    static int[] removeDuplicatesSortedArray(int[] nums) {
        int prevIndex = 0, newIndex = 0;

        for(int currentIndex = 1; currentIndex < nums.length; currentIndex++) {
            if(nums[currentIndex] != nums[prevIndex]) {
                prevIndex++;
                nums[prevIndex] = nums[currentIndex];
                newIndex = prevIndex+1;
            }
        }
        int[] result = new int[newIndex];
        System.arraycopy(nums, 0, result, 0, newIndex);
        return result;
    }

    static int[] partitionNegatives(int[] nums) {
        int i = 0;
        for(int j=0;j<nums.length;j++) {
            if(nums[j] < 0) {
                int tmp = nums[i];
                nums[i] = nums[j];
                nums[j] = tmp;
                i++;
            }
        }
        return nums;
    }

    static int[] reverseSubArrayInPlace(int[] nums, int leftIndex, int rightIndex) {
        while(leftIndex < rightIndex) {
            int temp = nums[leftIndex];
            nums[leftIndex] = nums[rightIndex];
            nums[rightIndex] = temp;
            leftIndex++;
            rightIndex--;
        }
        return nums;
    }

    static int trapRainWater(int[] height) {
        int leftIndex = 0, rightIndex = height.length-1;
        int leftMax = 0, rightMax = 0, waterHoldingCapacity = 0;
        while (leftIndex < rightIndex) {
            if(height[leftIndex] < height[rightIndex]) {
                System.out.println("leftIndex: " + leftIndex + ",Height[left]:" + height[leftIndex] + ",Height[right]:" + height[rightIndex]);
                if(height[leftIndex] >= leftMax) {
                    System.out.println("Height[left]:" + height[leftIndex] + ",leftMax:" + leftMax);
                    leftMax = height[leftIndex];
                    System.out.println("leftMax = " + leftMax);
                } else {
                    System.out.println("from left: waterHoldingCapacity:"
                            + waterHoldingCapacity +", leftMax:" + leftMax +", height[left]:" +  height[leftIndex]);
                    waterHoldingCapacity += leftMax - height[leftIndex];
                    System.out.println("from left: waterHoldingCapacity: " + waterHoldingCapacity);
                }
                leftIndex++;
            } else {
                if(height[rightIndex] >= rightMax) {
                    System.out.println("Height[rightIndex]:" + height[rightIndex] + ",rightMax:" + rightMax);
                    rightMax = height[rightIndex];
                    System.out.println("rightMax = " + rightMax);
                } else {
                    System.out.println("from right: waterHoldingCapacity:"
                            + waterHoldingCapacity +", rightMax:" + rightMax +", height[rightIndex]:" +  height[rightIndex]);
                    waterHoldingCapacity += rightMax - height[rightIndex];
                    System.out.println("from right: waterHoldingCapacity: " + waterHoldingCapacity);
                }
                rightIndex--;
            }
        }
        return waterHoldingCapacity;
    }
}
