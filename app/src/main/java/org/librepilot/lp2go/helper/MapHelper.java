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

package org.librepilot.lp2go.helper;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.librepilot.lp2go.H;
import org.librepilot.lp2go.MainActivity;

import java.util.ArrayDeque;
import java.util.Deque;

public class MapHelper implements OnMapReadyCallback {
    private final int MAX_LINES = 20;
    private MainActivity mActivity;
    private GoogleMap mMap;
    private Deque<Polyline> mMapLines;
    private Marker mPosMarker;

    public MapHelper(MainActivity activity) {
        this.mActivity = activity;
        mMapLines = new ArrayDeque<>();
    }

    public void updatePosition(LatLng s, LatLng e, float deg) {
        if (mMap != null) {
            PolylineOptions line = new PolylineOptions()
                    .add(s)
                    .add(e)
                    .color(Color.RED);
            Polyline pl = mMap.addPolyline(line);
            mMapLines.addFirst(pl);
            if (mMapLines.size() > MAX_LINES) {
                Polyline del = mMapLines.removeLast();
                del.remove();
            }

            if (mPosMarker != null) {
                mPosMarker.remove();
            }
            mPosMarker = mMap.addMarker(new MarkerOptions()
                    .position(e)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .rotation(deg)
            );

            double distance = H.calculationByDistance(s, e);
            //if (distance > 0.001) {
                CameraUpdate cameraUpdate =
                        CameraUpdateFactory.newLatLngZoom(e, 19);

                if (distance < 200) {
                    mMap.animateCamera(cameraUpdate);
                } else {
                    mMap.moveCamera(cameraUpdate);
                }
            //}
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap != null) {
            mMap = googleMap;
        }

        //Map can be null if services are not available, e.g. on an amazon fire tab
        if (mMap != null) {
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.setMyLocationEnabled(true);
            MapsInitializer.initialize(mActivity);

            LocationManager lm =
                    (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
            Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            LatLng currentDevicePos;
            if (l != null) {
                currentDevicePos = new LatLng(l.getLatitude(), l.getLongitude());
            } else {
                currentDevicePos = new LatLng(.0f, .0f);
            }

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentDevicePos, 19);
            mMap.moveCamera(cameraUpdate);
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }

    public void clear() {
        if (mMap != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMap.clear();
                }
            });
        }
    }
}
