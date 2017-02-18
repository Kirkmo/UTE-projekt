package com.example.zbyszek.ute;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import org.apache.commons.lang3.StringUtils;

import java.util.Calendar;

public class Messager {

    private static final int METRES_PER_MINUTE = 80;

    private AlertDialog messagerDialog;
    private TimePickerDialog timePickerDialog;
    private final String placeName;
    private final String placeAddress;
    private int toMinutes;
    private int currentMinutes;
    private int currentHours;
    private int hours;
    private int minutes;

    private String toTime;
    private View dialogView;
    private TextView msgTextView;
    private EditText signEditText;
    private EditText recNrEditText;
    private EditText sendNrEditText;

    public Messager(Context context, String placeName, String placeAddress, float distance){
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        setToTime(distance);
        setTime();
//        updateMessage();
        showDialogWindow(context);
    }

    private void updateMessage() {
        msgTextView.setText("");
        String place = placeName + ", " + placeAddress;

        SpannableString toTimeSsb = new SpannableString(toTime);
        toTimeSsb.setSpan(new StyleSpan(Typeface.BOLD),0,toTimeSsb.length(),0);

        SpannableString timeSsb = new SpannableString(writeTime());
        timeSsb.setSpan(new StyleSpan(Typeface.BOLD),0,timeSsb.length(),0);

        SpannableString placeSsb = new SpannableString(place);
        placeSsb.setSpan(new StyleSpan(Typeface.BOLD),0,placeSsb.length(),0);

        msgTextView.append("Witaj! Za ok. ");
        msgTextView.append(toTimeSsb);
        msgTextView.append(" (");
        msgTextView.append(timeSsb);
        msgTextView.append("), odwiedzę ");
        msgTextView.append(placeSsb);
        msgTextView.append(". Do zobaczenia na miejscu!");
//        message =  + toTime
//                + " (o " + writeTime() + ")"
//                + " odwiedzę " + placeName
//                + ", " + placeAddress
//                + ". Do zobaczenia na miejscu!"
        ;
    }


    private void setToTime(float distance) {
        toMinutes = Math.round(distance / METRES_PER_MINUTE);
        toTime = convertToTime(toMinutes);
        setTime();
    }

    private void updateTime(int newHours, int newMinutes) {
        hours = newHours;
        minutes = newMinutes;
        updateTime();
    }

    private void updateTime() {
        updateCurrentTime();
        int minutesToAdd = minutes - currentMinutes;
        int hoursToAdd = hours - currentHours;
        if (minutes < currentMinutes) {
            hoursToAdd++;
        }
        if (hours < currentHours) {
            hoursToAdd += 24;
        }
        toMinutes += (60 * hoursToAdd + minutesToAdd);
        toTime = convertToTime(toMinutes);
    }

    private void setTime() {
        updateCurrentTime();
        int min = currentMinutes + toMinutes;
        hours = (currentHours + min / 60) % 24;
        minutes = min % 60;
    }

    private void updateCurrentTime() {
        Calendar cal = Calendar.getInstance();
        currentHours = cal.get(Calendar.HOUR_OF_DAY);
        currentMinutes = cal.get(Calendar.MINUTE);
    }

    private String writeTime() {
        return minutes < 10
                ? hours + ":0" + minutes
                : hours + ":" + minutes;
    }

    private void sendSMS() {
        updateTime();
        updateMessage();
        String message = msgTextView.getText().toString();
        String signature = signEditText.getText().toString();
        if (signature.isEmpty())
            signature = signEditText.getHint().toString();
        message += (" " + signature);
        message = StringUtils.stripAccents(message);
        String receiver = recNrEditText.getText().toString();
        if (receiver.isEmpty())
            receiver = recNrEditText.getHint().toString();
        String sender = sendNrEditText.getText().toString();
        new APIExecutor(message, receiver, sender).execute();
    }

    private String convertToTime(int min) {
        StringBuilder sb = new StringBuilder();
        if (min > 60) {
            String hours = String.valueOf(min / 60);
            sb.append(hours).append(" h ");
        }
        String minutes = String.valueOf(min % 60);
        sb.append(minutes).append(" min");
        return sb.toString();
    }

    private void initLayout(Context context) {
        dialogView = View.inflate(context, R.layout.messager_layout, null);
        msgTextView = (TextView) dialogView.findViewById(R.id.msgTextView);
        signEditText = (EditText) dialogView.findViewById(R.id.signEditText);
        recNrEditText = (EditText) dialogView.findViewById(R.id.recNrEditText);
        sendNrEditText = (EditText) dialogView.findViewById(R.id.sendNrEditText);
        updateMessage();
    }

    private void showDialogWindow(final Context context) {
        initLayout(context);
        messagerDialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle("Wyślij wiadomość")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendSMS();
                    }
                })
                .setNeutralButton("Zmień czas", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (timePickerDialog == null) {
                            buildTimePickerDialog(context);
                        }
                        timePickerDialog.show();
                    }
                })
                .setNegativeButton("Wyjdź", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        messagerDialog.show();
    }

    private void buildTimePickerDialog(Context context) {
        timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                updateTime(hourOfDay, minute);
            }
        },hours,minutes,true);
        timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateMessage();
                messagerDialog.show();
            }
        });
        timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                messagerDialog.show();
            }
        });
        timePickerDialog.show();
    }
}
