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
        pattern1g(5);
        pattern1h(5);
        pattern1i(4);
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

    static void pattern1g(int n) {
        System.out.println("pattern1g");
        // outer loop
        for(int row = 1; row <= n; row++){
            for(int space = 0; space < n-row; space++ ){
                System.out.print("  ");
            }
            for (int col = row; col >= 1; col--) {
                System.out.print(col + " ");
            }
            for (int col = 2; col <= row; col++) {
                System.out.print(col + " ");
            }
            System.out.println();
        }
    }
    static void pattern1h(int n) {
        System.out.println("pattern1h");
        for (int row = 1; row <= 2 * n; row++) {
            int totColRow = row > n ? 2 * n - row : row;
            for(int space = 0; space < n - totColRow; space++) {
                System.out.print("  ");
            }
            for(int col = totColRow; col >= 1; col--) {
                System.out.print(col + " ");
            }
            for(int col = 2; col <= totColRow; col++) {
                System.out.print(col + " ");
            }
            System.out.println();
        }
    }

    static void pattern1i(int n) {
        int originalN = n;
        n = 2 * n;
        for (int row = 0; row <= n; row++) {
            for (int col = 0; col <=n; col++) {
                int atEveryIndex = originalN - Math.min(Math.min(row, col), Math.min(n-row, n-col));
                System.out.print(atEveryIndex + " ");
            }
            System.out.println();
        }
    }

}
