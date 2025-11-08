package brocode.dsa;

import java.util.Arrays;

public class MergeSortExample {
    static void main(String[] args) {
        int[] arr = {8,2,5,3,7,6,9,1,4};
        merge(arr);
        System.out.println(Arrays.toString(arr));
    }

    static void merge(int[] arr) {
        int len = arr.length;
        if (len <= 1) return; // base case
        // find middle element
        int mid = len / 2;
        // create two sub-arrays and copy elements from original array into left and right array.
        int[] leftArray = new int[mid];
        int[] rightArray = new int[len - leftArray.length];
        // create leftArrayIndex and rightArrayIndex
        int leftArrayIndex = 0;
        int rightArrayIndex = 0;
        for(; leftArrayIndex < len; leftArrayIndex++){
            if(leftArrayIndex < mid){
                leftArray[leftArrayIndex] = arr[leftArrayIndex];
            } else {
                rightArray[rightArrayIndex] = arr[leftArrayIndex];
                rightArrayIndex++;
            }
        }
        // now recursively call the mergeSort method
        merge(leftArray);
        merge(rightArray);
        // merge them back to the original array after sorting
        merge(leftArray, rightArray, arr);
    }

    private static void merge(int[] leftArray, int[] rightArray, int[] arr) {
        int leftSize = arr.length / 2;
        int rightSize = arr.length - leftSize;
        int i = 0, leftIndex = 0, rightIndex = 0;
        while (leftIndex < leftSize && rightIndex < rightSize) {
            if (leftArray[leftIndex] <= rightArray[rightIndex]) {
                arr[i] = leftArray[leftIndex];
                i++;
                leftIndex++;
            } else {
                arr[i] = rightArray[rightIndex];
                i++;
                rightIndex++;
            }
        }
        while (leftIndex < leftSize) {
            arr[i] = leftArray[leftIndex];
            i++;
            leftIndex++;
        }
        while (rightIndex < rightSize) {
            arr[i] = rightArray[rightIndex];
            i++;
            rightIndex++;
        }
    }
}
