package com.eigengraph.egf2.gradle

import com.eigengraph.egf2.generator.EGF2Config

class EGF2Extension implements EGF2Config {
    String url
    String urlPrefix
    File source
    File targetDirectory
    String targetPackage
    String prefixForModels
    String modelForFile
    String[] kinds
    String[] excludeModels = []
}
