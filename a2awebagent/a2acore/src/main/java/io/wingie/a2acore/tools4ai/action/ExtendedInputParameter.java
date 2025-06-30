package io.wingie.a2acore.tools4ai.action;

import lombok.*;

@RequiredArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ExtendedInputParameter {
    @NonNull
    private String name;
    @NonNull
    private String type;
    private String defaultValueStr;
    private boolean hasDefaultValue;

    public boolean hasDefaultValue(){
        return hasDefaultValue;
    }
}
