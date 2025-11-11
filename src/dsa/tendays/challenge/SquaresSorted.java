package dsa.tendays.challenge;

import java.util.Arrays;

/**
 * Problem: 977
 * <a href="https://leetcode.com/problems/squares-of-a-sorted-array/">...</a>
 */

public class SquaresSorted {
    static void main(String[] args) {
        int[] nums = {-7, -3, 2, 3, 11};
        System.out.println(Arrays.toString(nums));
        int[] squaredSortedElements = squaresSorted(nums);
        System.out.println(Arrays.toString(squaredSortedElements));
        int[] getSquareRoot = squareRoot(nums);
        System.out.println(Arrays.toString(getSquareRoot));
    }

    /**
     * Use 2 pointers and place the larger sq. values at the end of the array
     * @param arr
     * @return
     */
    static int[] squaresSorted(int[] arr) {
        int len = arr.length;
        int[] result = new int[len];
        int leftIndex = 0;
        int rightIndex = len-1;
        int pointerPosition = len-1;
        while (leftIndex <= rightIndex) {
            int leftElement = arr[leftIndex] * arr[leftIndex];
            int rightElement = arr[rightIndex] * arr[rightIndex];
            if(leftElement > rightElement) {
                result[pointerPosition] = leftElement;
                leftIndex++;
            } else {
                result[pointerPosition] = rightElement;
                rightIndex--;
            }
            pointerPosition--;
        }
        return result;
    }

    static int[] squareRoot(int[] nums) {
        int[] result = new int[nums.length];
        // First square all the elements in the array
        for (int i = 0; i < nums.length; i++) {
            nums[i] = nums[i] * nums[i];
        }
        int leftIndex = 0;
        int rightIndex = nums.length-1;

        for (int pos = nums.length-1; pos >= 0 ; pos--) {
            if(nums[leftIndex] > nums[rightIndex]) {
                result[pos] = nums[leftIndex];
                leftIndex++;
            } else {
                result[pos] = nums[rightIndex];
                rightIndex--;
            }
        }
        return result;
    }
}
