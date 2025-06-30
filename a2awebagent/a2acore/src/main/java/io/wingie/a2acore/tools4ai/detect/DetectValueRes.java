package io.wingie.a2acore.tools4ai.detect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DetectValueRes {
    private List<HallucinationQA> hallucinationList;
}
