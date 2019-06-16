package com.smartpos.mrs2btlib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MagicRs232toBt {
    private static String TAG = "MagicRs232toBt";
    private static UUID SerialBlueToothDev_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;

    private int mState;
    private int mErrorValue = ErrorConstants.ERROR_UNKNOWN;
    private String rs232toBtConnectorAddr = null;

    private BluetoothSocket mBtSocket;
    private BluetoothDevice mBtDevice;
    private InputStream mInStream;
    private OutputStream mOutStream;

    // CommonConstants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing (Disconnected status)
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new MagicRs232toBt instance.
     */
    public MagicRs232toBt() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            Log.i(TAG, "pairedDevices.size()=" + pairedDevices.size());
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "device.getName()=" + device.getName() + ",device.getAddress()=" + device.getAddress());
                if (device.getName().contains(CommonConstants.RS232TOBT_CONNECTOR_PREFIX_NAME)) {
                    rs232toBtConnectorAddr = device.getAddress();
                    mErrorValue = 0;
                    break;
                }
            }

            if(rs232toBtConnectorAddr == null) {
                mErrorValue = ErrorConstants.ERROR_NO_PAIRED_MRS2BT_DEVICE;
            }
        } else {
            mErrorValue = ErrorConstants.ERROR_NO_PAIRED_BT_DEVICE;
        }
    }


    /**
     * Constructor. Prepares a new MagicRs232toBt instance with connectorName.
     */
    public MagicRs232toBt(String connectorName) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            Log.i(TAG, "pairedDevices.size()=" + pairedDevices.size());
            for (BluetoothDevice device : pairedDevices) {
                Log.i(TAG, "device.getName()=" + device.getName() + ",device.getAddress()=" + device.getAddress());
                if (device.getName().equals(connectorName)) {
                    rs232toBtConnectorAddr = device.getAddress();
                    mErrorValue = 0;
                    break;
                }
            }

            if(rs232toBtConnectorAddr == null) {
                mErrorValue = ErrorConstants.ERROR_NO_PAIRED_MRS2BT_DEVICE;
            }
        } else {
            mErrorValue = ErrorConstants.ERROR_NO_PAIRED_BT_DEVICE;
        }
    }

    /**
     * connect to MagicRS2323toBT connector device
     */
    public int connect() {

        Log.i(TAG, "executed connect()");

        if(mErrorValue < 0) {
            Log.e(TAG, "mErrorValue = " + mErrorValue);
            return mErrorValue;
        }

        if(mBluetoothAdapter.isEnabled() == false) {
            Log.e(TAG, "ERROR_BT_DEVICE_NOT_ENABLE " + ErrorConstants.ERROR_BT_DEVICE_NOT_ENABLE);
            return ErrorConstants.ERROR_BT_DEVICE_NOT_ENABLE;
        }

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(rs232toBtConnectorAddr);

        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        BluetoothSocket tmp = null;
        try {
            Log.i(TAG, "connect to SerialBlueToothDev_UUID");
            tmp = device.createRfcommSocketToServiceRecord(SerialBlueToothDev_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: create() failed", e);
            mState = STATE_NONE;
            return ErrorConstants.ERROR_CANNOT_CREATE_RFCOMM_SOCKET;
        }
        mBtSocket = tmp;
        mState = STATE_CONNECTING;

        // Always cancel discovery because it will slow down a connection
        mBluetoothAdapter.cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            mBtSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();

            // Close the socket
            try {
                mBtSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() bt socket during connection failure", e2);
            }

            mState = STATE_NONE;
            return ErrorConstants.ERROR_CANNOT_CONNECT_MRS2BT_DEVICE;
        }

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = mBtSocket.getInputStream();
            tmpOut = mBtSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
            mState = STATE_NONE;
            Log.e(TAG, "ERROR_CANNOT_GET_IO_STREAM " + ErrorConstants.ERROR_CANNOT_GET_IO_STREAM);
            return ErrorConstants.ERROR_CANNOT_GET_IO_STREAM;
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;
        mState = STATE_CONNECTED;

        return 0;
    }


    /**
     * write data to MagicRS2323toBT connector device
     * @param writeData
     */
    public int write(byte []writeData) {
        Log.i(TAG, "executed write()");

        if(writeData == null || writeData.length <= 0) {
            Log.e(TAG, "ERROR_INVALID_WRITE_BUFFER " + ErrorConstants.ERROR_INVALID_WRITE_BUFFER);
            return ErrorConstants.ERROR_INVALID_WRITE_BUFFER;
        }

        if(mState != STATE_CONNECTED || mOutStream == null) {
            Log.e(TAG, "ERROR_INVALID_CONNECT_STATUS " + ErrorConstants.ERROR_INVALID_CONNECT_STATUS);
            return ErrorConstants.ERROR_INVALID_CONNECT_STATUS;
        }

        try {
            mOutStream.write(writeData);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "ERROR_WRITE_DATA_FAIL " + ErrorConstants.ERROR_WRITE_DATA_FAIL);
            mState = STATE_NONE;
            return ErrorConstants.ERROR_WRITE_DATA_FAIL;
        }

        return 0;
    }

    /**
     * read data to MagicRS2323toBT connector device
     * @param readData : this method will read data size no more than length of readData
     */
    public int read(byte []readData) {
        int retVal;

        Log.i(TAG, "executed read() 1 para");

        if(readData == null || readData.length <= 0) {
            Log.e(TAG, "ERROR_INVALID_READ_BUFFER " + ErrorConstants.ERROR_INVALID_READ_BUFFER);
            return ErrorConstants.ERROR_INVALID_READ_BUFFER;
        }

        if(mState != STATE_CONNECTED || mInStream == null) {
            Log.e(TAG, "ERROR_INVALID_CONNECT_STATUS " + ErrorConstants.ERROR_INVALID_CONNECT_STATUS);
            return ErrorConstants.ERROR_INVALID_CONNECT_STATUS;
        }

        Log.i(TAG, "readData.length = " + readData.length);

        int inBufSize = readData.length;
        try {
            retVal = mInStream.read(readData, 0, inBufSize);
        } catch (IOException e) {
            e.printStackTrace();
            mState = STATE_NONE;  //连接中断，应充值连接状态
            return ErrorConstants.ERROR_READ_DATA_FAIL;
        }

        return retVal;
    }

    /**
     * read data to MagicRS2323toBT connector device
     * @param readData : this method will read data size no more than length of readData
     * @param expectLength : and this method will read data size no more than length of expectLength
     */
    public int read(byte []readData, int expectLength) {
        int retVal;

        Log.i(TAG, "executed read() 2 paras");

        if(readData == null || readData.length <= 0) {
            Log.e(TAG, "ERROR_INVALID_READ_BUFFER " + ErrorConstants.ERROR_INVALID_READ_BUFFER);
            return ErrorConstants.ERROR_INVALID_READ_BUFFER;
        }

        if(expectLength > readData.length ) {
            Log.e(TAG, "ERROR_INVALID_READ_LENGTH " + ErrorConstants.ERROR_INVALID_READ_LENGTH);
            return ErrorConstants.ERROR_INVALID_READ_LENGTH;
        }

        if(mState != STATE_CONNECTED || mInStream == null) {
            Log.e(TAG, "ERROR_INVALID_CONNECT_STATUS " + ErrorConstants.ERROR_INVALID_CONNECT_STATUS);
            return ErrorConstants.ERROR_INVALID_CONNECT_STATUS;
        }

        Log.i(TAG, "readData.length = " + readData.length + ", expectLength = " + expectLength);

        int inBufSize = expectLength;
        try {
            retVal = mInStream.read(readData, 0, inBufSize);
        } catch (IOException e) {
            e.printStackTrace();
            mState = STATE_NONE; //连接中断，应充值连接状态
            return ErrorConstants.ERROR_READ_DATA_FAIL;
        }

        return retVal;
    }

    /**
     * read data to MagicRS2323toBT connector device
     * @param readData : this method will read data size no more than length of readData
     * @param offset : this method will read to this offset of the param readData
     * @param expectLength : and this method will read data size no more than length of expectLength
     */
    public int read(byte []readData, int offset, int expectLength) {
        int retVal;

        Log.i(TAG, "executed read() 3 paras");

        if(readData == null || readData.length <= 0) {
            Log.e(TAG, "ERROR_INVALID_READ_BUFFER " + ErrorConstants.ERROR_INVALID_READ_BUFFER);
            return ErrorConstants.ERROR_INVALID_READ_BUFFER;
        }

        if( offset < 0 || offset > readData.length ) {
            Log.e(TAG, "ERROR_INVALID_READ_OFFSET " + ErrorConstants.ERROR_INVALID_READ_OFFSET);
            return ErrorConstants.ERROR_INVALID_READ_OFFSET;
        }

        if(expectLength > (readData.length - offset) )  {
            Log.e(TAG, "ERROR_INVALID_READ_LENGTH " + ErrorConstants.ERROR_INVALID_READ_LENGTH);
            return ErrorConstants.ERROR_INVALID_READ_LENGTH;
        }

        if(mState != STATE_CONNECTED || mInStream == null) {
            Log.e(TAG, "ERROR_INVALID_CONNECT_STATUS " + ErrorConstants.ERROR_INVALID_CONNECT_STATUS);
            return ErrorConstants.ERROR_INVALID_CONNECT_STATUS;
        }

        Log.i(TAG, "readData.length = " + readData.length + ", expectLength = " + expectLength);

        int inBufSize = expectLength;
        try {
            retVal = mInStream.read(readData, offset, inBufSize);
        } catch (IOException e) {
            e.printStackTrace();
            mState = STATE_NONE; //连接中断，应充值连接状态
            return ErrorConstants.ERROR_READ_DATA_FAIL;
        }

        return retVal;
    }

    public int getCommState() {
        return mState;
    }

    public int getLastErrorValue() {
        return mErrorValue;
    }

    /**
     * close socket to MagicRS2323toBT connector device
     */
    public int close() {
        Log.i(TAG, "executed close()");
        try {
            mBtSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connect socket failed", e);
            return ErrorConstants.ERROR_CLOSE_BT_SOCKET_FAIL;
        }

        mState = STATE_NONE;
        mInStream = null;
        mOutStream = null;
        return 0;
    }

    public int outputBytesHEXandASC(String header, byte[] data, int bytesPerLine) {
        int p, end;
        String prnBuf, tmpHexStr, tmpASCStr;
        int Cycle, j, col, len;
//        int bytesPerLine = 16;

        len = data.length;

        /* 计算该批数据占用的行数 */
        if (len % bytesPerLine == 0) {
            col = len / bytesPerLine;
        } else {
            col = (len / bytesPerLine) + 1;
        }

        /* 在标题中加上行数 */
        prnBuf = header + "(" + col + " columns, " + len + "bytes)";

        /* 打印数据的标题 */
        if (header != null) {
            Log.i(TAG, prnBuf);
        }

        p = 0;
        end = len;

        while (true) {
            /* 处理待打印数据不是bytesPerLine的整数倍的情况 */
            if ((p + bytesPerLine) > end) {
                Cycle = len % bytesPerLine;
            } else {
                Cycle = bytesPerLine;
            }

            prnBuf = "";
            tmpASCStr = "";
            tmpHexStr = "";
            for (j = 0; j < Cycle; j++) {
                //中间多加一个空格
                if (j % 4 == 0) {
                    tmpHexStr += " ";
                }

                //中间再多加一个空格
                if (j % 8 == 0) {
                    tmpHexStr += " ";
                }

                tmpHexStr += String.format("%02X ", data[p + j]);

                /* 判断是否可打印字符 */
                if (((data[p + j] & 0x80) == 0) && ((data[p + j] & 0x60) != 0)) {
                    tmpASCStr += String.format("%c", data[p + j]);
                } else {
                    tmpASCStr += ".";
                }
            }

            if (bytesPerLine == 8)
                prnBuf = String.format("%-27s", tmpHexStr) + String.format("%8s", tmpASCStr);
            else if (bytesPerLine == 16)
                prnBuf = String.format("%-56s", tmpHexStr) + String.format("%16s", tmpASCStr);

            Log.i(TAG, prnBuf);

            p += bytesPerLine;

            /* 已经到达或超过待打印数据的末端,退出循环 */
            if (p >= end) {
                break;
            }
        }

        return len;
    }
}
