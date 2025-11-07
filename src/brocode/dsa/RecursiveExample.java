package brocode.dsa;

import java.math.BigInteger;

public class RecursiveExample {
    static void main() {
        System.out.println("|Factorial|" + factorial(4));
        int powerValue = power(2, 4);
        System.out.println(powerValue);
        System.out.println("PowerUsingInteger: "+power1(2, 31));
        BigInteger result = powerUsingBigInteger(BigInteger.valueOf(2), BigInteger.valueOf(100));
        System.out.println("PowerUsingBigInteger for power of " + BigInteger.valueOf(100) + " is " + result);
    }

    static int factorial(int n) {
        if (n < 1) return 1; // base case always a must in recursion, else you will end up with call stack error
        return n * factorial(n -1);
    }

    static int power(int base, int exponent) {
        if (exponent < 1) return 1; // base case
        return base * power(base, exponent-1); // recursive case
    }

    // neat and safe for Integer value limit
    static int power1(int base, int exponent) {
        if (exponent == 0) return 1;
        if (exponent < 0) return 0; // else throw error
        int result = 1;
        while ( exponent > 0 ) {
            result *= base;
            exponent--;
        }
        return result;
    }

    static BigInteger powerUsingBigInteger(BigInteger base, BigInteger exponent) {
        BigInteger result = BigInteger.ONE;
        for(int i = 0; i < 100; i++){
            result = result.multiply(base);
        }
        return result;
    }
}
