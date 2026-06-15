package com.github.youjiyun1123.youregexinterpreter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class YouRegexBundleTest {

    @Nested
    @DisplayName("Match results i18n")
    inner class MatchResultMessageTests {

        @Test
        @DisplayName("ui.matches.found 应正确替换占位符")
        fun foundMessageSubstitutesPlaceholder() {
            val result = YouRegexBundle.message("ui.matches.found", 3)
            assertThat(result).doesNotContain("%d")
            assertThat(result).doesNotContain("{0}")
            assertThat(result).contains("3")
        }

        @Test
        @DisplayName("ui.match.item 应正确替换所有占位符")
        fun matchItemSubstitutesPlaceholders() {
            val result = YouRegexBundle.message(
                "ui.match.item",
                1,
                "17746834978",
                0,
                10
            )
            assertThat(result).doesNotContain("%d")
            assertThat(result).doesNotContain("%s")
            assertThat(result).doesNotContain("{0}")
            assertThat(result).doesNotContain("{1}")
            assertThat(result).doesNotContain("{2}")
            assertThat(result).doesNotContain("{3}")
            assertThat(result).contains("1")
            assertThat(result).contains("17746834978")
        }
    }
}
