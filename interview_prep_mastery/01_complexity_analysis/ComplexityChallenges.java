package interview_prep_mastery._01_complexity_analysis;

/**
 * ============================================================================
 * MODULE 01: COMPLEXITY ANALYSIS & BIG-O CHALLENGES
 * ============================================================================
 * Comprehensive, executable implementations of Big-O loop patterns,
 * operation step-counters, mathematical derivations, probing questions,
 * assumptions, and automated verification testcases.
 */
public class ComplexityChallenges {

    // ========================================================================
    // Challenge 1: Nested Loop with Addition -> O(n^2)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - What if n <= 0? (Assumption: Loop does not execute, 0 iterations)
     * - Does inner loop bound depend on i? (No, independent: runs n times for every i)
     *
     * Complexity:
     * - Time: O(n^2) -> Sum(i=0 to n-1 of n) = n * n = n^2 operations
     * - Space: O(1) auxiliary
     */
    public static long challenge1_NestedAddition(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 2: Nested Loop with Subtraction (Dependent) -> O(n^2)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - Notice j starts at i: How does this change the sum?
     *   When i=0, j runs n times. When i=n-1, j runs 1 time.
     *
     * Complexity:
     * - Time: O(n^2) -> n + (n-1) + ... + 1 = n(n+1)/2 = n^2/2 + n/2 = O(n^2)
     * - Space: O(1) auxiliary
     */
    public static long challenge2_NestedSubtraction(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 3: Nested Loop with Multiplication -> O(n log n)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - j multiplies by 2: How many times can you double j before exceeding n?
     *   2^k <= n ==> k = floor(log2(n)) + 1 iterations per outer loop.
     *
     * Complexity:
     * - Time: O(n log n) -> n * (log2(n) + 1)
     * - Space: O(1) auxiliary
     */
    public static long challenge3_NestedMultiplication(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j *= 2) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 4: Nested Loop with Multiplication (Basic) -> O(log^2 n)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - Both loops double each step:
     *   Outer: log2(n) steps, Inner: log2(n) steps
     *
     * Complexity:
     * - Time: O(log^2 n) = O((log n)^2)
     * - Space: O(1) auxiliary
     */
    public static long challenge4_MultiplicationBasic(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = 1; i <= n; i *= 2) {
            for (int j = 1; j <= n; j *= 2) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 5: Nested Loop with Multiplication (Intermediate) -> O(n)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - Outer loop doubles (i = 1, 2, 4, 8, ... 2^k <= n), inner loop runs i times.
     * - Is this O(n log n)?
     *   NO! Sum = 1 + 2 + 4 + 8 + ... + 2^k = 2^(k+1) - 1 < 2n = O(n).
     *
     * Complexity:
     * - Time: O(n) (Geometric series convergence!)
     * - Space: O(1) auxiliary
     */
    public static long challenge5_MultiplicationIntermediate(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = 1; i <= n; i *= 2) {
            for (int j = 1; j <= i; j++) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 6: Nested Loop with Multiplication (Advanced) -> O(n)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - Outer loop divides by 2 (i = n, n/2, n/4, ... 1), inner loop runs i times.
     * - Sum = n + n/2 + n/4 + ... + 1 = n * (1 + 1/2 + 1/4 + ...) <= 2n
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) auxiliary
     */
    public static long challenge6_MultiplicationAdvanced(int n) {
        if (n <= 0) return 0;
        long stepCount = 0;
        for (int i = n; i > 0; i /= 2) {
            for (int j = 0; j < i; j++) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Challenge 7: Nested Loop with Multiplication (Pro) -> O(n log log n)
    // ========================================================================
    /**
     * Probing Questions / Assumptions:
     * - Outer loop squares i: i = 2, 4, 16, 256, ..., 2^(2^k) <= n
     *   Number of outer iterations = log2(log2(n)) + 1
     * - Inner loop runs n times.
     *
     * Complexity:
     * - Time: O(n log log n)
     * - Space: O(1) auxiliary
     */
    public static long challenge7_MultiplicationPro(int n) {
        if (n <= 1) return 0;
        long stepCount = 0;
        for (long i = 2; i <= n; i = i * i) {
            for (int j = 1; j <= n; j++) {
                stepCount++;
            }
        }
        return stepCount;
    }

    // ========================================================================
    // Comprehensive Test Suite & Runner
    // ========================================================================
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING COMPLEXITY ANALYSIS TEST SUITE ");
        System.out.println("=================================================");

        int n = 16;

        // Challenge 1 Check
        long c1 = challenge1_NestedAddition(n);
        System.out.printf("[Challenge 1] n=%d -> Steps: %d (Expected: %d)%n", n, c1, n * n);
        assert c1 == n * n : "Challenge 1 failed";

        // Challenge 2 Check
        long c2 = challenge2_NestedSubtraction(n);
        long c2Expected = (long) n * (n + 1) / 2;
        System.out.printf("[Challenge 2] n=%d -> Steps: %d (Expected: %d)%n", n, c2, c2Expected);
        assert c2 == c2Expected : "Challenge 2 failed";

        // Challenge 3 Check
        long c3 = challenge3_NestedMultiplication(n);
        System.out.printf("[Challenge 3] n=%d -> Steps: %d (O(n log n))%n", n, c3);
        assert c3 == 16 * 5 : "Challenge 3 failed for n=16 (j=1,2,4,8,16 -> 5 inner steps * 16)";

        // Challenge 4 Check
        long c4 = challenge4_MultiplicationBasic(n);
        System.out.printf("[Challenge 4] n=%d -> Steps: %d (O(log^2 n))%n", n, c4);
        assert c4 == 5 * 5 : "Challenge 4 failed";

        // Challenge 5 Check
        long c5 = challenge5_MultiplicationIntermediate(n);
        System.out.printf("[Challenge 5] n=%d -> Steps: %d (O(n) Geometric Sum = 1+2+4+8+16=31)%n", n, c5);
        assert c5 == 31 : "Challenge 5 failed";

        // Challenge 6 Check
        long c6 = challenge6_MultiplicationAdvanced(n);
        System.out.printf("[Challenge 6] n=%d -> Steps: %d (O(n) Inverse Sum = 16+8+4+2+1=31)%n", n, c6);
        assert c6 == 31 : "Challenge 6 failed";

        // Challenge 7 Check
        long c7 = challenge7_MultiplicationPro(256);
        System.out.printf("[Challenge 7] n=256 -> Steps: %d (i=2,4,16,256 -> 4 * 256 = 1024)%n", c7);
        assert c7 == 4 * 256 : "Challenge 7 failed";

        System.out.println("\n ALL 7 COMPLEXITY CHALLENGES PASSED VERIFICATION!");
        System.out.println("=================================================");
    }
}
