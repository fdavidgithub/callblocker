package com.nospam.blocker;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_CONTACTS = 1;
    private static final int REQ_ROLE = 2;

    private Switch enableSwitch;
    private Button contactsBtn;
    private Button roleBtn;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableSwitch = new Switch(this);
        enableSwitch.setText("Bloquear chamadas fora da agenda");
        enableSwitch.setChecked(isEnabled());
        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                setEnabled(checked);
                updateStatus();
            }
        });

        contactsBtn = new Button(this);
        contactsBtn.setText("Permissao de contatos");
        contactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
            }
        });

        roleBtn = new Button(this);
        roleBtn.setText("Ativar triagem de chamadas");
        roleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRole();
            }
        });

        statusText = new TextView(this);
        statusText.setTextSize(14);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);
        root.addView(enableSwitch);
        root.addView(contactsBtn);
        root.addView(roleBtn);
        root.addView(statusText);
        setContentView(root);

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private boolean isEnabled() {
        return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("enabled", true);
    }

    private void setEnabled(boolean v) {
        SharedPreferences.Editor e = getSharedPreferences("prefs", MODE_PRIVATE).edit();
        e.putBoolean("enabled", v);
        e.apply();
    }

    private void requestRole() {
        try {
            RoleManager rm = getSystemService(RoleManager.class);
            Intent intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
            startActivityForResult(intent, REQ_ROLE);
        } catch (Exception e) {
            Toast.makeText(this, "Nao foi possivel abrir a triagem: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasRole() {
        RoleManager rm = getSystemService(RoleManager.class);
        return rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
    }

    private boolean hasContactsPermission() {
        return checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateStatus() {
        boolean perm = hasContactsPermission();
        boolean role = hasRole();
        StringBuilder sb = new StringBuilder();
        sb.append("Permissao de contatos: ").append(perm ? "OK" : "FALTA").append('\n');
        sb.append("Triagem de chamadas: ").append(role ? "ativada" : "DESATIVADA").append('\n');
        sb.append("Bloqueio: ").append(isEnabled() ? "LIGADO" : "desligado");
        if (!perm) {
            sb.append("\n\nToque em \"Permissao de contatos\" para conceder.");
        }
        if (!role) {
            sb.append("\n\nToque em \"Ativar triagem de chamadas\" e confirme no dialogo.");
        }
        sb.append("\n\nSamsung: se chamadas fora da agenda ainda tocarem, desative a "
                + "'Protecao de identificador de chamadas' no app Telefone.");
        statusText.setText(sb.toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }
}