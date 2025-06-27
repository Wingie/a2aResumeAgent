package io.vishalmysore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class URLSafety {
    boolean isItSafeAndValid;
    
    // Explicit getter method to match expected method name
    public boolean isItSafeAndValid() {
        return isItSafeAndValid;
    }
}
