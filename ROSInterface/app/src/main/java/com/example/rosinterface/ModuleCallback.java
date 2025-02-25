package com.example.rosinterface;

import android.os.RemoteException;

import com.ainirobot.coreservice.client.module.ModuleCallbackApi;

public class ModuleCallback extends ModuleCallbackApi {
    @Override
    public boolean onSendRequest(int reqId, String reqType, String reqText, String reqParam)    throws RemoteException {
        //receive voice command,
        //reqTyp : voice command type
        //reqText : voice to text
        //reqParam : voice command parameter
        return true;
    }
    @Override
    public void onRecovery()    throws RemoteException {
        //When receiving the event, regain control of the robot
    }
    @Override
    public void onSuspend()    throws RemoteException {
        //Control is deprived by the system. When receiving this event, all Api calls are invalid
    }
}