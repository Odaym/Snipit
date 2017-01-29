/**
 *
 */
package com.om.snipit.abbyy_ocr;

/**
 * Barcode recognition settings.
 *
 * For all possible parameters see
 * http://ocrsdk.com/documentation/apireference/processBarcodeField/
 */
public class BarcodeSettings {

  private String barcodeType = "autodetect";

  public String asUrlParams() {
    return "barcodeType=" + barcodeType;
  }

  public String getType() {
    return barcodeType;
  }

  public void setType(String newType) {
    barcodeType = newType;
  }
}
