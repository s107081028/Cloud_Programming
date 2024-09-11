package com.example.cloudprog.viewmodels;

import android.content.Context;

import com.example.cloudprog.Service.AWSService;

public class Injection {
    private static AWSService awsService = null;

    public static synchronized AWSService getAWSService() {
        return awsService;
    }

    public static synchronized void initialize(Context context) {
        if (awsService == null) {
            awsService = new AWSService(context);
        }
    }
}
