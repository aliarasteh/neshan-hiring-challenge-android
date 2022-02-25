package org.neshan.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.neshan.common.model.LatLng;
import org.neshan.data.model.response.AddressDetailResponse;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class MainActivityViewModel extends AndroidViewModel {

    private final MainActivityModel mModel;

    private final CompositeDisposable mCompositeDisposable;

    private final MutableLiveData<AddressDetailResponse> mLocationAddressDetail;

    @Inject
    public MainActivityViewModel(@NonNull Application application, MainActivityModel model) {
        super(application);

        mModel = model;
        mCompositeDisposable = new CompositeDisposable();
        mLocationAddressDetail = new MutableLiveData<>();

    }

    public LiveData<AddressDetailResponse> getLocationAddressDetailLiveData() {
        return mLocationAddressDetail;
    }

    // try to load address detail from server
    public void loadAddressForLocation(LatLng latLng) {
        mModel.getAddress(latLng.getLatitude(), latLng.getLongitude())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<>() {
                    @Override
                    public void onSubscribe(Disposable disposable) {
                        mCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onSuccess(AddressDetailResponse response) {

                        if (response.isSuccessFull()) {
                            mLocationAddressDetail.postValue(response);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        // TODO: show error to user
                        e.printStackTrace();
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // disposes any incomplete request to avoid possible error also unnecessary network usage
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }

    }

}
