package com.anhui.fabricbaasweb.bean;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AntMatcherCollection {
    public final String[] matchers;

    public String[] get() {
        return matchers;
    }
}
