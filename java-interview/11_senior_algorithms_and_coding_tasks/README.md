# 11. Senior Coding Tasks & Algorithmic Interview Solutions

Production-grade, fully runnable Java implementations with complete verbal scripts, complexity analysis, and edge case handling.

---

## 📑 Coding Index
1. [Palindrome Number without String Conversion](#1-palindrome-number-without-string-conversion)
2. [Sort a Stack using Another Stack](#2-sort-a-stack-using-another-stack)
3. [Maximum Sum Subarray (Kadane's Algorithm)](#3-maximum-sum-subarray-kadanes-algorithm)
4. [Remove Duplicates from List using Two Pointers](#4-remove-duplicates-from-list-using-two-pointers)
5. [Count All Palindromic Substrings in a String](#5-count-all-palindromic-substrings-in-a-string)
6. [Complete 3-Tier REST API: Controller, Service, Repository with Pagination & Sorting](#6-complete-3-tier-rest-api-controller-service-repository)

---

### 1. Palindrome Number without String Conversion

#### 🎙️ 60-Second Verbal Script
> "To check if an integer is a palindrome without converting it to a String, we mathematically reverse the second half of the number and compare it with the first half.
>
> 1. Negative numbers (e.g. `-121`) and numbers ending in zero (except `0` itself) cannot be palindromes.
> 2. We extract the last digit using modulo `x % 10` and build the reversed half `reversed = reversed * 10 + (x % 10)` while dividing `x /= 10`.
> 3. We stop when `x <= reversed`. For even-length numbers, `x == reversed`; for odd-length numbers, `x == reversed / 10`.
> 4. **Complexity**: Time is $O(\log_{10} N)$ and Auxiliary Space is strictly $O(1)$ without any memory allocation."

```java
package coding;

public class PalindromeNumber {

    public static boolean isPalindrome(int x) {
        // Negative numbers and multiples of 10 (except 0) are not palindromes
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }

        int reversedHalf = 0;
        while (x > reversedHalf) {
            reversedHalf = (reversedHalf * 10) + (x % 10);
            x /= 10;
        }

        // Handles even length (x == reversedHalf) and odd length (x == reversedHalf / 10)
        return x == reversedHalf || x == reversedHalf / 10;
    }

    public static void main(String[] args) {
        System.out.println("121 is palindrome: " + isPalindrome(121));   // true
        System.out.println("-121 is palindrome: " + isPalindrome(-121)); // false
        System.out.println("10 is palindrome: " + isPalindrome(10));     // false
        System.out.println("1221 is palindrome: " + isPalindrome(1221)); // true
    }
}
```

---

### 2. Sort a Stack using Another Stack

#### 🎙️ 60-Second Verbal Script
> "To sort an input stack using only one temporary auxiliary stack:
>
> 1. While the input stack is not empty, pop the top element into a variable `temp`.
> 2. While the auxiliary stack is not empty and its top element is greater than `temp`, pop from the auxiliary stack and push back into the input stack.
> 3. Push `temp` into the auxiliary stack.
> 4. Once the input stack is empty, the auxiliary stack contains the elements in sorted order.
> 5. **Complexity**: Time Complexity is $O(N^2)$ in worst-case (reverse sorted), Space Complexity is $O(N)$ for the auxiliary stack."

```java
package coding;

import java.util.ArrayDeque;
import java.util.Deque;

public class SortStack {

    public static Deque<Integer> sort(Deque<Integer> input) {
        Deque<Integer> auxStack = new ArrayDeque<>();

        while (!input.isEmpty()) {
            int temp = input.pop();

            // Shift elements back to input stack if they are greater than temp
            while (!auxStack.isEmpty() && auxStack.peek() > temp) {
                input.push(auxStack.pop());
            }

            auxStack.push(temp);
        }

        return auxStack;
    }

    public static void main(String[] args) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(34);
        stack.push(3);
        stack.push(31);
        stack.push(98);
        stack.push(92);
        stack.push(23);

        Deque<Integer> sorted = sort(stack);
        System.out.println("Sorted Stack (Smallest at top): " + sorted);
    }
}
```

---

### 3. Maximum Sum Subarray (Kadane's Algorithm)

#### 🎙️ 60-Second Verbal Script
> "**Kadane's Algorithm** finds the contiguous subarray within a one-dimensional array of numbers which has the largest sum.
>
> We maintain two variables in a single pass:
> 1. `currentSum`: At each element, we decide whether to add the current element to the existing subarray (`currentSum + num`) or start a fresh subarray from `num` (`Math.max(num, currentSum + num)`).
> 2. `maxSum`: Tracks the global maximum sum seen so far.
> 3. **Complexity**: Time Complexity is strictly linear **$O(N)$** and Space Complexity is strictly **$O(1)$**."

```java
package coding;

public class KadaneMaxSubarray {

    public static int maxSubArray(int[] nums) {
        if (nums == null || nums.length == 0) return 0;

        int currentSum = nums[0];
        int maxSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            maxSum = Math.max(maxSum, currentSum);
        }

        return maxSum;
    }

    public static void main(String[] args) {
        int[] arr = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        System.out.println("Max Subarray Sum: " + maxSubArray(arr)); // Output: 6 (Subarray: [4, -1, 2, 1])
    }
}
```

---

### 4. Remove Duplicates from List using Two Pointers

#### 🎙️ 60-Second Verbal Script
> "To remove duplicate elements from an array or list:
> 1. If unsorted, we sort it in $O(N \log N)$ time or use a `LinkedHashSet`.
> 2. On a sorted array, we use **Two Pointers (Slow and Fast)**:
>    - `slow` pointer points to the last unique element index.
>    - `fast` pointer scans ahead. Whenever `nums[fast] != nums[slow]`, we increment `slow` and copy `nums[slow] = nums[fast]`.
> 3. This operates **in-place with $O(1)$ auxiliary space** and returns the count of unique elements."

```java
package coding;

import java.util.Arrays;

public class RemoveDuplicatesTwoPointers {

    public static int removeDuplicates(int[] nums) {
        if (nums == null || nums.length == 0) return 0;

        int slow = 0;
        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
        }
        return slow + 1; // Number of unique elements
    }

    public static void main(String[] args) {
        int[] nums = {0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        int uniqueCount = removeDuplicates(nums);

        System.out.println("Unique Count: " + uniqueCount);
        System.out.println("Unique Array: " + Arrays.toString(Arrays.copyOfRange(nums, 0, uniqueCount)));
    }
}
```

---

### 5. Count All Palindromic Substrings in a String

#### 🎙️ 60-Second Verbal Script
> "To count all palindromic substrings in a string of length $N$, we use the **Expand Around Center** approach:
>
> 1. Every palindrome has a center. For a string of length $N$, there are $2N - 1$ potential centers ($N$ single-character centers for odd palindromes, and $N-1$ two-character centers for even palindromes).
> 2. For each center, we expand outward while characters match (`s.charAt(left) == s.charAt(right)`), incrementing the palindrome count.
> 3. **Complexity**: Time Complexity is **$O(N^2)$**, Space Complexity is strictly **$O(1)$** without table overhead."

```java
package coding;

public class CountPalindromicSubstrings {

    public static int countSubstrings(String s) {
        if (s == null || s.isEmpty()) return 0;

        int totalCount = 0;
        for (int i = 0; i < s.length(); i++) {
            totalCount += expandAroundCenter(s, i, i);     // Odd-length palindromes (e.g. "aba")
            totalCount += expandAroundCenter(s, i, i + 1); // Even-length palindromes (e.g. "abba")
        }
        return totalCount;
    }

    private static int expandAroundCenter(String s, int left, int right) {
        int count = 0;
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            count++;
            left--;
            right++;
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println("Count for 'abc': " + countSubstrings("abc")); // Output: 3 ("a", "b", "c")
        System.out.println("Count for 'aaa': " + countSubstrings("aaa")); // Output: 6 ("a", "a", "a", "aa", "aa", "aaa")
    }
}
```

---

### 6. Complete 3-Tier REST API: Controller, Service, Repository

```java
package com.example.product;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

// 1. Domain Entity
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;

    public ProductEntity() {}
    public ProductEntity(String name, String category, BigDecimal price) {
        this.name = name; this.category = category; this.price = price;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
}

// 2. DTO
public record ProductResponseDto(Long id, String name, String category, BigDecimal price) {}

// 3. Repository Layer
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Page<ProductEntity> findByCategoryIgnoreCase(String category, Pageable pageable);
}

// 4. Service Layer
@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponseDto> getProductsByCategory(
            String category, int page, int size, String sortBy, String sortDirection) {
        
        Sort sort = sortDirection.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return productRepository.findByCategoryIgnoreCase(category, pageable)
                .map(p -> new ProductResponseDto(p.getId(), p.getName(), p.getCategory(), p.getPrice()));
    }
}

// 5. Controller Layer (Path params, Query params, Pagination, Sorting)
@RestController
@RequestMapping("/api/v1/categories/{category}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponseDto>> getCategoryProducts(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Page<ProductResponseDto> result = productService.getProductsByCategory(
                category, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(result);
    }
}
```
