package io.wingie.playwright;

import com.t4a.annotations.Prompt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URL Safety validation for web navigation
 * Based on the a2aPlaywrightReference implementation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class URLSafety {

    @Prompt(describe = "Is this URL safe to navigate? Return true if safe, false if not safe")
    private boolean itSafeAndValid;
}