package com.tsongkha.spinnerdatepicker;
/* Fork of Oreo DatePickerSpinnerDelegate
 *
 * Original class is Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * A delegate implementing the basic DatePicker
 */
@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class SpinnerDatePicker extends FrameLayout {

    private static final boolean DEFAULT_ENABLED_STATE = true;

    public static final int KNOWN_LEAP_YEAR = 2000;

    private final LinearLayout mPickerContainer;

    private NumberPicker mDaySpinner;

    private NumberPicker mMonthSpinner;

    private NumberPicker mYearSpinner;

    private final EditText mDaySpinnerInput;

    private final EditText mMonthSpinnerInput;

    private final EditText mYearSpinnerInput;

    private OnDateChangedListener mOnDateChangedListener;

    private String[] mShortMonths;

    private int mNumberOfMonths;

    private Calendar mTempDate;

    private Calendar mMinDate;

    private Calendar mMaxDate;

    private Calendar mCurrentDate;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;

    private boolean mIsDayShown = true;

    private boolean mIsYearShown = true;

    SpinnerDatePicker(ViewGroup root, int numberPickerStyle) {
        super(root.getContext());
        Context mContext = root.getContext();

        // initialization based on locale
        setCurrentLocale(Locale.getDefault());

        LayoutInflater inflater = (LayoutInflater) new ContextThemeWrapper(mContext,
                                                                           numberPickerStyle).getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.date_picker_container, this, true);

        mPickerContainer = findViewById(R.id.parent);

        OnValueChangeListener onChangeListener = (picker, oldVal, newVal) -> {
            updateInputState();
            mTempDate.set(Calendar.MONTH, mCurrentDate.get(Calendar.MONTH));
            mTempDate.set(Calendar.DAY_OF_MONTH, mCurrentDate.get(Calendar.DAY_OF_MONTH));
            if (mIsYearShown) {
                mTempDate.set(Calendar.YEAR, mCurrentDate.get(Calendar.YEAR));
            } else {
                mTempDate.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
            }
            // take care of wrapping of days and months to update greater fields
            if (picker == mDaySpinner) {
                int maxDayOfMonth = mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (oldVal == maxDayOfMonth && newVal == 1) {
                    mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                } else if (oldVal == 1 && newVal == maxDayOfMonth) {
                    mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                } else {
                    mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                }
            } else if (picker == mMonthSpinner) {
                if (oldVal == 11 && newVal == 0) {
                    mTempDate.add(Calendar.MONTH, 1);
                } else if (oldVal == 0 && newVal == 11) {
                    mTempDate.add(Calendar.MONTH, -1);
                } else {
                    mTempDate.add(Calendar.MONTH, newVal - oldVal);
                }
            } else if (picker == mYearSpinner) {
                mTempDate.set(Calendar.YEAR, newVal);
            } else {
                throw new IllegalArgumentException();
            }
            // now set the date to the adjusted one
            if (mIsYearShown) {
                setDate(mTempDate.get(Calendar.YEAR), mTempDate.get(Calendar.MONTH),
                        mTempDate.get(Calendar.DAY_OF_MONTH));
            } else {
                setDate(mTempDate.get(Calendar.MONTH), mTempDate.get(Calendar.DAY_OF_MONTH));
            }
            updateSpinners();
            notifyDateChanged();
        };

        // day
        mDaySpinner = (NumberPicker) inflater.inflate(R.layout.number_picker_day_month,
                                                      mPickerContainer, false);
        mDaySpinner.setId(R.id.day);
        mDaySpinner.setFormatter(new TwoDigitFormatter());
        mDaySpinner.setOnLongPressUpdateInterval(100);
        mDaySpinner.setOnValueChangedListener(onChangeListener);
        mDaySpinnerInput = NumberPickers.findEditText(mDaySpinner);


        // month
        mMonthSpinner = (NumberPicker) inflater.inflate(R.layout.number_picker_day_month,
                                                        mPickerContainer, false);
        mMonthSpinner.setId(R.id.month);
        mMonthSpinner.setMinValue(0);
        mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
        mMonthSpinner.setDisplayedValues(mShortMonths);
        mMonthSpinner.setOnLongPressUpdateInterval(200);
        mMonthSpinner.setOnValueChangedListener(onChangeListener);
        mMonthSpinnerInput = NumberPickers.findEditText(mMonthSpinner);

        // year
        mYearSpinner = (NumberPicker) inflater.inflate(R.layout.number_picker_year,
                                                       mPickerContainer, false);
        mYearSpinner.setId(R.id.year);
        mYearSpinner.setOnLongPressUpdateInterval(100);
        mYearSpinner.setOnValueChangedListener(onChangeListener);
        mYearSpinnerInput = NumberPickers.findEditText(mYearSpinner);

        // initialize to current date
        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        if (!mIsYearShown) {
            mCurrentDate.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
        }

        // re-order the number spinners to match the current date format
        reorderSpinners();

        // If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        root.addView(this);
    }

    void init(int year,
              int monthOfYear,
              int dayOfMonth,
              boolean isDayShown,
              boolean isYearShown,
              OnDateChangedListener onDateChangedListener) {
        mIsDayShown = isDayShown;
        mIsYearShown = isYearShown;
        setDate(year, monthOfYear, dayOfMonth);
        updateSpinners();
        mOnDateChangedListener = onDateChangedListener;
        notifyDateChanged();
    }

    void init(int monthOfYear,
              int dayOfMonth,
              boolean isDayShown,
              boolean isYearShown,
              OnDateChangedListener onDateChangedListener) {
        mIsDayShown = isDayShown;
        mIsYearShown = isYearShown;
        setDate(monthOfYear, dayOfMonth);
        updateSpinners();
        mOnDateChangedListener = onDateChangedListener;
        notifyDateChanged();
    }

    void updateDate(int year, int month, int dayOfMonth) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateSpinners();
        notifyDateChanged();
    }

    void updateDate(int month, int dayOfMonth) {
        if (!isNewDate(month, dayOfMonth)) {
            return;
        }
        setDate(month, dayOfMonth);
        updateSpinners();
        notifyDateChanged();
    }

    int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    void setMinDate(Calendar minDate) {
        mTempDate.set(Calendar.MONTH, minDate.get(Calendar.MONTH));
        mTempDate.set(Calendar.DAY_OF_MONTH, minDate.get(Calendar.DAY_OF_MONTH));
        mTempDate.set(Calendar.YEAR, minDate.get(Calendar.YEAR));
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMinDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        mMinDate.set(Calendar.MONTH, minDate.get(Calendar.MONTH));
        mMinDate.set(Calendar.DAY_OF_MONTH, minDate.get(Calendar.DAY_OF_MONTH));
        mMinDate.set(Calendar.YEAR, minDate.get(Calendar.YEAR));
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.set(Calendar.MONTH, mMinDate.get(Calendar.MONTH));
            mCurrentDate.set(Calendar.DAY_OF_MONTH, mMinDate.get(Calendar.DAY_OF_MONTH));
            mCurrentDate.set(Calendar.YEAR, mMinDate.get(Calendar.YEAR));
        }
        updateSpinners();
    }

    void setMaxDate(Calendar maxDate) {
        mTempDate.set(Calendar.MONTH, maxDate.get(Calendar.MONTH));
        mTempDate.set(Calendar.DAY_OF_MONTH, maxDate.get(Calendar.DAY_OF_MONTH));
        mTempDate.set(Calendar.YEAR, maxDate.get(Calendar.YEAR));
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) == mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            // Same day, no-op.
            return;
        }
        mMaxDate.set(Calendar.MONTH, maxDate.get(Calendar.MONTH));
        mMaxDate.set(Calendar.DAY_OF_MONTH, maxDate.get(Calendar.DAY_OF_MONTH));
        mMaxDate.set(Calendar.YEAR, maxDate.get(Calendar.YEAR));
        if (mCurrentDate.before(mMaxDate)) {
            mCurrentDate.set(Calendar.MONTH, mMaxDate.get(Calendar.MONTH));
            mCurrentDate.set(Calendar.DAY_OF_MONTH, mMaxDate.get(Calendar.DAY_OF_MONTH));
            mCurrentDate.set(Calendar.YEAR, mMaxDate.get(Calendar.YEAR));
        }
        updateSpinners();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mDaySpinner.setEnabled(enabled);
        mMonthSpinner.setEnabled(enabled);
        mYearSpinner.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    protected void setCurrentLocale(Locale locale) {
        mTempDate = getCalendarForLocale(mTempDate, locale, mIsYearShown);
        mMinDate = getCalendarForLocale(mMinDate, locale, mIsYearShown);
        mMaxDate = getCalendarForLocale(mMaxDate, locale, mIsYearShown);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale, mIsYearShown);

        mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
        mShortMonths = new DateFormatSymbols().getShortMonths();

        if (usingNumericMonths()) {
            // We're in a locale where a date should either be all-numeric, or all-text.
            // All-text would require custom NumberPicker formatters for day and year.
            mShortMonths = new String[mNumberOfMonths];
            for (int i = 0; i < mNumberOfMonths; ++i) {
                mShortMonths[i] = String.format(Locale.ROOT, "%d", i + 1);
            }
        }
    }

    /**
     * Tests whether the current locale is one where there are no real month names,
     * such as Chinese, Japanese, or Korean locales.
     */
    private boolean usingNumericMonths() {
        return Character.isDigit(mShortMonths[Calendar.JANUARY].charAt(0));
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given calendar.
     *  @param oldCalendar The old calendar.
     * @param locale      The locale.
     * @param isYearShown Is the year spinner shown
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale, boolean isYearShown) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.set(Calendar.MONTH, oldCalendar.get(Calendar.MONTH));
            newCalendar.set(Calendar.DAY_OF_MONTH, oldCalendar.get(Calendar.DAY_OF_MONTH));
            if (isYearShown) {
                newCalendar.set(Calendar.YEAR, oldCalendar.get(Calendar.YEAR));
            } else {
                newCalendar.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
            }
            return newCalendar;
        }
    }

    /**
     * Reorders the spinners according to the date format that is
     * explicitly set by the user and if no such is set fall back
     * to the current locale's default format.
     */
    private void reorderSpinners() {
        mPickerContainer.removeAllViews();
        // We use numeric spinners for year and day, but textual months. Ask icu4c what
        // order the user's locale uses for that combination. http://b/7207103.
        String pattern;
        pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMdd");
        char[] order = ICU.getDateFormatOrder(pattern);
        final int spinnerCount = order.length;
        for (int i = 0; i < spinnerCount; i++) {
            switch (order[i]) {
                case 'd':
                    mPickerContainer.addView(mDaySpinner);
                    setImeOptions(mDaySpinner, spinnerCount, i);
                    break;
                case 'M':
                    mPickerContainer.addView(mMonthSpinner);
                    setImeOptions(mMonthSpinner, spinnerCount, i);
                    break;
                case 'y':
                    mPickerContainer.addView(mYearSpinner);
                    setImeOptions(mYearSpinner, spinnerCount, i);
                    break;
                default:
                    throw new IllegalArgumentException(Arrays.toString(order));
            }
        }
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != month
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != dayOfMonth);
    }

    private boolean isNewDate(int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.MONTH) != month
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != dayOfMonth);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }

    private void setDate(int month, int dayOfMonth) {
        mCurrentDate.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
        mCurrentDate.set(Calendar.MONTH, month);
        mCurrentDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    private void updateSpinners() {
        // set the spinner ranges respecting the min and max dates
        mDaySpinner.setVisibility(mIsDayShown ? View.VISIBLE : View.GONE);
        mYearSpinner.setVisibility(mIsYearShown ? View.VISIBLE : View.GONE);
        if (mIsYearShown && mCurrentDate.equals(mMinDate)) {
            mDaySpinner.setMinValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(mCurrentDate.get(Calendar.MONTH));
            mMonthSpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
            mMonthSpinner.setWrapSelectorWheel(false);
        } else if (mIsYearShown && mCurrentDate.equals(mMaxDate)) {
            mDaySpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            mDaySpinner.setMaxValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
            mMonthSpinner.setMaxValue(mCurrentDate.get(Calendar.MONTH));
            mMonthSpinner.setWrapSelectorWheel(false);
        } else {
            mDaySpinner.setMinValue(1);
            if (mIsYearShown) {
                mDaySpinner.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            } else {
                mTempDate = (Calendar)mCurrentDate.clone();
                mTempDate.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
                mDaySpinner.setMaxValue(mTempDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            }
            mDaySpinner.setWrapSelectorWheel(true);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(11);
            mMonthSpinner.setWrapSelectorWheel(true);
        }

        // make sure the month names are a zero based array
        // with the months in the month spinner
        String[] displayedValues = Arrays.copyOfRange(mShortMonths,
                                                      mMonthSpinner.getMinValue(),
                                                      mMonthSpinner.getMaxValue() + 1);
        mMonthSpinner.setDisplayedValues(displayedValues);

        // year spinner range does not change based on the current date
        mYearSpinner.setMinValue(mMinDate.get(Calendar.YEAR));
        mYearSpinner.setMaxValue(mMaxDate.get(Calendar.YEAR));
        mYearSpinner.setWrapSelectorWheel(false);

        // set the spinner values
        if (mIsYearShown) {
            mYearSpinner.setValue(mCurrentDate.get(Calendar.YEAR));
        }
        mMonthSpinner.setValue(mCurrentDate.get(Calendar.MONTH));
        mDaySpinner.setValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));

        if (usingNumericMonths()) {
            mMonthSpinnerInput.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }


    /**
     * Notifies the listener, if such, for a change in the selected date.
     */
    private void notifyDateChanged() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnDateChangedListener != null) {
            if (mCurrentDate.isSet(Calendar.YEAR)) {
                mOnDateChangedListener.onDateChanged(this, getYear(), getMonth(),
                        getDayOfMonth());
            } else {
                mOnDateChangedListener.onDateChanged(this, getMonth(),
                        getDayOfMonth());
            }
        }
    }

    /**
     * Sets the IME options for a spinner based on its ordering.
     *
     * @param spinner      The spinner.
     * @param spinnerCount The total spinner count.
     * @param spinnerIndex The index of the given spinner.
     */
    private void setImeOptions(NumberPicker spinner, int spinnerCount, int spinnerIndex) {
        final int imeOptions;
        if (spinnerIndex < spinnerCount - 1) {
            imeOptions = EditorInfo.IME_ACTION_NEXT;
        } else {
            imeOptions = EditorInfo.IME_ACTION_DONE;
        }
        TextView input = NumberPickers.findEditText(spinner);
        if (input != null) {
            input.setImeOptions(imeOptions);
        }
    }

    private void updateInputState() {
        // Make sure that if the user changes the value and the IME is active
        // for one of the inputs if this widget, the IME is closed. If the user
        // changed the value via the IME and there is a next input the IME will
        // be shown, otherwise the user chose another means of changing the
        // value and having the IME up makes no sense.
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            if (inputMethodManager.isActive(mYearSpinnerInput)) {
                mYearSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mMonthSpinnerInput)) {
                mMonthSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mDaySpinnerInput)) {
                mDaySpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        return new SavedState(superState, mCurrentDate, mMinDate, mMaxDate, mIsDayShown, mIsYearShown);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mCurrentDate = Calendar.getInstance();
        mCurrentDate.setTimeInMillis(ss.currentDate);
        mMinDate = Calendar.getInstance();
        mMinDate.setTimeInMillis(ss.minDate);
        mMaxDate = Calendar.getInstance();
        mMaxDate.setTimeInMillis(ss.maxDate);
        if (!ss.isYearSpinnerShown) {
            mCurrentDate.set(Calendar.YEAR, KNOWN_LEAP_YEAR);
        }
        updateSpinners();
    }

    private static class SavedState extends BaseSavedState {

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        final long currentDate;
        final long minDate;
        final long maxDate;
        final boolean isDaySpinnerShown;
        final boolean isYearSpinnerShown;

        /**
         * Constructor called from {@link SpinnerDatePicker#onSaveInstanceState()}
         */
        SavedState(Parcelable superState,
                   Calendar currentDate,
                   Calendar minDate,
                   Calendar maxDate,
                   boolean isDaySpinnerShown,
                   boolean isYearSpinnerShown) {
            super(superState);
            this.currentDate = currentDate.getTimeInMillis();
            this.minDate = minDate.getTimeInMillis();
            this.maxDate = maxDate.getTimeInMillis();
            this.isDaySpinnerShown = isDaySpinnerShown;
            this.isYearSpinnerShown = isYearSpinnerShown;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            this.currentDate = in.readLong();
            this.minDate = in.readLong();
            this.maxDate = in.readLong();
            this.isDaySpinnerShown = in.readByte() != 0;
            this.isYearSpinnerShown = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(currentDate);
            dest.writeLong(minDate);
            dest.writeLong(maxDate);
            dest.writeByte(isDaySpinnerShown ? (byte) 1 : (byte) 0);
            dest.writeByte(isYearSpinnerShown ? (byte) 1 : (byte) 0);
        }
    }
}