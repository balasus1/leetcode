package patterns;

/*
    Courtesy: Kunal Kushwaha
    Thank you very much Kunal Kushwaha (https://github.com/kunal-kushwaha)
 */

import java.util.stream.IntStream;

public class Patterns {
    public static void main(String[] args) {
        pattern1(5);
        pattern1a(5);
        pattern1b(5);
        pattern1c(5);
        pattern1d(5);
        pattern1e(5);
        pattern1f(5);
    }

    static void pattern1(int n) {
        System.out.println("pattern-1, for-loop");
        for(int row = 1; row <= n; row++){
            for(int col = 1; col <= row; col++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }

    static void pattern1a(int n) {
        System.out.println("pattern1a");
        IntStream.rangeClosed(1, n)
                .forEach(row -> {
                    IntStream.rangeClosed(1, row)
                            .forEach(col -> System.out.print("* "));
                    System.out.println();
                });
    }

    static void pattern1b(int n) {
        System.out.println("pattern1b");
        IntStream.rangeClosed(1, n)
                .forEach(row -> {
                    IntStream.rangeClosed(1, row)
                            .forEach(System.out::print);
                    System.out.println();
                });
    }

    static void pattern1c(int n) {
        System.out.println("pattern1c");
        IntStream.rangeClosed(1, n)
                .forEach(row -> {
                    IntStream.rangeClosed(1, n)
                            .forEach(col -> System.out.print("* "));
                    System.out.println();
                });
    }

    static void pattern1d(int n) {
        System.out.println("pattern1d");
        for(int row = 1; row <= n; row++) {
            for(int col = 1; col <= n-row+1; col++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }

    static void pattern1e(int n) {
        System.out.println("pattern1e");
        for(int row = 0; row < 2 * n; row++) {
            int totColRow = row > n ? 2 * n - row : row;
            for(int col = 0; col < totColRow; col++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }

    static void pattern1f(int n) {
        System.out.println("pattern1e");
        for(int row = 0; row < 2 * n; row++) {
            int totColRow = row > n ? 2 * n - row : row;
            int noOfSpaces = n - totColRow;
            for(int space = 0; space < noOfSpaces; space++) {
                System.out.print(" ");
            }
            for(int col = 0; col < totColRow; col++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }

}
