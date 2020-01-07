package com.example.rxandrxbindinginprojectactualyll1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Email
        RxTextView.afterTextChangeEvents(edtEmail)
            //Skip enterEmail’s initial, empty state
            .skipInitialValue().map {
                emailError.error = null
                it.view().text.toString()
            }
            .debounce(400, TimeUnit.MILLISECONDS)
            //Make sure we’re in Android’s main UI thread
            .observeOn(AndroidSchedulers.mainThread())
            //Apply the validateEmailAddress transformation function
            .compose(validateEmailAddress)
            .compose(retryWhenError { emailError.error = it.message })
            .subscribe()

        //Password
        RxTextView.afterTextChangeEvents(edtPassword)
            //Skip enterEmail’s initial, empty state
            .skipInitialValue().map {
                passwordError.error = null
                it.view().text.toString()
            }
            .debounce(400, TimeUnit.MILLISECONDS)
            //Make sure we’re in Android’s main UI thread
            .observeOn(AndroidSchedulers.mainThread())
            //Apply the validatePassword transformation function
            .compose(validatePassword)
            .compose(retryWhenError { passwordError.error = it.message })
            .subscribe()
    }

    //try again
    private inline fun retryWhenError(crossinline onError: (ex: Throwable) ->
    Unit): ObservableTransformer<String, String> = ObservableTransformer { observable ->
        observable.retryWhen { errors ->
            errors.flatMap {
                onError(it)
                Observable.just("")
            }
        }
    }

    //validate email
    private val validateEmailAddress = ObservableTransformer<String, String> { observable ->
        observable.flatMap { it ->
            Observable.just(it).map { it.trim() }
                .filter {
                    Patterns.EMAIL_ADDRESS.matcher(it).matches()
                }
            //If the user’s input doesn’t match the email pattern, then throw an error
                .singleOrError()
                .onErrorResumeNext {
                    if (it is NoSuchElementException){
                        Single.error(Exception("Please enter a valid email address"))
                    }else{
                        Single.error(it)
                    }
                }
                .toObservable()
        }
    }

    //validate password
    private val validatePassword = ObservableTransformer<String, String> { observable ->
        observable.flatMap {it ->
            Observable.just(it).map { it.trim() }
                .filter { it.length > 7 }
                .singleOrError()
                .onErrorResumeNext {
                    if (it is NoSuchElementException){
                        Single.error(Exception("Your password must be 7 characters or more"))
                    }else{
                        Single.error(it)
                    }
                }
                .toObservable()
        }
    }

}
