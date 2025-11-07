package brocode.dsa;

import java.util.Arrays;

public class BubbleSortExample {
    static void main(String[] args) {
        int[] array = {9,1,8,2,7,3,6,4,5,24,96};
        System.out.println("Original array: " + Arrays.toString(array));
        bubbleSort(array);
        System.out.println("After bubble sort");
        for(int i : array){
            System.out.print(i + " ");
        }
    }
    static void bubbleSort(int[] array){
        for(int i = 0; i < array.length-1; i++){
            for(int j = 0; j < array.length-1; j++){
                // descening order. to make it to ascending start checiking array[j] > array[j+1]
                if(array[j] < array[j+1]){
                    swap(array, j);
                }
            }
        }
    }

    private static void swap(int[] array, int j) {
        int temp = array[j];
        array[j] = array[j + 1];
        array[j + 1] = temp;
    }
}
