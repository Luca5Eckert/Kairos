package com.kairos.infrastructure.semantic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkerExtractorAdapterTest {

    private ChunkerExtractorAdapter chunker;

    @BeforeEach
    void setUp() {
        chunker = new ChunkerExtractorAdapter();
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void extract_returnsEmpty_whenContentIsBlankOrNull(String content) {
        assertThat(chunker.extract(content, 10, 2)).isEmpty();
    }


    @Test
    void extract_throwsIllegalArgument_whenChunkSizeIsZero() {
        assertThatThrownBy(() -> chunker.extract("hello", 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");
    }

    @Test
    void extract_throwsIllegalArgument_whenChunkSizeIsNegative() {
        assertThatThrownBy(() -> chunker.extract("hello", -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");
    }

    @Test
    void extract_throwsIllegalArgument_whenOverlapIsNegative() {
        assertThatThrownBy(() -> chunker.extract("hello", 5, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void extract_throwsIllegalArgument_whenOverlapEqualsChunkSize() {
        assertThatThrownBy(() -> chunker.extract("hello world", 5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void extract_throwsIllegalArgument_whenOverlapExceedsChunkSize() {
        assertThatThrownBy(() -> chunker.extract("hello world", 5, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }


    @Test
    void extract_returnsSingleChunk_whenContentFitsExactlyInOneChunk() {
        List<String> result = chunker.extract("hello", 5, 0);

        assertThat(result).containsExactly("hello");
    }

    @Test
    void extract_returnsSingleChunk_whenContentShorterThanChunkSize() {
        List<String> result = chunker.extract("hi", 10, 0);

        assertThat(result).containsExactly("hi");
    }

    @Test
    void extract_returnsMultipleChunks_withNoOverlap() {
        List<String> result = chunker.extract("abcdefghijklmno", 5, 0);

        assertThat(result).containsExactly("abcde", "fghij", "klmno");
    }

    @Test
    void extract_returnsLastChunkSmallerThanChunkSize_whenContentNotDivisible() {
        List<String> result = chunker.extract("abcdefghijkl", 5, 0);

        assertThat(result).containsExactly("abcde", "fghij", "kl");
    }


    @Test
    void extract_appliesOverlap_betweenConsecutiveChunks() {
        List<String> result = chunker.extract("abcdefghijk", 5, 2);

        assertThat(result).containsExactly("abcde", "defgh", "ghijk");
    }

    @Test
    void extract_overlappingChunksShareExpectedCharacters() {
        List<String> result = chunker.extract("abcdefghij", 4, 1);

        assertThat(result).hasSize(3);

        assertThat(result.get(0)).endsWith(result.get(1).substring(0, 1));
        assertThat(result.get(1)).endsWith(result.get(2).substring(0, 1));
    }

    @Test
    void extract_returnsLastChunkContainingRemainder_withOverlap() {
        // chunkSize=5, overlap=2, step=3 → "abcde", "defgh", "ghij" (shorter last)
        List<String> result = chunker.extract("abcdefghij", 5, 2);

        assertThat(result).containsExactly("abcde", "defgh", "ghij");
    }


    @Test
    void extract_returnsSingleChunk_whenContentIsExactlyOneCharacter() {
        List<String> result = chunker.extract("x", 1, 0);

        assertThat(result).containsExactly("x");
    }

    @Test
    void extract_worksCorrectly_whenChunkSizeLargerThanContent_withOverlapOfZero() {
        List<String> result = chunker.extract("abc", 100, 0);

        assertThat(result).containsExactly("abc");
    }

    @Test
    void extract_chunkSizeOfOne_withZeroOverlap_splitsEachCharacter() {
        List<String> result = chunker.extract("abcd", 1, 0);

        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    void extract_allChunksHaveAtMostChunkSizeCharacters() {
        String content = "a".repeat(97);
        int chunkSize = 10;

        List<String> result = chunker.extract(content, chunkSize, 3);

        assertThat(result).allSatisfy(chunk ->
                assertThat(chunk.length()).isLessThanOrEqualTo(chunkSize)
        );
    }

    @Test
    void extract_fullContentIsCoveredByChunks_withOverlap() {
        String content = "The quick brown fox jumps over the lazy dog";
        int chunkSize = 10;
        int overlap = 3;
        int step = chunkSize - overlap;

        List<String> result = chunker.extract(content, chunkSize, overlap);

        assertThat(result.getFirst()).isEqualTo(content.substring(0, chunkSize));

        assertThat(result.getLast()).isEqualTo(
                content.substring(content.length() - result.getLast().length())
        );

        for (int i = 1; i < result.size() - 1; i++) {
            String prev = result.get(i - 1);
            String curr = result.get(i);
            int prevStart = content.indexOf(prev);
            int currStart = content.indexOf(curr, prevStart + 1);
            assertThat(currStart - prevStart).isEqualTo(step);
        }
    }


    @Test
    void extract_producesCorrectChunkCount_forAdr005Configuration() {
        String content = "a".repeat(1000);
        int chunkSize = 200;
        int overlap = 40;

        List<String> result = chunker.extract(content, chunkSize, overlap);

        assertThat(result).hasSize(6);
        assertThat(result.getFirst()).hasSize(200);
        assertThat(result.getLast()).hasSize(200);
    }
}