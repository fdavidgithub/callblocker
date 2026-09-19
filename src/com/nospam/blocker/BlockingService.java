package com.nospam.blocker;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.util.Log;

public class BlockingService extends CallScreeningService {

    private static final String TAG = "CallBlocker";
    private static final int MIN_MATCH_DIGITS = 8;
    private static final int EMERGENCY_MAX_DIGITS = 4;

    @Override
    public void onScreenCall(final Call.Details callDetails) {
        if (!isEnabled()) {
            allow(callDetails);
            return;
        }

        final Uri handle = callDetails.getHandle();
        if (handle == null || handle.getSchemeSpecificPart() == null) {
            allow(callDetails);
            return;
        }
        final String number = handle.getSchemeSpecificPart();
        final String digits = normalize(number);
        if (digits.isEmpty() || digits.length() < EMERGENCY_MAX_DIGITS) {
            allow(callDetails);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean inContacts = isInContacts(digits);
                CallResponse response = inContacts
                        ? new CallResponse.Builder().build()
                        : new CallResponse.Builder().setRejectCall(true).setDisallowCall(true).build();
                if (!inContacts) {
                    Log.i(TAG, "Bloqueando chamada de " + number);
                }
                respondToCall(callDetails, response);
            }
        }).start();
    }

    private void allow(Call.Details callDetails) {
        respondToCall(callDetails, new CallResponse.Builder().build());
    }

    private boolean isEnabled() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        return prefs.getBoolean("enabled", true);
    }

    private boolean isInContacts(String digits) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null);
            if (c == null) {
                return false;
            }
            while (c.moveToNext()) {
                String contact = c.getString(0);
                if (contact == null) {
                    continue;
                }
                String cd = normalize(contact);
                if (digits.equals(cd) || matchesSuffix(digits, cd)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao consultar contatos", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    private static boolean matchesSuffix(String a, String b) {
        if (a.length() < MIN_MATCH_DIGITS || b.length() < MIN_MATCH_DIGITS) {
            return false;
        }
        if (a.length() >= b.length()) {
            return a.endsWith(b);
        }
        return b.endsWith(a);
    }

    private static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}