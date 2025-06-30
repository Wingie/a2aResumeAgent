package io.wingie.a2acore.tools4ai.detect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HallucinationQA {
    private String question;
    private String answer;
    private String context;
    private String truthPercentage;


    public double calculateTruthPercent() {
        String numberString = truthPercentage.replaceAll("[^\\d.]", "");
        double doublePer = Double.parseDouble(numberString);
        return doublePer;
    }
}
