package com.commit451.fourohone.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    TextView textLoggedIn;
    Button buttonLogIn;
    Button buttonFetchResource;
    Button buttonAlwaysFail;

    Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonLogIn = (Button) findViewById(R.id.button_login);
        buttonFetchResource = (Button) findViewById(R.id.button_fetch_resource);
        buttonAlwaysFail = (Button) findViewById(R.id.button_always_fail);
        textLoggedIn = (TextView) findViewById(R.id.text_logged_in);

        client = new Client(getApplicationContext());

        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.login()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<String>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                            }

                            @Override
                            public void onSuccess(@NonNull String s) {
                                Toast.makeText(MainActivity.this, "Login successful! Server assigned token of " + s, Toast.LENGTH_SHORT)
                                        .show();
                                setLoggedIn();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                onHandleFailure(e);
                            }
                        });
            }
        });

        buttonFetchResource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //make the client forget the token to make it fail
                client.setToken(null);
                client.getResource()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<String>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                            }

                            @Override
                            public void onSuccess(@NonNull String s) {
                                Toast.makeText(MainActivity.this, "Got authenticated resource" + s, Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                onHandleFailure(e);
                            }
                        });
            }
        });

        buttonAlwaysFail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                client.getResourceAndFail()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<String>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                            }

                            @Override
                            public void onSuccess(@NonNull String s) {
                                Toast.makeText(MainActivity.this, "Got authenticated resource" + s, Toast.LENGTH_SHORT)
                                        .show();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                onHandleFailure(e);
                            }
                        });
            }
        });

        setLoggedOut();
    }

    private void setLoggedIn() {
        buttonLogIn.setVisibility(View.GONE);
        textLoggedIn.setVisibility(View.VISIBLE);
        buttonFetchResource.setVisibility(View.VISIBLE);
        buttonAlwaysFail.setVisibility(View.VISIBLE);
    }

    private void setLoggedOut() {
        buttonLogIn.setVisibility(View.VISIBLE);
        textLoggedIn.setVisibility(View.GONE);
        buttonFetchResource.setVisibility(View.GONE);
        buttonAlwaysFail.setVisibility(View.GONE);
    }

    private void onHandleFailure(Throwable t) {
        Log.e("Error", "Error", t);
        Toast.makeText(this, "Error: " + t.getMessage(), Toast.LENGTH_LONG)
                .show();
    }
}
