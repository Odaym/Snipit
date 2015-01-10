/**
 *
 */
package com.ttco.bookmarker.ocr;

/**
 * Business card processing settings.
 * <p/>
 * For all possible settings see
 * http://ocrsdk.com/documentation/apireference/processBusinessCard/
 */
public class BusCardSettings {

    private String language = "English";
    private OutputFormat outputFormat = OutputFormat.vCard;

    public String asUrlParams() {
        // For all possible parameters, see documentation at
        // http://ocrsdk.com/documentation/apireference/processBusinessCard/
        return String.format("language=%s&exportFormat=%s", language, outputFormat);
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat format) {
        outputFormat = format;
    }

    public String getLanguage() {
        return language;
    }

    /*
     * Set recognition language. You can set any language listed at
     * http://ocrsdk.com/documentation/specifications/recognition-languages/ or
     * set comma-separated combination of them.
     *
     * Examples: English English,ChinesePRC English,French,German
     */
    public void setLanguage(String newLanguage) {
        language = newLanguage;
    }

    public enum OutputFormat {
        vCard, xml, csv
    }
}
