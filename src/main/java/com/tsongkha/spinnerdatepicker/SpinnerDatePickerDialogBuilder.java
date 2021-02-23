package com.tsongkha.spinnerdatepicker;

import android.content.Context;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SpinnerDatePickerDialogBuilder {

    private Context context;
    private SpinnerDatePickerDialog.OnDateSetListener callBack;
    private SpinnerDatePickerDialog.OnDateCancelListener onCancel;
    private boolean isDayShown = true;
    private boolean isYearShown = true;
    private boolean isTitleShown = true;
    private String customTitle = "";
    private int theme = 0;                 //default theme
    private int spinnerTheme = 0;          //default theme
    private Calendar defaultDate = new GregorianCalendar(1980, 0, 1);
    private Calendar minDate = new GregorianCalendar(1900, 0, 1);
    private Calendar maxDate = new GregorianCalendar(2100, 0, 1);


    public SpinnerDatePickerDialogBuilder context(Context context) {
        this.context = context;
        return this;
    }

    public SpinnerDatePickerDialogBuilder callback(SpinnerDatePickerDialog.OnDateSetListener callBack) {
        this.callBack = callBack;
        return this;
    }

    public SpinnerDatePickerDialogBuilder onCancel(SpinnerDatePickerDialog.OnDateCancelListener onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public SpinnerDatePickerDialogBuilder dialogTheme(int theme) {
        this.theme = theme;
        return this;
    }

    public SpinnerDatePickerDialogBuilder spinnerTheme(int spinnerTheme) {
        this.spinnerTheme = spinnerTheme;
        return this;
    }

    public SpinnerDatePickerDialogBuilder defaultDate(int year, int monthIndexedFromZero, int day) {
        this.defaultDate = new GregorianCalendar(year, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder defaultDate(int monthIndexedFromZero, int day) {
        this.defaultDate = new GregorianCalendar(SpinnerDatePicker.KNOWN_LEAP_YEAR, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder minDate(int year, int monthIndexedFromZero, int day) {
        this.minDate = new GregorianCalendar(year, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder minDate(int monthIndexedFromZero, int day) {
        this.minDate = new GregorianCalendar(SpinnerDatePicker.KNOWN_LEAP_YEAR, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder maxDate(int year, int monthIndexedFromZero, int day) {
        this.maxDate = new GregorianCalendar(year, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder maxDate(int monthIndexedFromZero, int day) {
        this.maxDate = new GregorianCalendar(SpinnerDatePicker.KNOWN_LEAP_YEAR, monthIndexedFromZero, day);
        return this;
    }

    public SpinnerDatePickerDialogBuilder showDaySpinner(boolean showDaySpinner) {
        this.isDayShown = showDaySpinner;
        return this;
    }

    public SpinnerDatePickerDialogBuilder showYearSpinner(boolean showYearSpinner) {
        this.isYearShown = showYearSpinner;
        return this;
    }

    public SpinnerDatePickerDialogBuilder showTitle(boolean showTitle) {
        this.isTitleShown = showTitle;
        return this;
    }

    public SpinnerDatePickerDialogBuilder customTitle(String title) {
        this.customTitle = title;
        return this;
    }

    public SpinnerDatePickerDialog build() {
        if (context == null) throw new IllegalArgumentException("Context must not be null");
        if (maxDate.getTime().getTime() <= minDate.getTime().getTime()) throw new IllegalArgumentException("Max date is not after Min date");

        return new SpinnerDatePickerDialog(context, theme, spinnerTheme, callBack, onCancel, defaultDate, minDate, maxDate, isDayShown, isYearShown, isTitleShown, customTitle);
    }
}