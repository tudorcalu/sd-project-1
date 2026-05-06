package org.search.search;

public final class TypoMatcher {
    private TypoMatcher() {
    }

    public static int maxDistanceFor(String token) {
        if (token == null) {
            return 0;
        }
        int length = token.length();
        if (length >= 8) {
            return 2;
        }
        if (length >= 4) {
            return 1;
        }
        return 0;
    }

    public static boolean isTypoMatch(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        int maxDistance = Math.max(maxDistanceFor(left), maxDistanceFor(right));
        if (Math.abs(left.length() - right.length()) > maxDistance) {
            return false;
        }
        return levenshteinDistance(left, right, maxDistance) <= maxDistance;
    }

    static int levenshteinDistance(String left, String right, int cutoff) {
        int leftLen = left.length();
        int rightLen = right.length();
        if (leftLen == 0) {
            return rightLen;
        }
        if (rightLen == 0) {
            return leftLen;
        }

        int[] previous = new int[rightLen + 1];
        int[] current = new int[rightLen + 1];
        for (int j = 0; j <= rightLen; j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= leftLen; i++) {
            current[0] = i;
            int rowMin = current[0];
            char leftChar = left.charAt(i - 1);
            for (int j = 1; j <= rightLen; j++) {
                int substitutionCost = leftChar == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitutionCost
                );
                rowMin = Math.min(rowMin, current[j]);
            }
            if (rowMin > cutoff) {
                return cutoff + 1;
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[rightLen];
    }
}
