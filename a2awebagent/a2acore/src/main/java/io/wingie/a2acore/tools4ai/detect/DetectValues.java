package io.wingie.a2acore.tools4ai.detect;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DetectValues {
    private String prompt;
    private String context;
    private String response;
    private String additionalData;
}
