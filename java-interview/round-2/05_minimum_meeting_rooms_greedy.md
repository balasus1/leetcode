# 05. Minimum Meeting Rooms Problem using a Greedy Approach (LeetCode 253)

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "The **Minimum Meeting Rooms** problem asks for the minimum number of conference rooms required to schedule a collection of meeting time intervals without overlaps.
>
> We solve this using a **Greedy interval approach with a Min-Heap (Priority Queue)** or **Two Pointers (Chronological Sweep-line)**:
>
> 1. **Greedy Min-Heap Approach**:
>    - Sort the meetings by their **start times** in ascending order.
>    - Maintain a Min-Heap that stores the **end times** of currently ongoing meetings. The top of the heap holds the earliest ending meeting.
>    - For each meeting, we check if the earliest meeting in the heap has ended (i.e. `meeting.start >= minHeap.peek()`). If so, we reuse that room by polling from the heap.
>    - We push the current meeting's end time into the heap.
>    - The final size of the heap is the minimum number of rooms needed.
>
> 2. **Complexity**:
>    - Sorting takes **$O(N \log N)$** time.
>    - Heap operations take **$O(N \log N)$** time and **$O(N)$** space.
>    - Alternatively, separating start and end arrays with Two Pointers gives $O(N \log N)$ time and $O(N)$ space with smaller constant factors."

---

## 🧠 Algorithmic Trace & Dry Run

```
Intervals: [[0, 30], [5, 10], [15, 20]]

1. Sort by start: [[0, 30], [5, 10], [15, 20]]
2. Process [0, 30]:
   - Heap: [30] (Room 1) -> Rooms = 1
3. Process [5, 10]:
   - Earliest end = 30. Since start(5) < 30, overlap! Need new room.
   - Heap: [10, 30] (Room 1, Room 2) -> Rooms = 2
4. Process [15, 20]:
   - Earliest end = 10. Since start(15) >= 10, Room 2 is FREE! Reuse it (poll 10).
   - Push 20 -> Heap: [20, 30] -> Rooms = 2

Result: Heap size = 2 rooms.
```

---

## 💻 Complete Working Java Code: Min-Heap & Two-Pointers

```java
package round2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class MeetingRoomsGreedy {

    public record Interval(int start, int end) {}

    /**
     * Approach 1: Greedy with Min-Heap (PriorityQueue)
     * Time: O(N log N) | Space: O(N)
     */
    public static int minMeetingRoomsHeap(int[][] intervals) {
        if (intervals == null || intervals.length == 0) return 0;

        // 1. Sort intervals by start time
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        // 2. Min-heap of end times
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int[] meeting : intervals) {
            // If the earliest ending meeting has finished before current start, reuse room
            if (!minHeap.isEmpty() && meeting[0] >= minHeap.peek()) {
                minHeap.poll();
            }
            // Allocate / update room with current meeting's end time
            minHeap.offer(meeting[1]);
        }

        return minHeap.size();
    }

    /**
     * Approach 2: Two-Pointer Chronological Sweep-line
     * Time: O(N log N) | Space: O(N) (Extremely cache-friendly)
     */
    public static int minMeetingRoomsTwoPointers(int[][] intervals) {
        if (intervals == null || intervals.length == 0) return 0;

        int n = intervals.length;
        int[] starts = new int[n];
        int[] ends = new int[n];

        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }

        Arrays.sort(starts);
        Arrays.sort(ends);

        int rooms = 0;
        int endPtr = 0;

        for (int startPtr = 0; startPtr < n; startPtr++) {
            if (starts[startPtr] < ends[endPtr]) {
                // Meeting started before earliest meeting ended -> need new room
                rooms++;
            } else {
                // Room vacated -> advance end pointer
                endPtr++;
            }
        }

        return rooms;
    }

    public static void main(String[] args) {
        int[][] test1 = {{0, 30}, {5, 10}, {15, 20}};
        int[][] test2 = {{7, 10}, {2, 4}};
        int[][] test3 = {{1, 5}, {8, 9}, {8, 9}};

        System.out.println("Test 1 (Heap): " + minMeetingRoomsHeap(test1)); // Expected: 2
        System.out.println("Test 1 (Two Pointers): " + minMeetingRoomsTwoPointers(test1)); // Expected: 2

        System.out.println("Test 2 (Heap): " + minMeetingRoomsHeap(test2)); // Expected: 1
        System.out.println("Test 3 (Heap): " + minMeetingRoomsHeap(test3)); // Expected: 2
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What if a meeting ends at 10 and another starts at 10 (e.g. `[5, 10]` and `[10, 15]`)?"
**Answer:**
> "Because the condition is `meeting.start >= minHeap.peek()`, the room is considered vacated at time 10 and can be immediately reused without requiring an additional room."

### 2. "How would you return the actual schedule/assignment of rooms to meetings?"
**Answer:**
> "Instead of storing just the end time in the heap, store a `Room` object containing `(roomId, availableAt)`. When polling/reusing a room, record `(meetingId -> room.id)` in an output mapping."
