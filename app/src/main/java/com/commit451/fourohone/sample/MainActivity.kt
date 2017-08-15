package com.commit451.fourohone.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

    lateinit var textLoggedIn: TextView
    lateinit var buttonLogIn: Button
    lateinit var buttonFetchResource: Button
    lateinit var buttonAlwaysFail: Button

    lateinit var client: Client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonLogIn = findViewById<View>(R.id.button_login) as Button
        buttonFetchResource = findViewById<View>(R.id.button_fetch_resource) as Button
        buttonAlwaysFail = findViewById<View>(R.id.button_always_fail) as Button
        textLoggedIn = findViewById<View>(R.id.text_logged_in) as TextView

        client = Client(applicationContext)

        buttonLogIn.setOnClickListener {
            client.login()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<String> {
                        override fun onSubscribe(@NonNull d: Disposable) {}

                        override fun onSuccess(@NonNull s: String) {
                            Toast.makeText(this@MainActivity, "Login successful! Server assigned token of " + s, Toast.LENGTH_SHORT)
                                    .show()
                            setLoggedIn()
                        }

                        override fun onError(@NonNull e: Throwable) {
                            onHandleFailure(e)
                        }
                    })
        }

        buttonFetchResource.setOnClickListener {
            //make the client forget the token to make it fail
            client.setToken(null)
            client.getResource()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<String> {
                        override fun onSubscribe(@NonNull d: Disposable) {}

                        override fun onSuccess(@NonNull s: String) {
                            Toast.makeText(this@MainActivity, "Got authenticated resource" + s, Toast.LENGTH_SHORT)
                                    .show()
                        }

                        override fun onError(@NonNull e: Throwable) {
                            onHandleFailure(e)
                        }
                    })
        }

        buttonAlwaysFail.setOnClickListener {
            client.getResourceAndFail()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<String> {
                        override fun onSubscribe(@NonNull d: Disposable) {}

                        override fun onSuccess(@NonNull s: String) {
                            Toast.makeText(this@MainActivity, "Got authenticated resource" + s, Toast.LENGTH_SHORT)
                                    .show()
                        }

                        override fun onError(@NonNull e: Throwable) {
                            onHandleFailure(e)
                        }
                    })
        }

        setLoggedOut()
    }

    private fun setLoggedIn() {
        buttonLogIn.visibility = View.GONE
        textLoggedIn.visibility = View.VISIBLE
        buttonFetchResource.visibility = View.VISIBLE
        buttonAlwaysFail.visibility = View.VISIBLE
    }

    private fun setLoggedOut() {
        buttonLogIn.visibility = View.VISIBLE
        textLoggedIn.visibility = View.GONE
        buttonFetchResource.visibility = View.GONE
        buttonAlwaysFail.visibility = View.GONE
    }

    private fun onHandleFailure(t: Throwable) {
        Log.e("Error", "Error", t)
        Toast.makeText(this, "Error: " + t.message, Toast.LENGTH_LONG)
                .show()
    }
}
