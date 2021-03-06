package org.librepilot.lp2go.controller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.librepilot.lp2go.H;
import org.librepilot.lp2go.MainActivity;
import org.librepilot.lp2go.R;
import org.librepilot.lp2go.VisualLog;
import org.librepilot.lp2go.c.PID;
import org.librepilot.lp2go.helper.SettingsHelper;
import org.librepilot.lp2go.uavtalk.UAVTalkDeviceHelper;
import org.librepilot.lp2go.uavtalk.UAVTalkObjectTree;
import org.librepilot.lp2go.uavtalk.UAVTalkXMLObject;
import org.librepilot.lp2go.ui.PidTextView;
import org.librepilot.lp2go.ui.SingleToast;
import org.librepilot.lp2go.ui.alertdialog.PidInputAlertDialog;

import java.util.HashSet;

public class ViewControllerVerticalPid extends ViewController implements View.OnClickListener {
    private final ImageView imgVPidDownload;
    private final ImageView imgVPidSave;
    private final ImageView imgVPidUpload;
    private final HashSet<PidTextView> mVerticalPidTexts;

    public ViewControllerVerticalPid(MainActivity activity, int title, int localSettingsVisible,
                                     int flightSettingsVisible) {
        super(activity, title, localSettingsVisible, flightSettingsVisible);

        final MainActivity ma = getMainActivity();

        ma.mViews.put(VIEW_VPID, ma.getLayoutInflater().inflate(R.layout.activity_vpid, null));
        ma.setContentView(ma.mViews.get(VIEW_VPID));

        imgVPidUpload = (ImageView) ma.findViewById(R.id.imgVPidUpload);
        imgVPidDownload = (ImageView) ma.findViewById(R.id.imgVPidDownload);
        imgVPidSave = (ImageView) ma.findViewById(R.id.imgVPidSave);

        if (imgVPidUpload != null) {
            imgVPidUpload.setOnClickListener(this);
        }
        if (imgVPidDownload != null) {
            imgVPidDownload.setOnClickListener(this);
        }
        if (imgVPidSave != null) {
            imgVPidSave.setOnClickListener(this);
        }

        mVerticalPidTexts = new HashSet<PidTextView>();

        PidTextView txtVerticalAltitudeProportional =
                (PidTextView) findViewById(R.id.txtVerticalAltitudeProportional);
        if (txtVerticalAltitudeProportional != null) {
            txtVerticalAltitudeProportional.init(
                    PID.PID_VERTICAL_ALTI_PROP_DENOM,
                    PID.PID_VERTICAL_ALTI_PROP_MAX,
                    PID.PID_VERTICAL_ALTI_PROP_STEP,
                    PID.PID_VERTICAL_ALTI_PROP_DFS,
                    getString(R.string.VPID_NAME_ALP),
                    "VerticalPosP", "");
        }
        mVerticalPidTexts.add(txtVerticalAltitudeProportional);

        PidTextView txtVerticalExponential =
                (PidTextView) findViewById(R.id.txtVerticalExponential);
        if (txtVerticalExponential != null) {
            txtVerticalExponential.init(
                    PID.PID_VERTICAL_EXPO_DENOM,
                    PID.PID_VERTICAL_EXPO_MAX,
                    PID.PID_VERTICAL_EXPO_STEP,
                    PID.PID_VERTICAL_EXPO_DFS,
                    getString(R.string.VPID_NAME_EXP),
                    "ThrustExp", "",
                    UAVTalkXMLObject.FIELDTYPE_UINT8);
        }
        mVerticalPidTexts.add(txtVerticalExponential);

        PidTextView txtVerticalThrustRate =
                (PidTextView) findViewById(R.id.txtVerticalThrustRate);
        if (txtVerticalThrustRate != null) {
            txtVerticalThrustRate.init(
                    PID.PID_VERTICAL_THRUST_R_DENOM,
                    PID.PID_VERTICAL_THRUST_R_MAX,
                    PID.PID_VERTICAL_THRUST_R_STEP,
                    PID.PID_VERTICAL_THRUST_R_DFS,
                    getString(R.string.VPID_NAME_THR),
                    "ThrustRate", "");
        }
        mVerticalPidTexts.add(txtVerticalThrustRate);

        PidTextView txtVerticalVelocityBeta =
                (PidTextView) findViewById(R.id.txtVerticalVelocityBeta);
        if (txtVerticalVelocityBeta != null) {
            txtVerticalVelocityBeta.init(
                    PID.PID_VERTICAL_VELO_BETA_DENOM,
                    PID.PID_VERTICAL_VELO_BETA_MAX,
                    PID.PID_VERTICAL_VELO_BETA_STEP,
                    PID.PID_VERTICAL_VELO_BETA_DFS,
                    getString(R.string.VPID_NAME_VEB),
                    "VerticalVelPID", "Beta");
        }
        mVerticalPidTexts.add(txtVerticalVelocityBeta);

        PidTextView txtVerticalVelocityDerivative =
                (PidTextView) findViewById(R.id.txtVerticalVelocityDerivative);
        if (txtVerticalVelocityDerivative != null) {
            txtVerticalVelocityDerivative.init(
                    PID.PID_VERTICAL_VELO_DERI_DENOM,
                    PID.PID_VERTICAL_VELO_DERI_MAX,
                    PID.PID_VERTICAL_VELO_DERI_STEP,
                    PID.PID_VERTICAL_VELO_DERI_DFS,
                    getString(R.string.VPID_NAME_VED),
                    "VerticalVelPID", "Kd");
        }
        mVerticalPidTexts.add(txtVerticalVelocityDerivative);

        PidTextView txtVerticalVelocityIntegral =
                (PidTextView) findViewById(R.id.txtVerticalVelocityIntegral);
        if (txtVerticalVelocityIntegral != null) {
            txtVerticalVelocityIntegral.init(
                    PID.PID_VERTICAL_VELO_INTE_DENOM,
                    PID.PID_VERTICAL_VELO_INTE_MAX,
                    PID.PID_VERTICAL_VELO_INTE_STEP,
                    PID.PID_VERTICAL_VELO_INTE_DFS,
                    getString(R.string.VPID_NAME_VEI),
                    "VerticalVelPID", "Ki");
        }
        mVerticalPidTexts.add(txtVerticalVelocityIntegral);

        PidTextView txtVerticalVelocityProportional =
                (PidTextView) findViewById(R.id.txtVerticalVelocityProportional);
        if (txtVerticalVelocityProportional != null) {
            txtVerticalVelocityProportional.init(
                    PID.PID_VERTICAL_VELO_PROP_DENOM,
                    PID.PID_VERTICAL_VELO_PROP_MAX,
                    PID.PID_VERTICAL_VELO_PROP_STEP,
                    PID.PID_VERTICAL_VELO_PROP_DFS,
                    getString(R.string.VPID_NAME_VEP),
                    "VerticalVelPID", "Kp");
        }
        mVerticalPidTexts.add(txtVerticalVelocityProportional);

        for (PidTextView ptv : mVerticalPidTexts) {
            ptv.setOnClickListener(this);
        }
    }

    @Override
    public void enter(int view) {
        super.enter(view);

        final MainActivity ma = getMainActivity();

        SingleToast.show(ma, R.string.CHECK_PID_WARNING, Toast.LENGTH_SHORT);

        try {

            final View lloStickResponse = findViewById(R.id.lloStickResponse);
            final View lloControllCoeff = findViewById(R.id.lloControlCoeff);

            if (lloStickResponse != null && lloControllCoeff != null) {
                if (SettingsHelper.mColorfulVPid) {
                    lloStickResponse.setBackground(
                            ContextCompat.getDrawable(ma, R.drawable.border_top_yellow));
                    lloControllCoeff.setBackground(
                            ContextCompat.getDrawable(ma, R.drawable.border_top_blue));
                } else {
                    lloStickResponse.setBackground(
                            ContextCompat.getDrawable(ma, R.drawable.border_top));
                    lloControllCoeff.setBackground(
                            ContextCompat.getDrawable(ma, R.drawable.border_top));
                }
            }
        } catch (NullPointerException e2) {
            VisualLog.d("MainActivity", "VIEW_VPID", e2);
        }

    }

    @Override
    public void update() {
        super.update();
        for (PidTextView ptv : mVerticalPidTexts) {
            String data;
            switch (ptv.getFieldType()) {
                case (UAVTalkXMLObject.FIELDTYPE_FLOAT32):
                    data = ptv.getDecimalString(
                            toFloat(getData("AltitudeHoldSettings",
                                    ptv.getField(), ptv.getElement())));
                    ptv.setText(data);
                    break;
                case (UAVTalkXMLObject.FIELDTYPE_UINT8):
                    data = getData("AltitudeHoldSettings",
                            ptv.getField(), ptv.getElement()).toString();
                    ptv.setText(data);
                    break;
                default:
                    break;

            }
        }
        getMainActivity().mFcDevice.requestObject("AltitudeHoldSettings");

    }

    @Override
    public void onToolbarLocalSettingsClick(View v) {
        final String[] items = {getString(R.string.COLORFUL_VIEW)};
        final boolean[] checkedItems = {true};

        checkedItems[0] = SettingsHelper.mColorfulVPid;

        AlertDialog dialog = new AlertDialog.Builder(getMainActivity())
                .setTitle(R.string.SETTINGS)
                .setMultiChoiceItems(items, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int indexSelected,
                                                boolean isChecked) {

                                SettingsHelper.mColorfulVPid = isChecked;

                            }
                        }).setPositiveButton(R.string.CLOSE_BUTTON,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                enter(VIEW_VPID);
                            }
                        }).create();
        dialog.show();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgVPidDownload: {
                onVerticalPidDownloadClick(v);
            }
            break;
            case R.id.imgVPidUpload: {
                onVerticalPidUploadClick(v);
            }
            break;
            case R.id.imgVPidSave: {
                onVerticalPidSaveClick(v);
            }
            break;
        }

        if (v.getClass().equals(PidTextView.class)) {
            onVerticalPidGridNumberClick(v);
        }
    }

    private void onVerticalPidSaveClick(View v) {
        final MainActivity ma = getMainActivity();
        if (ma.mFcDevice != null && ma.mFcDevice.isConnected()) {
            ma.mFcDevice.savePersistent("AltitudeHoldSettings");
            SingleToast.show(ma, getString(R.string.SAVED_PERSISTENT)
                    + getString(R.string.CHECK_PID_WARNING), Toast.LENGTH_SHORT);
        } else {
            SingleToast.show(ma, R.string.SEND_FAILED, Toast.LENGTH_SHORT);
        }
    }

    private void onVerticalPidUploadClick(View v) {
        final MainActivity ma = getMainActivity();
        if (ma.mFcDevice != null && ma.mFcDevice.isConnected()) {
            UAVTalkObjectTree oTree = ma.mFcDevice.getObjectTree();
            if (oTree != null) {
                oTree.getObjectFromName("AltitudeHoldSettings").setWriteBlocked(true);

                for (PidTextView ptv : mVerticalPidTexts) {
                    switch (ptv.getFieldType()) {
                        case (UAVTalkXMLObject.FIELDTYPE_FLOAT32):
                            try {
                                float f = H.stringToFloat(ptv.getText().toString());

                                byte[] buffer = H.reverse4bytes(H.floatToByteArray(f));

                                UAVTalkDeviceHelper.updateSettingsObject(
                                        oTree, "AltitudeHoldSettings", 0,
                                        ptv.getField(), ptv.getElement(), buffer);
                            } catch (NumberFormatException e) {
                                VisualLog.e("MainActivity",
                                        "Error parsing float (vertical): " + ptv.getField() + " " +
                                                ptv.getElement() + " " + ptv.getText().toString());
                            }
                            break;
                        case (UAVTalkXMLObject.FIELDTYPE_UINT8):
                            try {
                                byte[] buffer = new byte[1];
                                VisualLog.d("SDFG", ptv.getText().toString());
                                buffer[0] =
                                        (byte) (Integer.parseInt(ptv.getText().toString()) & 0xff);
                                UAVTalkDeviceHelper.updateSettingsObject(
                                        oTree, "AltitudeHoldSettings", 0, ptv.getField(),
                                        ptv.getElement(), buffer);
                            } catch (NumberFormatException e) {
                                VisualLog.e("MainActivity",
                                        "Error parsing uint8 (vertical): " + ptv.getField() + " " +
                                                ptv.getElement() + " " + ptv.getText().toString());
                            }
                            break;
                        default:

                            break;
                    }
                }

                ma.mFcDevice.sendSettingsObject("AltitudeHoldSettings", 0);

                SingleToast.show(ma, getString(R.string.PID_SENT)
                        + getString(R.string.CHECK_PID_WARNING), Toast.LENGTH_SHORT);

                oTree.getObjectFromName("AltitudeHoldSettings").setWriteBlocked(false);
            }
        } else {
            SingleToast.show(ma, R.string.SEND_FAILED, Toast.LENGTH_SHORT);
        }
    }

    private void onVerticalPidDownloadClick(View v) {
        final MainActivity ma = getMainActivity();
        if (ma.mFcDevice != null && ma.mFcDevice.isConnected()) {

            for (PidTextView ptv : mVerticalPidTexts) {
                ptv.allowUpdate();
            }

            SingleToast.show(ma, getString(R.string.PID_LOADING)
                    + getString(R.string.CHECK_PID_WARNING), Toast.LENGTH_SHORT);
        } else {
            SingleToast.show(ma, R.string.SEND_FAILED, Toast.LENGTH_SHORT);
        }
    }

    private void onVerticalPidGridNumberClick(View v) {
        final MainActivity ma = getMainActivity();
        PidTextView p = (PidTextView) v;
        new PidInputAlertDialog(ma)
                .withStep(p.getStep())
                .withDenominator(p.getDenom())
                .withDecimalFormat(p.getDfs())
                .withPidTextView(p)
                .withValueMax(p.getMax())
                .withPresetText(p.getText().toString())
                .withTitle(p.getDialogTitle())
                .withLayout(R.layout.alert_dialog_pid_grid)
                .withUavTalkDevice(ma.mFcDevice)
                .withObject("AltitudeHoldSettings")
                .withField(p.getField())
                .withElement(p.getElement())
                .withFieldType(p.getFieldType())
                .show();
    }
}
