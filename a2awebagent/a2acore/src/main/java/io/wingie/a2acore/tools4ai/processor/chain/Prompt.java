package io.wingie.a2acore.tools4ai.processor.chain;

import java.util.List;

public class Prompt {
    private List<SubPrompt> prmpt;

    public List<SubPrompt> getPrmpt() {
        return prmpt;
    }

    public void setPrmpt(List<SubPrompt> prmpt) {
        this.prmpt = prmpt;
    }

    @Override
    public String toString() {
        return "Prompt{" +
                "prmpt=" + prmpt +
                '}';
    }
}
