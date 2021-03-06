/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/* This file incorporates work covered by the following copyright and
 * permission notice:
 */

/*
 * Copyright (C) 2011 The Android Open Source Project
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
package org.librepilot.lp2go;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.librepilot.lp2go.controller.ViewController;
import org.librepilot.lp2go.controller.ViewControllerAbout;
import org.librepilot.lp2go.controller.ViewControllerDebug;
import org.librepilot.lp2go.controller.ViewControllerLogs;
import org.librepilot.lp2go.controller.ViewControllerMain;
import org.librepilot.lp2go.controller.ViewControllerMainAnimatorViewSetter;
import org.librepilot.lp2go.controller.ViewControllerMap;
import org.librepilot.lp2go.controller.ViewControllerObjects;
import org.librepilot.lp2go.controller.ViewControllerPid;
import org.librepilot.lp2go.controller.ViewControllerScope;
import org.librepilot.lp2go.controller.ViewControllerSettings;
import org.librepilot.lp2go.controller.ViewControllerVerticalPid;
import org.librepilot.lp2go.helper.SettingsHelper;
import org.librepilot.lp2go.helper.TextToSpeechHelper;
import org.librepilot.lp2go.menu.MenuItem;
import org.librepilot.lp2go.menu.MenuListAdapter;
import org.librepilot.lp2go.uavtalk.UAVTalkObjectTree;
import org.librepilot.lp2go.uavtalk.UAVTalkXMLObject;
import org.librepilot.lp2go.uavtalk.device.FcBluetoothDevice;
import org.librepilot.lp2go.uavtalk.device.FcDevice;
import org.librepilot.lp2go.uavtalk.device.FcUsbDevice;
import org.librepilot.lp2go.ui.SingleToast;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {
    public static final int CALLBACK_FILEPICKER = 3456;
    public static final int CALLBACK_TTS = 6574;
    public static final int POLL_WAIT_TIME = 500;
    public static final int POLL_SECOND_FACTOR = 1000 / POLL_WAIT_TIME;
    public static final int SERIAL_BLUETOOTH = 2;
    public static final int SERIAL_NONE = 0;
    protected static final int SERIAL_USB = 1;
    private static final int NUM_OF_VIEWS = 10;
    private static final String UAVO_INTERNAL_PATH = "uavo";
    static int mCurrentView = 0;
    private static boolean mHasPThread = false;
    public ImageView imgSerial;
    public ImageView imgToolbarFlightSettings;
    public ImageView imgToolbarLocalSettings;
    public ImageView imgUavoSanity;
    public BluetoothAdapter mBluetoothAdapter;
    public boolean mDoReconnect = false;
    public FcDevice mFcDevice;
    public PollThread mPollThread = null;
    public ArrayList<ViewController> mVcList;
    public Map<Integer, View> mViews;
    public Map<String, UAVTalkXMLObject> mXmlObjects = null;
    public MenuItem menDebug = null;
    ImageView imgFlightTelemetry;
    ImageView imgGroundTelemetry;
    private ConnectionThread mConnectionThread = null;
    private android.hardware.usb.UsbDevice mDevice;
    private UsbDeviceConnection mDeviceConnection;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private UsbInterface mInterface;
    private PendingIntent mPermissionIntent = null;
    private long mRxObjectsBad;
    private long mRxObjectsGood;
    private CharSequence mTitle;
    private TextToSpeechHelper mTtsHelper;
    private long mTxObjects;
    private String mUavoLongHash;
    private String mUavoLongHashFc;
    private UsbManager mUsbManager = null;
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            VisualLog.d(getString(R.string.USB), action);

            if (SettingsHelper.mSerialModeUsed == SERIAL_USB &&
                    UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                android.hardware.usb.UsbDevice device =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                VisualLog.d(getString(R.string.USB), device.getVendorId() +
                        getString(R.string.DASH) +
                        device.getProductId() +
                        getString(R.string.DASH) +
                        device.getDeviceClass() +
                        getString(R.string.SPACE) +
                        device.getDeviceSubclass() +
                        getString(R.string.SPACE) +
                        device.getDeviceProtocol());

                if (device.getDeviceClass() == UsbConstants.USB_CLASS_MISC) {
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }

            } else if (SettingsHelper.mSerialModeUsed == SERIAL_USB
                    && UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                android.hardware.usb.UsbDevice device =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    setUsbInterface(null, null);
                    if (mFcDevice != null) {
                        mFcDevice.stop();
                    }
                }
            } else if (SettingsHelper.mSerialModeUsed == SERIAL_USB &&
                    getString(R.string.ACTION_USB_PERMISSION).equals(action)) {
                synchronized (this) {
                    android.hardware.usb.UsbDevice device =
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbInterface intf = findAdbInterface(device);
                            if (intf != null) {
                                setUsbInterface(device, intf);
                            }
                        }
                    } else {
                        VisualLog.d("DBG", "permission denied for device " + mDevice);
                    }
                }
            }
        }
    };

    public static boolean hasPThread() {
        return mHasPThread;
    }

    static private UsbInterface findAdbInterface(android.hardware.usb.UsbDevice device) {

        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 3
                    && intf.getInterfaceSubclass() == 0
                    && intf.getInterfaceProtocol() == 0) {
                return intf;
            }
        }
        return null;
    }

    public FcDevice getFcDevice() {
        return mFcDevice;
    }

    public long getRxObjectsBad() {
        return mRxObjectsBad;
    }

    public synchronized void setRxObjectsBad(long o) {
        mRxObjectsBad = o;
    }

    public long getRxObjectsGood() {
        return mRxObjectsGood;
    }

    public synchronized void setRxObjectsGood(long o) {
        mRxObjectsGood = o;
    }

    public TextToSpeechHelper getTtsHelper() {
        return mTtsHelper;
    }

    public long getTxObjects() {
        return mTxObjects;
    }

    public synchronized void setTxObjects(long o) {
        mTxObjects = o;
    }

    public String getUavoLongHash() {
        return mUavoLongHash;
    }

    public static void setPThread(boolean mHasPThread) {
        MainActivity.mHasPThread = mHasPThread;
    }

    public void setPollThreadObjectTree(UAVTalkObjectTree oTree) {
        mPollThread.setObjectTree(oTree);
    }

    public void setUavoLongHashFC(String uavolonghashfc) {
        mUavoLongHashFc = uavolonghashfc;
    }

    public synchronized void incRxObjectsGood() {
        mRxObjectsGood++;
    }

    public synchronized void incRxObjectsBad() {
        mRxObjectsBad++;
    }

    public synchronized void incTxObjects() {
        mTxObjects++;
    }

    public void copyAssets() {

        VisualLog.d("STARTING", "CopyAssets");
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list(UAVO_INTERNAL_PATH);
        } catch (IOException e) {
            VisualLog.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null) {
            for (String filename : files) {
                try {
                    copyFile(assetManager.open(UAVO_INTERNAL_PATH +
                            File.separator + filename), filename);
                } catch (IOException e) {
                    VisualLog.e("tag", "Failed to copy asset file: " + filename, e);
                }
            }
        }
    }

    public void copyFile(InputStream source, String relativeFilename) throws IOException {

        VisualLog.d("COPY", "Copy " + relativeFilename);

        FileOutputStream out = openFileOutput(UAVO_INTERNAL_PATH +
                getString(R.string.DASH) + relativeFilename, Context.MODE_PRIVATE);

        copyFile(source, out);
        source.close();
        out.flush();
        out.close();
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void initSlider() {
        mTitle = mDrawerTitle = getTitle();
        TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);
        ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();

        menuItems.add(new MenuItem(getString(R.string.menu_main),
                R.drawable.ic_notifications_on_24dp));
        menuItems.add(new MenuItem(getString(R.string.menu_map),
                R.drawable.ic_public_24dp));
        menuItems.add(new MenuItem(getString(R.string.menu_objects),
                R.drawable.ic_now_widgets_24dp));
        menuItems.add(new MenuItem(getString(R.string.menu_pid),
                R.drawable.ic_tune_128dp));
        menuItems.add(new MenuItem(getString(R.string.menu_vpid),
                R.drawable.ic_vertical_align_center_black_128dp));
        menuItems.add(new MenuItem(getString(R.string.menu_logs),
                R.drawable.ic_rate_review_24dp));
        menuItems.add(new MenuItem(getString(R.string.menu_settings),
                R.drawable.ic_settings_24dp));
        menuItems.add(new MenuItem(getString(R.string.menu_about),
                R.drawable.ic_info_outline_24dp));
        if (menDebug != null) {
            menuItems.add(menDebug);
        }

        navMenuIcons.recycle();
        MenuListAdapter drawListAdapter = new MenuListAdapter(getApplicationContext(),
                menuItems);
        mDrawerList.setAdapter(drawListAdapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setLogo(R.drawable.ic_librepilot_logo_toolbar_48dp);
            toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp);
        }
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                new Toolbar(this), R.string.APP_NAME, R.string.APP_NAME) {

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());
    }

    public void reconnect() {
        mDoReconnect = true;
    }

    @Override
    public void onRestart() {
        super.onRestart();

        SettingsHelper.loadSettings(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        ActionBar ab = this.getSupportActionBar();
        if (ab != null) {
            ab.setTitle(mTitle);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VisualLog.setActivity(this);

        mViews = new HashMap<>(NUM_OF_VIEWS);
        ViewController mVcDebug =
                new ViewControllerDebug(this, R.string.menu_debug, View.INVISIBLE, View.INVISIBLE);

        copyAssets();
        SettingsHelper.loadSettings(this);

        mTtsHelper = new TextToSpeechHelper(this);

        //debug view is initialized above
        ViewController mVcPid =
                new ViewControllerPid(this, R.string.menu_pid, View.VISIBLE, View.INVISIBLE);
        ViewController mVcVPid =
                new ViewControllerVerticalPid(this, R.string.menu_vpid, View.VISIBLE,
                        View.INVISIBLE);
        ViewController mVcScope =
                new ViewControllerScope(this, R.string.menu_scope, View.INVISIBLE, View.INVISIBLE);
        ViewController mVcAbout =
                new ViewControllerAbout(this, R.string.menu_about, View.INVISIBLE, View.INVISIBLE);
        ViewController mVcLogs =
                new ViewControllerLogs(this, R.string.menu_logs, View.INVISIBLE, View.INVISIBLE);
        ViewController mVcSettings =
                new ViewControllerSettings(this, R.string.menu_settings, View.INVISIBLE,
                        View.INVISIBLE);
        ViewController mVcMap =
                new ViewControllerMap(this, R.string.menu_map, View.INVISIBLE, View.INVISIBLE);
        ViewController mVcObjects =
                new ViewControllerObjects(this, R.string.menu_objects, View.INVISIBLE,
                        View.INVISIBLE);
        ViewController mVcMain =
                new ViewControllerMain(this, R.string.menu_main, View.VISIBLE, View.VISIBLE);

        mVcList = new ArrayList<ViewController>();
        mVcList.add(ViewController.VIEW_MAIN, mVcMain);
        mVcList.add(ViewController.VIEW_MAP, mVcMap);
        mVcList.add(ViewController.VIEW_OBJECTS, mVcObjects);
        mVcList.add(ViewController.VIEW_PID, mVcPid);
        mVcList.add(ViewController.VIEW_VPID, mVcVPid);
        mVcList.add(ViewController.VIEW_LOGS, mVcLogs);
        mVcList.add(ViewController.VIEW_SETTINGS, mVcSettings);
        mVcList.add(ViewController.VIEW_ABOUT, mVcAbout);
        mVcList.add(ViewController.VIEW_DEBUG, mVcDebug);
        mVcList.add(ViewController.VIEW_SCOPE, mVcScope);

        ((ViewControllerMainAnimatorViewSetter) mVcMain).setLayout();

        initSlider();

        initWarnDialog().show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        mVcList.get(mCurrentView).init();
        displayView(mCurrentView);
        initSlider();
    }

    @Override
    protected void onStop() {

        if (mFcDevice != null) {
            mFcDevice.setLogging(false);
        }

        if (mPollThread != null) {
            mPollThread.setInvalid();
            mPollThread = null;
        }

        if (mConnectionThread != null) {
            mConnectionThread.setInvalid();
            mConnectionThread = null;
            MainActivity.mHasPThread = false;
        }

        SettingsHelper.mSerialModeUsed = SERIAL_NONE;
        mDoReconnect = true;

        if (mFcDevice != null) {
            mFcDevice.stop();
        }
        mFcDevice = null;

        unregisterReceiver(mUsbReceiver);
        setUsbInterface(null, null);
        mPermissionIntent = null;

        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean loadXmlObjects(boolean overwrite) {

        if (mXmlObjects == null || (overwrite && SettingsHelper.mLoadedUavo != null)) {
            mXmlObjects = new TreeMap<String, UAVTalkXMLObject>();

            String file = SettingsHelper.mLoadedUavo + getString(R.string.UAVO_FILE_EXTENSION);
            ZipInputStream zis = null;
            MessageDigest crypt;
            MessageDigest cumucrypt;
            try {
                InputStream is =
                        openFileInput(UAVO_INTERNAL_PATH + getString(R.string.DASH) + file);
                zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;

                //we need to sort the files to generate the correct hash
                SortedMap<String, String> files = new TreeMap<String, String>();

                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.getName().endsWith("xml")) {

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, count);
                        }

                        String xml = baos.toString();
                        files.put(ze.getName(), xml);

                        if (xml.length() > 0) {
                            UAVTalkXMLObject obj = new UAVTalkXMLObject(xml);
                            mXmlObjects.put(obj.getName(), obj);
                        }
                    }
                }

                crypt = MessageDigest.getInstance("SHA-1");     //single files hash
                cumucrypt = MessageDigest.getInstance("SHA-1"); //cumulative hash
                cumucrypt.reset();
                for (String xmle : files.values()) {            //cycle over the sorted files
                    crypt.reset();
                    crypt.update(xmle.getBytes());              //hash the file
                    //update a hash over the file hash string representations (yes.)
                    cumucrypt.update(H.bytesToHex(crypt.digest()).toLowerCase().getBytes()); //sic!
                }

                mUavoLongHash = H.bytesToHex(cumucrypt.digest()).toLowerCase();
                VisualLog.d("SHA1", H.bytesToHex(cumucrypt.digest()).toLowerCase());

            } catch (IOException | SAXException
                    | ParserConfigurationException | NoSuchAlgorithmException e) {
                VisualLog.e("UAVO", "UAVO Load Error", e);
            } finally {
                try {
                    if (zis != null) {
                        zis.close();
                    }
                } catch (IOException e) {
                    VisualLog.e("LoadXML", "Exception on Close");
                }
            }
            mDoReconnect = true;

            return true;
        }
        return false;
    }

    protected void connectUSB() {
        if (SettingsHelper.mSerialModeUsed == SERIAL_USB) {
            for (android.hardware.usb.UsbDevice device : mUsbManager.getDeviceList().values()) {
                if (device.getDeviceClass() == UsbConstants.USB_CLASS_MISC) {
                    try {
                        mUsbManager.requestPermission(device, mPermissionIntent);
                    } catch (SecurityException e) {
                        SingleToast.show(this,
                                "USB Security Error. Please try again." + e.getMessage(),
                                Toast.LENGTH_LONG);
                    }
                }
            }
        }
    }

    protected void connectBluetooth() {
        if (SettingsHelper.mSerialModeUsed == SERIAL_BLUETOOTH) {
            setBluetoothInterface();
        }
    }

    public void setContentView(View v, int p) {
        if (mCurrentView != p) {
            mCurrentView = p;
            super.setContentView(v);
            initSlider();
        }
    }

    public void displayView(int position) {
        //clean up current view
        mVcList.get(mCurrentView).leave();
        mVcList.get(position).enter(position);

        mDrawerList.setItemChecked(position, true);
        mDrawerList.setSelection(position);

        imgSerial = (ImageView) findViewById(R.id.imgSerial);
        imgUavoSanity = (ImageView) findViewById(R.id.imgUavoSanity);

        imgGroundTelemetry = (ImageView) findViewById(R.id.imgGroundTelemetry);
        imgFlightTelemetry = (ImageView) findViewById(R.id.imgFlightTelemetry);

        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mTtsHelper.onActivityResult(requestCode, resultCode, data);

        mVcList.get(mCurrentView).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewController.VIEW_MAIN) {
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_warning_black_24dp)
                    .setMessage(R.string.ARE_YOU_SURE_YOU_WANT_TO_CLOSE)
                    .setTitle(R.string.EXIT)
                    .setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.NO, null)
                    .show();
        } else {
            displayView(ViewController.VIEW_MAIN);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mPermissionIntent == null) {
            mPermissionIntent =
                    PendingIntent.getBroadcast(this, 0,
                            new Intent(getString(R.string.ACTION_USB_PERMISSION)), 0);
        }

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // listen for new usb devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(getString(R.string.ACTION_USB_PERMISSION));

        registerReceiver(mUsbReceiver, filter);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        if (mPollThread == null) {
            mPollThread = new PollThread(this);
            mPollThread.start();
        }
        if (mConnectionThread == null) {
            mConnectionThread = new ConnectionThread(this);
            mConnectionThread.start();
        }

        mDoReconnect = true;

        displayView(mCurrentView);

        VisualLog.d("onStart", "onStart");
    }

    public void onToolbarFlightSettingsClick(View v) {
        mVcList.get(mCurrentView).onToolbarFlightSettingsClick(v);
    }

    public void onToolbarLocalSettingsClick(View v) {
        mVcList.get(mCurrentView).onToolbarLocalSettingsClick(v);
    }

    private boolean setBluetoothInterface() {
        if (mFcDevice != null) {
            mFcDevice.stop();
        }
        mFcDevice = null;
        mFcDevice = new FcBluetoothDevice(this, mXmlObjects);
        mFcDevice.start();

        return mFcDevice != null;
    }

    private boolean setUsbInterface(android.hardware.usb.UsbDevice device, UsbInterface intf) {
        if (mDeviceConnection != null) {
            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
        }

        if (device != null && intf != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null) {
                if (connection.claimInterface(intf, true)) {
                    mDevice = device;
                    mDeviceConnection = connection;
                    mInterface = intf;
                    mFcDevice = new FcUsbDevice(this, mDeviceConnection, intf,
                            mXmlObjects);
                    mFcDevice.getObjectTree().setXmlObjects(mXmlObjects);

                    mFcDevice.start();
                    return true;
                } else {
                    connection.close();
                }
            }
        }

        if (mDeviceConnection == null && mFcDevice != null) {
            mFcDevice.stop();
            mFcDevice = null;
        }
        return false;
    }

    private AlertDialog.Builder initWarnDialog() {
        AlertDialog.Builder warnDialogBuilder = new AlertDialog.Builder(this);
        warnDialogBuilder.setTitle(R.string.WARNING);

        warnDialogBuilder.setCancelable(false);

        final TextView info = new TextView(this);
        warnDialogBuilder.setView(info);
        info.setText(R.string.GNU_WARNING);
        info.setPadding(5, 5, 5, 5);

        warnDialogBuilder
                .setPositiveButton(R.string.I_UNDERSTAND, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialog.cancel();
                    }
                });
        return warnDialogBuilder;
    }

    public void onObjectsSanityIndicatorClick(View v) {
        showObjectSanityWarningMessage();
    }

    void showObjectSanityWarningMessage() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        String uavoShortHash = mUavoLongHash != null && mUavoLongHash.length() > 8
                ? mUavoLongHash.substring(0, 8) : getString(R.string.NULL);
        String uavoShortHashFc = mUavoLongHashFc != null && mUavoLongHashFc.length() > 8
                ? mUavoLongHashFc.substring(0, 8) : getString(R.string.NULL);

        if (uavoShortHashFc.equals(getString(R.string.NULL))) {
            dialogBuilder.setTitle(R.string.INFO);
            dialogBuilder.setMessage(
                    getString(R.string.NOT_CONNECTED) + getString(R.string.LF) +
                            getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION_FC) + getString(R.string.TAB) +
                            getString(R.string.TAB) + getString(R.string.TAB) +
                            getString(R.string.TAB) + uavoShortHashFc + getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION) + getString(R.string.TAB) +
                            uavoShortHash
            );
        } else if (uavoShortHashFc.equals(uavoShortHash)) {
            dialogBuilder.setTitle(R.string.INFO);
            dialogBuilder.setMessage(
                    getString(R.string.UAVO_VERSION_OK) + getString(R.string.LF) +
                            getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION_FC) + getString(R.string.TAB) +
                            getString(R.string.TAB) + getString(R.string.TAB) +
                            getString(R.string.TAB) + uavoShortHashFc + getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION) + getString(R.string.TAB) +
                            uavoShortHash
            );
        } else if (uavoShortHashFc.equals(getString(R.string.BAD_FC_UAVO_VERSION))) {
            dialogBuilder.setTitle(R.string.WARNING);
            dialogBuilder.setMessage(
                    getString(R.string.UAVO_MAYBE_TOO_FAST) + getString(R.string.LF) +
                            getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION_FC) + getString(R.string.TAB) +
                            getString(R.string.TAB) + getString(R.string.TAB) +
                            getString(R.string.TAB) + uavoShortHashFc + getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION) + getString(R.string.TAB) +
                            uavoShortHash
            );
        } else {
            dialogBuilder.setTitle(R.string.WARNING);
            dialogBuilder.setMessage(
                    getString(R.string.UAVO_WARNING_A) + getString(R.string.LF) +
                            getString(R.string.UAVO_WARNING_B) + getString(R.string.LF) +
                            getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION_FC) + getString(R.string.TAB) +
                            getString(R.string.TAB) + getString(R.string.TAB) +
                            getString(R.string.TAB) + uavoShortHashFc + getString(R.string.LF) +
                            getString(R.string.UAVO_VERSION) + getString(R.string.TAB) +
                            uavoShortHash
            );
        }

        dialogBuilder.setPositiveButton(R.string.OK_BUTTON,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialogBuilder.show();
    }

    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            displayView(position);
        }
    }
}
