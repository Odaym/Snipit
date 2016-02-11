package com.om.snipit.classes;


import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

public interface IndicatorController {
    View newInstance(@NonNull Context context);

    void initialize(int slideCount);

    void selectPosition(int index);

    void setSelectedIndicatorColor(int color);

    void setUnselectedIndicatorColor(int color);
}