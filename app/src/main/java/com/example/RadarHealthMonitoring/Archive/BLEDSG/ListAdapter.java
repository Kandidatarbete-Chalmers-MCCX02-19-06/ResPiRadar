package com.example.RadarHealthMonitoring.Archive.BLEDSG;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> ble_devices;
    public ListAdapter() {
        super();
        ble_devices = new ArrayList<BluetoothDevice>();
    }
    public void addDevice(BluetoothDevice device) {
        if (!ble_devices.contains(device)) {
            ble_devices.add(device);
        }
    }
    public boolean contains(BluetoothDevice device) {
        return ble_devices.contains(device);
    }
    public BluetoothDevice getDevice(int position) {
        return ble_devices.get(position);
    }
    public void clear() {
        ble_devices.clear();
    }
    @Override
    public int getCount() {
        return ble_devices.size();
    }
    @Override
    public Object getItem(int i) {
        return ble_devices.get(i);
    }
    @Override
    public long getItemId(int i) {
        return i;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        //ViewHolder viewHolder;
        if (view == null) {
         //   view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
         //   viewHolder = new ViewHolder();
         //   viewHolder.text = (TextView) view.findViewById(R.id.textView);
         //   viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
         //   view.setTag(viewHolder);
        } else {
        //    viewHolder = (ViewHolder) view.getTag();
        }
        BluetoothDevice device = ble_devices.get(i);
        String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0) {
        //    viewHolder.text.setText(deviceName);
        } else {
        //    viewHolder.text.setText("unknown device");
        }
        //viewHolder.bdaddr.setText(device.getAddress());
        return view;
    }
}
