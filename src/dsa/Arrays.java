package dsa;

import java.util.stream.IntStream;

public class Arrays {
    public static void main(String[] args) {
        initArray();
    }

    static void printArray(int[] array) {
        IntStream.rangeClosed(0, array.length)
                .forEach(s -> System.out.print(s + " "));
    }

    static void initArray() {
        int[] myArray;
        myArray = new int[5];
        myArray[0] = 3;
        myArray[1] = 7;
        myArray[2] = 9;
        myArray[3] = 11;
        myArray[4] = 13;
        printArray(myArray);
    }
}
