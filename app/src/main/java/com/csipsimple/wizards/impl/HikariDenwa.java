package com.csipsimple.wizards.impl;

import com.csipsimple.api.SipProfile;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class HikariDenwa extends Expert {

    private static class AccountBalanceList extends AccountBalanceHelper {
        WeakReference<HikariDenwa> w;

        AccountBalanceList(HikariDenwa wizard){
            w = new WeakReference<HikariDenwa>(wizard);
        }
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {
            String requestURL = "http://ntt.setup/cas_tel_list/";
            HttpGet req = new HttpGet(requestURL);
            return req;
        }

        @Override
        public String parseResponseLine(String line) {
            return null;
        }

        @Override
        public void applyResultError() {

        }

        @Override
        public void applyResultSuccess(String balanceText) {

        }
    }
    private static class AccountBalance extends AccountBalanceHelper {
        WeakReference<HikariDenwa> w;

        AccountBalance(HikariDenwa wizard){
            w = new WeakReference<HikariDenwa>(wizard);
        }
        @Override
        public HttpRequestBase getRequest(SipProfile acc) throws IOException {
            String requestURL = "http://ntt.setup/cas_tel_conf/";
            HttpPost req = new HttpPost(requestURL);
            List<NameValuePair> post_params = new ArrayList<NameValuePair>();
            post_params.add(new BasicNameValuePair("post_1", "ユーザID"));
            post_params.add(new BasicNameValuePair("post_2", "パスワード"));
            req.setEntity(new UrlEncodedFormEntity(post_params, "UTF-8"));

            return req;
        }

        @Override
        public String parseResponseLine(String line) {
            return null;
        }

        @Override
        public void applyResultError() {

        }

        @Override
        public void applyResultSuccess(String balanceText) {

        }
    }
}
