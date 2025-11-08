package brocode.dsa;

import java.util.Arrays;

public class QuickSortExample {
    static void main(String[] args) {
        int[] arr = {8,2,5,3,7,6,9,1,4};
        quickSort(arr, 0,arr.length-1);
        System.out.println(Arrays.toString(arr));
    }
    static void quickSort(int[] arr, int leftIndex, int rightIndex) {
        if (rightIndex <= leftIndex) return;
        int pivotIndex = partition(arr, leftIndex, rightIndex);
        quickSort(arr, leftIndex, pivotIndex - 1);
        quickSort(arr, pivotIndex + 1, rightIndex);
    }
    static int partition(int[] arr, int leftIndex, int rightIndex) {
        int pivot = arr[rightIndex];
        int i = leftIndex - 1;
        for (int j = leftIndex; j < rightIndex; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        i++;
        int temp = arr[i];
        arr[i] = arr[rightIndex];
        arr[rightIndex] = temp;
        // location of the pivot
        return i;
    }
}
