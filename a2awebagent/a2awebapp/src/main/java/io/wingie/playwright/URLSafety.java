package io.wingie.playwright;

import io.wingie.a2acore.tools4ai.annotations.Prompt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * URL Safety validation for web navigation
 * Data model for URL safety checks with AI prompt annotation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class URLSafety {

    @Prompt(describe = "Is this URL safe to navigate? Return true if safe, false if not safe")
    private boolean itSafeAndValid;
}