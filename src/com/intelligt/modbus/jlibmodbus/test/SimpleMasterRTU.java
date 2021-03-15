package com.intelligt.modbus.jlibmodbus.test;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.serial.SerialParameters;
import com.intelligt.modbus.jlibmodbus.serial.SerialPort;
import gnu.io.CommPort;
import jssc.SerialPortList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/*
 * Copyright (C) 2016 "Invertor" Factory", JSC
 * All rights reserved
 *
 * This file is part of JLibModbus.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Authors: Vladislav Y. Kochedykov, software engineer.
 * email: vladislav.kochedykov@gmail.com
 */
public class SimpleMasterRTU {

    public static void main(String[] argv) {
        float[] scaler = {1000,1000,1000,1000,100,100,100,100,100,100,100,100f,100f,
                100f,1000,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,0.1f,1000,1000,
                1000,1,1,1,1,1,1,1,1,1000,1000,1000,0.1f,1,0.1f,1,0.1f,100,1f,1f};
        int[] sign = {1,1,1,1,1,1,1,1,1,1,1,-1,-1,1,-1,-1,-1,-1,-1,-1,-1,1,1,1,-1,-1,-1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
        String dir = "C:/Meters";
        String CommPort = "COM5";
        int StartAddress = 11;
        int EndAddress = 26;

        SerialParameters sp = new SerialParameters();
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
        try {
            // you can use just string to get connection with remote slave,
            // but you can also get a list of all serial ports available at your system.
            String[] dev_list = SerialPortList.getPortNames();
            // if there is at least one serial port at your system
            if (dev_list.length > 0) {
                // you can choose the one of those you need
                sp.setDevice(CommPort);//dev_list[2]);
                // these parameters are set by default
                sp.setBaudRate(SerialPort.BaudRate.BAUD_RATE_9600);
                sp.setDataBits(8);
                sp.setParity(SerialPort.Parity.EVEN);
                sp.setStopBits(1);
                //you can choose the library to use.
                //the library uses jssc by default.
                //
                //first, you should set the factory that will be used by library to create an instance of SerialPort.
                //SerialUtils.setSerialPortFactory(new SerialPortFactoryRXTX());
                //  JSSC is Java Simple Serial Connector
                //SerialUtils.setSerialPortFactory(new SerialPortFactoryJSSC());
                //  PJC is PureJavaComm.
                //SerialUtils.setSerialPortFactory(new SerialPortFactoryPJC());
                //  JavaComm is the Java Communications API (also known as javax.comm)
                //SerialUtils.setSerialPortFactory(new SerialPortFactoryJavaComm());
                //in case of using serial-to-wifi adapter
                //String ip = "192.168.0.180";//for instance
                //int port  = 777;
                //SerialUtils.setSerialPortFactory(new SerialPortFactoryTcp(new TcpParameters(InetAddress.getByName(ip), port, true)));
                // you should use another method:
                //next you just create your master and use it.
                ModbusMaster m = ModbusMasterFactory.createModbusMasterRTU(sp);
                m.connect();

                int offset = 768;
                int quantity = 92;
                //you can invoke #connect method manually, otherwise it'll be invoked automatically
                try {
                    for (int x = StartAddress; x <= EndAddress; x++) {
                        int slaveId = x;
                        int[] registerValues = {0};
                        // at next string we receive ten registers from a slave with id of 1 at offset of 0.

                        // Added for catch comm timout
                        try {
                            registerValues = m.readHoldingRegisters(slaveId, offset, quantity);
                        } catch (Exception e){
                            m.connect();
                        }
                        // print values
                        if (registerValues.length > 1) {
                            String readings = "";

                            for (int i = 0; i < registerValues.length; i = i+2) {
                                double reading = registerValues[i] * 65536 + registerValues[i + 1] ;
                                if (sign[i/2]== -1){// i]== -1){ //////////////////////////////////////////////
                                    if (reading > 65536){
                                        reading = reading -4294967295d;
                                    }
                                }
                                readings = readings + (float)reading/scaler[i/2];
//                                i++;
//                                readings = readings + registerValues[i];
                                if (i < registerValues.length - 1) readings = readings + ",";
                                //Get the file reference
                            }
                            readings = readings + "\n";
                            saveData(readings, dir, x);
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        m.disconnect();
                    } catch (ModbusIOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            } catch(RuntimeException e){
                throw e;
            } catch(Exception e){
                e.printStackTrace();
            }
        }

    public enum ByteOrder {ABCD,CDAB,BADC,DCBA}

    public static void saveData(String data, String dir, int address){
        Path path = Paths.get(dir + "/A"+ address+"Readings" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");
        //Use try-with-resource to get auto-closeable writer instance
        if (!Files.exists(path))
        {
            try {
                Files.write(path, "Time,I1 A,I2 A,I3 A,IN A,U12 V,U23 V,U31 V,V1,V2,V3,Hz,P kW,Q Kvar,S kAV,PF,P1 W,P2 W,P3 W,Q1 var,Q2 var,Q3 var,S1 VA,S2 VA,S3 VA,PF1,PF2,PF3,Reserved,Reserved,Reserved,Reserved,Reserved,Reserved,Reserved,Reserved,Max I1 A,Max I2 A,Max I3 A,Max P W,Reserved,Max Q var,Reserved,Max S VA,Hour Meter,E kWh,RE kvarh \n".getBytes());
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }

        }
        try {
            String dataOut  = LocalTime.now() + "," + data;
            Files.write(path, dataOut.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            //exception handling left as an exercise for the reader
        }



    }

    public static float hex32bitToIEEE754(String hex, ByteOrder byteOrder){

        if (hex.length() != 8) return 0.0f;
        try {
            String hexSort = hex;
            if (byteOrder != ByteOrder.ABCD){
                String A = hex.substring(0,2);
                String B = hex.substring(2,4);
                String C = hex.substring(4,6);
                String D = hex.substring(6,8);
                if (byteOrder == ByteOrder.BADC){
                    hexSort = B+A+D+C;
                }
                if (byteOrder == ByteOrder.CDAB){
                    hexSort = C+D+A+B;
                }
                if (byteOrder == ByteOrder.DCBA){
                    hexSort = D+C+B+A;
                }

            }
            int ieee754Int = Integer.parseInt(hexSort, 16);
            float realValue = Float.intBitsToFloat(ieee754Int);
            return realValue;

        } catch (NumberFormatException e){
            return 0.0f;
        }
    }
    public static double DariasMeter(int first, int second){
        return 0.0;
    }
}
