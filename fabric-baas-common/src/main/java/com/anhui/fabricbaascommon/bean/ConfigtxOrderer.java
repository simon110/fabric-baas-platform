package com.anhui.fabricbaascommon.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigtxOrderer extends Node {
    private File serverTlsCert;
    private File clientTlsCert;
}
