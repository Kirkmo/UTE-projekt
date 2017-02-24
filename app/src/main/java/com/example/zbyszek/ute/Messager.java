package com.example.zbyszek.ute;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Messager {

    public static final int SEND_SMS_REQUEST_CODE = 0;
    private final Context mContext;

    private AlertDialog messagerDialog;
    private AlertDialog messageEditorDialog;
    private TimePickerDialog timePickerDialog;
    private final String placeName;
    private final String placeAddress;
    private int currentMinutes;
    private int currentHours;
    private int hours;
    private int minutes;

    private String message;
    private String receiver;
    private String sender;

    private boolean hasCustomTime = false;
    private String duration;
    private View dialogView;
    private TextView msgTextView;
    private EditText signEditText;
    private EditText recNrEditText;
    private EditText sendNrEditText;
    private EditText msgEditText;

    private APIExecutor OrangeApiExe;

    public Messager(Context context, String placeName, String placeAddress, String duration) {
        mContext = context;
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.duration = duration;
        setTime();
        buildDialogWindow();
    }

    private void setMessage() {
        String message = mContext.getString(R.string.SMS_message);

        String time = writeTime();
        String place = writePlace();

        message = String.format(message, time, place);
        int timeStart = message.indexOf(time);
        int placeStart = message.indexOf(place);
        SpannableString messageSS = new SpannableString(message);
        messageSS.setSpan(new StyleSpan(Typeface.BOLD), timeStart, timeStart + time.length(), 0);
        messageSS.setSpan(new StyleSpan(Typeface.BOLD), placeStart, placeStart + place.length(), 0);

        msgTextView.setText(messageSS);
    }

    private void setTime() {
        updateCurrentTime();
        if (duration == null) {
            hours = currentHours;
            minutes = currentMinutes;
            return;
        }
        String[] splitted = duration.split(" ");
        int hoursToAdd;
        int minutesToAdd;
        if (splitted.length < 3) {
            hoursToAdd = 0;
            minutesToAdd = Integer.valueOf(splitted[0]);
        } else {
            hoursToAdd = Integer.valueOf(splitted[0]);
            minutesToAdd = Integer.valueOf(splitted[2]);
        }
        hours = currentHours + hoursToAdd;
        minutes = currentMinutes + minutesToAdd;
        if (minutes > 60) {
            hours += minutes / 60;
            minutes = minutes % 60;
        }
        if (hours > 24) {
            hours = hours % 24;
        }
    }

    private void updateTime(int newHours, int newMinutes) {
        String oldTime = writeTime();
        hours = newHours;
        minutes = newMinutes;
        String newTime = writeTime();
        String msg = msgTextView.getText().toString().replace(oldTime, newTime);
        distinguishMessage(msg);
    }

    private void distinguishMessage(String msg) {
        String time = writeTime();
        String place = writePlace();
        int index = 0;
        int start;
        List<Integer> timeStarts = new ArrayList<>();
        while (true) {
            start = msg.indexOf(time, index);
            if (start == -1)
                break;
            timeStarts.add(start);
            index = start + 1;
        }
        index = 0;
        List<Integer> placeStarts = new ArrayList<>();
        while (true) {
            start = msg.indexOf(place, index);
            if (start == -1)
                break;
            placeStarts.add(start);
            index = start + 1;
        }
        SpannableString msgSS = new SpannableString(msg);
        for (int timeStart : timeStarts) {
            msgSS.setSpan(new StyleSpan(Typeface.BOLD), timeStart, timeStart + time.length(), 0);
        }
        for (int placeStart : placeStarts) {
            msgSS.setSpan(new StyleSpan(Typeface.BOLD), placeStart, placeStart + place.length(), 0);
        }
        msgTextView.setText(msgSS);
    }

    private void updateCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        currentMinutes = calendar.get(Calendar.MINUTE);
    }

    private String writeTime() {
        return minutes < 10
                ? hours + ":0" + minutes
                : hours + ":" + minutes;
    }

    private void sendSMS() {
        message = msgTextView.getText().toString();
        String signature = signEditText.getText().toString();
        if (signature.isEmpty())
            signature = signEditText.getHint().toString();
        message += " " + signature;
        message = StringUtils.stripAccents(message);
        receiver = recNrEditText.getText().toString();
        if (receiver.isEmpty())
            receiver = recNrEditText.getHint().toString();
        else if (!TextUtils.isDigitsOnly(receiver) || receiver.length() != 11 || !receiver.startsWith("48")) {
            Toast.makeText(mContext,"Proszę wpisać numer w formacie: 48123456789",Toast.LENGTH_LONG).show();
            return;
        }
        sender = sendNrEditText.getText().toString();
        if (!sender.isEmpty())
            if (!TextUtils.isDigitsOnly(sender) || sender.length() != 11 || !sender.startsWith("48")) {
                Toast.makeText(mContext,"Proszę wpisać numer w formacie: 48123456789",Toast.LENGTH_LONG).show();
                return;
            }
        OrangeApiExe = new APIExecutor(this, message, receiver, sender);
        OrangeApiExe.execute();
    }

    private void initLayout() {
        dialogView = View.inflate(mContext, R.layout.messager_layout, null);
        msgTextView = (TextView) dialogView.findViewById(R.id.msgEditText);
        msgTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messageEditorDialog == null) {
                    buildMessageEditorDialog(mContext);
                } else {
                    msgEditText.setText(msgTextView.getText());
                }
                messageEditorDialog.show();
                messagerDialog.hide();
            }
        });
        signEditText = (EditText) dialogView.findViewById(R.id.placeButton);
        recNrEditText = (EditText) dialogView.findViewById(R.id.recNrEditText);
        sendNrEditText = (EditText) dialogView.findViewById(R.id.sendNrEditText);
        setMessage();
    }

    private void buildMessageEditorDialog(final Context context) {
        View messageView = View.inflate(context, R.layout.message_editor, null);
        msgEditText = (EditText) messageView.findViewById(R.id.msgEditText);
        msgEditText.setText(msgTextView.getText());
        Button timeButton = (Button) messageView.findViewById(R.id.timeButton);
        timeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableString ss = new SpannableString(writeTime());
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, ss.length(), 0);
                int timeStart = msgEditText.getSelectionStart();
                msgEditText.getText().insert(timeStart,ss);
            }
        });
        Button placeButton = (Button) messageView.findViewById(R.id.placeButton);
        placeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpannableString ss = new SpannableString(writePlace());
                ss.setSpan(new StyleSpan(Typeface.BOLD), 0, ss.length(), 0);
                int cursor = msgEditText.getSelectionStart();
                msgEditText.getText().insert(cursor, ss);
            }
        });
        messageEditorDialog = new AlertDialog.Builder(context)
                .setView(messageView)
                .setTitle("Edytuj wiadomość")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        msgTextView.setText(msgEditText.getText());
                        messagerDialog.show();
                    }
                })
                .setNegativeButton("Wyjdź", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        messagerDialog.show();
                    }
                })
                .create();
    }

    private String writePlace() {
        return placeName + ", " + placeAddress;
    }

    private void buildDialogWindow() {
        initLayout();
        messagerDialog = new AlertDialog.Builder(mContext)
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
                            buildTimePickerDialog(mContext);
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
                hasCustomTime = true;
            }
        }, hours, minutes, true);
        timePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
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

    public boolean getStatus() {
        return OrangeApiExe.getStatus();
    }

    public void sendDirectSMS() {
        SmsManager.getDefault().sendTextMessage(receiver, null, message, null, null);
        Toast.makeText(mContext,"SMS wysłany bezpośrednio",Toast.LENGTH_SHORT).show();
    }

    public void showSMSOption() {
        if (!OrangeApiExe.getStatus()) {
            Snackbar.make(((Activity) mContext).findViewById(R.id.maps_activity), "Wysłanie SMSa nie powiodło się. Wysłać SMS bezpośrednio?", Snackbar.LENGTH_LONG)
                    .setAction("TAK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    String[] permissions = new String[]{Manifest.permission.SEND_SMS};
                                    ((Activity) mContext).requestPermissions(permissions, SEND_SMS_REQUEST_CODE);
                                }
                            } else
                                sendDirectSMS();
                        }
                    }).show();
        }
    }

    public void show() {
        messagerDialog.show();
    }

    public void updateAndShow() {
        if (!hasCustomTime) {
            String oldTime = writeTime();
            setTime();
            String newTime = writeTime();
            String msg = msgTextView.getText().toString().replace(oldTime, newTime);
            distinguishMessage(msg);
        }
        messagerDialog.show();
    }
}
