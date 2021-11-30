package com.microservice.authentication.service;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomLogoutSuccessHandlerTest {
    @Mock
    RedisTokenStoreService redisTokenStoreService;

    @Test
    void testOnLogoutSuccess() throws IOException, ServletException {
        CustomLogoutSuccessHandler handler = new CustomLogoutSuccessHandler(redisTokenStoreService);

        handler.onLogoutSuccess(new MockHttpServletRequest(), new MockHttpServletResponse(), new UsernamePasswordAuthenticationToken("", ""));

        verify(redisTokenStoreService).removeAllTokensByAuthenticationUser(any(Authentication.class));
    }

    @Disabled
    @Test
    void testArray() {
        int[] nums = {1, 2, 3, 4, 5, 6, 7};
        rotate(nums, 3);

        assertThat(nums).containsExactly(5, 6, 7, 1, 2, 3, 4);

        nums = new int[] {-1, -100, 3, 99};
        rotate(nums, 2);
        assertThat(nums).containsExactly(3, 99, -1, -100);

        nums = new int[] {1};
        rotate(nums, 1);
        assertThat(nums).containsExactly(1);

        nums = new int[] {1, 2};
        rotate(nums, 2);
        assertThat(nums).containsExactly(1, 2);

        nums = new int[] {1, 2};
        rotate(nums, 0);
        assertThat(nums).containsExactly(1, 2);

        nums = new int[] {1, 2};
        rotate(nums, 3);
        assertThat(nums).containsExactly(2, 1);

        nums = new int[] {1, 2, 3};
        rotate(nums, 1);
        assertThat(nums).containsExactly(3, 1, 2);

        nums = new int[] {1, 2, 3};
        rotate(nums, 4);
        assertThat(nums).containsExactly(3, 1, 2);

        nums = new int[] {1, 2, 3};
        rotate(nums, 3);
        assertThat(nums).containsExactly(1, 2, 3);

        nums = new int[] {1, 2, 3};
        rotate(nums, 100);
        assertThat(nums).containsExactly(3, 1, 2);

        nums = new int[] {1, 2, 3, 4, 5, 6};
        rotate(nums, 1);
        assertThat(nums).containsExactly(6, 1, 2, 3, 4, 5);

        nums = new int[] {1, 2, 3, 4, 5, 6};
        rotate(nums, 11);
        assertThat(nums).containsExactly(2, 3, 4, 5, 6, 1);
    }

    @Disabled
    @Test
    void testAddTwoNumbers() {
        /*ListNode listNode = addTwoNumbers(new ListNode(2, new ListNode(4, new ListNode(3))), new ListNode(5, new ListNode(6, new ListNode(4))));

        assertThat(listNode.)

        int reverse = reverse(123);
        assertThat(reverse).isEqualTo(321);

        reverse = reverse(1534236469);
        assertThat(reverse).isEqualTo(9646324351L);*/

        List<String> strings = fullJustify(new String[] {"This", "is", "an", "example", "of", "text", "justification."}, 16);
        assertThat(strings).containsExactly("This    is    an",
            "example  of text",
            "justification.  "
        );

        strings = fullJustify(new String[] {"What","must","be","acknowledgment","shall","be"}, 16);
        assertThat(strings).containsExactly("What   must   be",
            "acknowledgment  ",
            "shall be        "
        );

        int countDigitOne = countDigitOne(13);
        assertThat(countDigitOne).isEqualTo(6);

        int missingPositive = firstMissingPositive(new int[] {3, 4, -1, 1});
        assertThat(missingPositive).isEqualTo(2);

        missingPositive = firstMissingPositive(new int[] {1, 2, 0});
        assertThat(missingPositive).isEqualTo(3);

        ListNode reverseKGroup = reverseKGroup(new ListNode(1, new ListNode(2, new ListNode(3, new ListNode(4, new ListNode(5))))), 2);
        assertThat(reverseKGroup.val).isEqualTo(2);

        List<Integer> list = findSubstring("barfoofoobarthefoobarman", new String[] {"bar", "foo", "the"});
        assertThat(list).containsExactlyInAnyOrder(6, 9, 12);

        ListNode listNode = mergeKLists(new ListNode[] {new ListNode(1, new ListNode(4, new ListNode(5))), new ListNode(1, new ListNode(3, new ListNode(4))), new ListNode(2, new ListNode(6))});
        assertThat(listNode).isNotNull();

        double median = findMedianSortedArrays(new int[] {1, 3}, new int[] {2});
        assertThat(median).isEqualTo(2);

        median = findMedianSortedArrays(new int[] {1, 2}, new int[] {3, 4});
        assertThat(median).isEqualTo(2.5);

        median = findMedianSortedArrays(new int[] {0, 0}, new int[] {0, 0});
        assertThat(median).isZero();

        median = findMedianSortedArrays(new int[] {}, new int[] {1});
        assertThat(median).isOne();

        median = findMedianSortedArrays(new int[] {2}, new int[] {});
        assertThat(median).isEqualTo(2);

        median = findMedianSortedArrays(new int[] {}, new int[] {2, 3});
        assertThat(median).isEqualTo(2.5);

        median = findMedianSortedArrays(new int[] {3}, new int[] {-2, -1});
        assertThat(median).isEqualTo(-1);
    }

    public List<String> fullJustify(String[] words, int maxWidth) {
        List<String> list = new ArrayList();
        StringBuffer sb = new StringBuffer();
        char space = ' ';
        int size = words.length;
        int countWords = 0;
        for (int i = 0; i < size; i++) {
            String word = words[i];
            if (sb.length() + word.length() > maxWidth) {
                sb.deleteCharAt(sb.length() - 1);
                String justify = sb.toString();
                int missingSpaces = (maxWidth - justify.length()) - countWords;
                char[] arrayEmptySpacesLast = new char[missingSpaces - 1];
                Arrays.fill(arrayEmptySpacesLast, space);
                int rest = missingSpaces % 2;
                int countIndex = 0;
                int index = justify.indexOf(space);
                while (justify.length() < maxWidth && index != -1) {
                    /*if (rest > 0 && countIndex++ + 1 == arrayEmptySpacesLast.length) {
                        arrayEmptySpacesLast = new char[rest];
                        Arrays.fill(arrayEmptySpacesLast, space);
                        sb.insert(index, new String(arrayEmptySpacesLast));
                        break;
                    } else {*/
                        sb.insert(index, arrayEmptySpacesLast);
                    //}
                    index = justify.indexOf(space, index + arrayEmptySpacesLast.length);
                }
                list.add(sb.toString());
                sb = new StringBuffer();
                countWords = 0;
            }
            sb.append(word);
            sb.append(space);
            countWords++;
        }
        if (!sb.toString().isEmpty()) {
            int missingSpaces = maxWidth - (maxWidth - sb.length());
            char[] arrayEmptySpacesLast = new char[missingSpaces - 1];
            list.add(sb.append(arrayEmptySpacesLast).toString());
        }
        return list;
    }

    public int countDigitOne(int n) {
        int count = 0;
        for (int i = 0; i <= n; i++) {
            if (i == 1) {
                count++;
            }
            if (i > 10) {
                String strNumber = String.valueOf(i);
                for (int j = 0; j < strNumber.length(); j++) {
                    if (strNumber.charAt(j) == '1') {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int firstMissingPositive(int[] nums) {
        int n = nums.length;
        for (int i = 0; i < n; i++) {
            int current = nums[i];
            if (current <= 0) {
                continue;
            }
            // when the ith element is not i
            while (current != i) {
                // no need to swap when ith element is out of range [0,n]
                if (current >= n)
                    break;

                //handle duplicate elements
                if(nums[i]==nums[current])
                    break;
                // swap elements
                int temp = current;
                nums[i] = nums[temp];
                nums[temp] = temp;
            }
        }

        for (int i = 0; i < n; i++) {
            if (nums[i] != i)
                return i;
        }

        return n;
    }

    public ListNode reverseKGroup(ListNode head, int k) {
        if (head == null || head.next == null || k <= 1) {
            return head;
        }

        ListNode dummy = new ListNode(-1);
        ListNode previous = dummy;
        previous.next = head;
        ListNode current = previous.next;
        int count = 0;
        while (current != null) {
            ++count;
            if (count == k) {
                break;
            }
            current = current.next;
        }
        if (count < k) {
            return head;
        }

        ListNode nextPrevious = previous.next;
        ListNode previous2 = null;
        ListNode next;
        for (int i = 0; i < k; i++) {
            next = current.next;
            current.next = previous2;
            previous2 = current;
            current = next;
        }
        previous.next = previous2;
        nextPrevious.next = current;
        previous = nextPrevious;
        return dummy.next;
    }

    public void rotate(int[] nums, int k) {
        if (nums.length == 1 || k <= 0 || k == nums.length) {
            return;
        }

        for (int i = 0; i < k; i++) {

        }

        int[] tempArray = new int[nums.length];
        for (int i = 0, j = Math.max(nums.length, k); i < j; i++) {
            if (i == nums.length) {
                i = 0;
            }
        }

        for (int i = 0; i < tempArray.length; i++) {
            nums[i] = tempArray[i];
        }
    }

    public List<Integer> findSubstring(String s, String[] words) {
        // Resultant list
        List<Integer> indices = new ArrayList<>();
        // Base conditions
        if (s == null || s.isEmpty() || words == null || words.length == 0) {
            return indices;
        }
        // Store the words and their counts in a hash map
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        // Length of each word in the words array`
        int wordLength = words[0].length();
        // Length of all the words combined in the array
        int wordArrayLength = wordLength * words.length;
        // Loop for the entire string
        for (int i = 0; i <= s.length() - wordArrayLength; i++) {
            // Get the substring of length equal to wordArrayLength
            String current = s.substring(i, i + wordArrayLength);
            // Map to store each word of the substring
            Map<String, Integer> wordMap = new HashMap<>();
            // Index to loop through the words array
            int index = 0;
            // Index to get each word in the current
            int j = 0;
            // Loop through each word of the words array
            while (index < words.length) {
                // Divide the current string into strings of length of
                // each word in the array
                String part = current.substring(j, j + wordLength);
                // Put this string into the wordMap
                wordMap.put(part, wordMap.getOrDefault(part, 0) + 1);
                // Update j and index
                j += wordLength;
                index++;
            }
            // At this point compare the maps
            if (wordCount.equals(wordMap)) {
                indices.add(i);
            }
        }
        return indices;
        /*String concatenation = sbConcat.toString();
        int index = s.indexOf(concatenation);
        while (index != -1) {
            result.add(index);
            index = s.indexOf(concatenation, index + 1);

        }
        String reverse = sbReverse.toString();
        index = s.indexOf(reverse);
        while (index != -1) {
            result.add(index);
            index = s.indexOf(reverse, index + 1);
        }
        return result;*/
    }

    public ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        for (ListNode head : lists) {
            while (head != null) {
                minHeap.add(head.val);
                head = head.next;
            }
        }

        ListNode result = new ListNode();
        ListNode head = result;
        while (!minHeap.isEmpty()) {
            head.next = new ListNode(minHeap.remove());
            head = head.next;
        }
        return result;
    }

    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        int mergedArray[] = new int[nums1.length + nums2.length];
        for (int i = 0, j = 0, x = -1; x < mergedArray.length - 1; i++, j++) {
            while (i < nums1.length) {
                mergedArray[++x] = nums1[i];
                if (i + 1 < nums1.length && nums1[i+1] == nums1[i] + 1) {
                    i++;
                } else {
                    break;
                }
            }
            while (j < nums2.length) {
                mergedArray[++x] = nums2[j];
                if (j + 1 < nums2.length && nums2[j+1] == nums2[j] + 1) {
                    j++;
                } else {
                    break;
                }
            }
        }
        if (mergedArray.length == 1) {
            return mergedArray[mergedArray.length - 1];
        }
        int middle = mergedArray.length / 2;
        return (mergedArray.length == 2 || middle % 2 == 0 ? (double)(mergedArray[middle - 1] + mergedArray[middle]) / 2 : mergedArray[middle]);
    }

    public int reverse(int x) {
        String reverse = new StringBuffer().append(x).reverse().toString();
        if (x < 0) {
            reverse = "-" + reverse.substring(0, reverse.length() - 1);
        }
        long l = Long.parseLong(reverse);
        return l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE ? (int) l : 0;
    }

    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode result = new ListNode();
        ListNode resultIterator = result;
        int carry = 0;
        while (l1 != null || l2 != null) {
            int l1Val = (l1 != null ? l1.val : 0);
            int l2Val = (l2 != null ? l2.val : 0);

            int currentSum = l1Val + l2Val + carry;
            carry = currentSum / 10;
            int lastDigit = currentSum % 10;

            resultIterator.next = new ListNode(lastDigit);
            l1 = l1.next;
            l2 = l2.next;
            resultIterator = resultIterator.next;
        }

        if (carry > 0) {
            ListNode newNode = new ListNode(carry);
            resultIterator.next = newNode;
        }
        return result.next;
    }

    public class ListNode {
      int val;
      ListNode next;
      ListNode() {}
      ListNode(int val) { this.val = val; }
      ListNode(int val, ListNode next) { this.val = val; this.next = next; }
  }
}
