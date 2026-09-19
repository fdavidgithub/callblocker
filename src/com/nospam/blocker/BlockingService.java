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
    private static final String PREFS = "prefs";
    private static final String DIAG_NUMBER = "diag_number";
    private static final String DIAG_DIGITS = "diag_digits";
    private static final String DIAG_CONTACTS_READ = "diag_contacts_read";
    private static final String DIAG_QUERY = "diag_query";
    private static final String DIAG_RESULT = "diag_result";

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
                ContactLookup lookup = new ContactLookup();
                isInContacts(digits, lookup);
                boolean inContacts = lookup.found;
                CallResponse response = inContacts
                        ? new CallResponse.Builder().build()
                        : new CallResponse.Builder().setRejectCall(true).setDisallowCall(true).build();
                recordDiagnostic(number, digits, lookup, !inContacts);
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
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getBoolean("enabled", true);
    }

    private void recordDiagnostic(String number, String digits, ContactLookup lookup, boolean blocked) {
        try {
            SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            e.putString(DIAG_NUMBER, number);
            e.putString(DIAG_DIGITS, digits);
            e.putInt(DIAG_CONTACTS_READ, lookup.scanned);
            e.putString(DIAG_QUERY, lookup.status);
            e.putString(DIAG_RESULT, blocked ? "BLOQUEADA" : "permitida");
            e.apply();
        } catch (Exception ex) {
            Log.e(TAG, "Erro ao salvar diagnostico", ex);
        }
    }

    private void isInContacts(String digits, ContactLookup out) {
        ContentResolver cr = getContentResolver();
        Cursor c = null;
        try {
            c = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null);
            if (c == null) {
                out.status = "NULL_CURSOR";
                return;
            }
            while (c.moveToNext()) {
                out.scanned++;
                String contact = c.getString(0);
                if (contact == null) {
                    continue;
                }
                String cd = normalize(contact);
                if (digits.equals(cd) || matchesSuffix(digits, cd)) {
                    out.found = true;
                    out.status = "OK";
                    return;
                }
            }
            out.status = "OK";
        } catch (Exception e) {
            out.status = e.toString();
            Log.e(TAG, "Erro ao consultar contatos", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
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
        String digits = sb.toString();
        if (digits.length() >= 11 && digits.charAt(0) == '0') {
            return digits.substring(1);
        }
        return digits;
    }

    private static class ContactLookup {
        boolean found;
        int scanned;
        String status = "OK";
    }
}